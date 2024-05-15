package com.github.nebelnidas.kfdl.core;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;

public class Downloader {
	private final List<Runnable> onFinishListeners = Collections.synchronizedList(new ArrayList<>());
	private final Queue<MergedEpisodeData> pendingDownloads = new ConcurrentLinkedQueue<>();
	private final List<MergedEpisodeData> activeDownloads = Collections.synchronizedList(new ArrayList<>());
	private final List<MergedEpisodeData> successfulDownloads = Collections.synchronizedList(new ArrayList<>());
	private final List<MergedEpisodeData> failedDownloads = Collections.synchronizedList(new ArrayList<>());
	private final Path workingDir;
	private final int maxThreads;
	private final ExecutorService threadPool;
	private final SaveFileHandler saveFileHandler;
	private volatile boolean finished = false;

	public Downloader(Path workingDir, int maxThreads, SaveFileHandler saveFileHandler) {
		this.workingDir = workingDir;
		this.maxThreads = maxThreads;
		this.threadPool = Executors.newFixedThreadPool(maxThreads);
		this.saveFileHandler = saveFileHandler;
	}

	public void queue(MergedEpisodeData data) {
		pendingDownloads.add(data);
		saveFileHandler.addOrUpdate(data, DownloadState.QUEUED);

		if (activeDownloads.size() < maxThreads) {
			synchronized (activeDownloads) {
				if (activeDownloads.size() < maxThreads) {
					startNextDownload();
				}
			}
		}
	}

	private void startNextDownload() {
		MergedEpisodeData item = pendingDownloads.poll();

		if (item == null) {
			finished = true;
			return;
		}

		threadPool.submit(() -> {
			try {
				Kfdl.LOGGER.info("Starting download of episode '{}'", item.title());
				activeDownloads.add(item);
				saveFileHandler.addOrUpdate(item, DownloadState.DOWNLOADING);
				download(item);
				successfulDownloads.add(item);
				tag(item, workingDir.resolve(item.date().toString() + ".mp3"));
				saveFileHandler.addOrUpdate(item, DownloadState.SUCCESSFUL);
				Kfdl.LOGGER.info("Download of episode '{}' finished", item.title());
			} catch (Exception e) {
				failedDownloads.add(item);
				saveFileHandler.addOrUpdate(item, DownloadState.FAILED);
				Kfdl.LOGGER.error("Download of episode '{}' failed", item.title(), e);
			}

			activeDownloads.remove(item);
			saveFileHandler.suggestSave();
			startNextDownload();
		});
	}

	private void download(MergedEpisodeData episodeData) throws MalformedURLException, IOException {
		ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(episodeData.defaultDownload()).openStream());
		Path path = workingDir.resolve(episodeData.date().toString() + ".mp3");

		if (!path.toFile().exists()) {
			try (FileOutputStream fileOutputStream = new FileOutputStream(path.toString())) {
				FileChannel fileChannel = fileOutputStream.getChannel();
				fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
		}
	}

	private void tag(MergedEpisodeData episodeData, Path path) {
		try {
			MP3File mp3File = (MP3File) AudioFileIO.read(path.toFile());
			ID3v24Tag tagContainer = new ID3v24Tag(); // Jellyfin doesn't read full dates from <2.4
			tagContainer.setField(FieldKey.ALBUM, "Kontrafunk aktuell");
			tagContainer.setField(FieldKey.TITLE, episodeData.title());
			tagContainer.setField(FieldKey.LANGUAGE, "deu");
			tagContainer.setField(FieldKey.YEAR, episodeData.date().toString());
			tagContainer.setField(FieldKey.ORIGINALRELEASEDATE, episodeData.date().toString());

			if (episodeData.description() != null) {
				tagContainer.setField(FieldKey.COMMENT, episodeData.description());
			}

			tagContainer.setField(FieldKey.ALBUM_ARTIST, "Kontrafunk");
			tagContainer.setField(FieldKey.ARTIST, Stream.of(Collections.singletonList(episodeData.host()), episodeData.guests(), Collections.singletonList(episodeData.commentAuthor()))
					.flatMap(List::stream)
					.filter(person -> person != null)
					.map(person -> person.getName())
					.collect(Collectors.joining("; ")));
			tagContainer.setField(FieldKey.GENRE, episodeData.tags().stream().map(tag -> tag.getName()).collect(Collectors.joining("; ")));
			mp3File.setTag(tagContainer);
			mp3File.commit();
		} catch (Exception e) {
			Kfdl.LOGGER.error("Failed to write ID3 tags to file", e);
		}
	}

	public void addOnFinish(Runnable onFinish) {
		if (finished) {
			onFinish.run();
		} else {
			onFinishListeners.add(onFinish);
		}
	}
}

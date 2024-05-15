package com.github.nebelnidas.kfdl.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;

/**
 * The format of the file is as follows:
 * <pre>{@code
 * file    = <header> <episode>*
 * header  = 'kfdl-save' <tab> <major-version> <tab> <minor-version> <newline>
 * episode = <episode-date> <tab> <download-link> <tab> <download-state> <newline>
 * }</pre>.
 */
public class SaveFileHandler {
	private static final long SAVE_INTERVAL = 1000;
	private static final String V1_HEADER_PREFIX = "kfdl-save\t1\t";
	private static final String V1_0_HEADER = V1_HEADER_PREFIX + "0\n";
	private static final Map<Path, Lock> locks = new HashMap<>();
	private final Path saveFilePath;
	private final Lock lock;
	private volatile TreeSet<SaveFileEntry> episodes;
	private volatile boolean dirty;
	private volatile long lastSaveTime = 0;

	public SaveFileHandler(Path saveFile) {
		this.saveFilePath = saveFile;
		this.lock = locks.computeIfAbsent(saveFile, path -> new ReentrantLock(true));
	}

	public Set<SaveFileEntry> getEpisodes() {
		if (episodes != null) {
			return episodes;
		}

		lock.lock();

		try {
			if (episodes == null) {
				readEpisodes();
			}
		} finally {
			lock.unlock();
		}

		return Collections.unmodifiableSet(episodes);
	}

	private void readEpisodes() {
		episodes = new TreeSet<>((e1, e2) -> e2.episodeDate().compareTo(e1.episodeDate()));

		if (!Files.exists(saveFilePath)) {
			return;
		}

		try {
			String saveFileContent = FileUtils.readFileToString(saveFilePath.toFile(), "UTF-8");

			if (!saveFileContent.startsWith(V1_HEADER_PREFIX)) {
				Kfdl.LOGGER.warn("Save file header is invalid, ignoring save file");
				return;
			}

			String[] lines = saveFileContent
					.substring(V1_0_HEADER.length())
					.split("\n");

			for (String line : lines) {
				String[] parts = line.split("\t");

				if (parts.length != 3) {
					Kfdl.LOGGER.warn("Save file line has invalid number of parts, ignoring line: {}", line);
					continue;
				}

				SaveFileEntry episode = new SaveFileEntry(LocalDate.parse(parts[0]), parts[1], DownloadState.parse(parts[2]));
				episodes.add(episode);
			}
		} catch (IOException e) {
			Kfdl.LOGGER.error("Failed to read save file", e);
		}
	}

	public void addOrUpdate(MergedEpisodeData episode, DownloadState state) {
		add(new SaveFileEntry(episode, state));
	}

	public void add(SaveFileEntry entry) {
		lock.lock();

		try {
			for (SaveFileEntry existingEntry : episodes) {
				if (existingEntry.episodeDate().equals(entry.episodeDate())) {
					if (existingEntry.episodeDate().equals(entry.episodeDate())) {
						if (existingEntry.equals(entry)) {
							return; // Entry with same properties is already in the set
						}

						episodes.remove(existingEntry);
						break;
					}
				}
			}

			episodes.add(entry);
			dirty = true;
		} finally {
			lock.unlock();
		}
	}

	public void suggestSave() {
		lock.lock();

		try {
			if (dirty || System.currentTimeMillis() - lastSaveTime > SAVE_INTERVAL) {
				forceSave();
			}
		} finally {
			lock.unlock();
		}
	}

	public void forceSave() {
		lock.lock();

		try {
			if (!dirty) {
				return;
			}

			writeFile();
			lastSaveTime = System.currentTimeMillis();
		} finally {
			lock.unlock();
		}
	}

	private void writeFile() {
		StringBuilder saveFileContent = new StringBuilder(V1_0_HEADER);

		for (SaveFileEntry episode : episodes) {
			saveFileContent
					.append(episode.episodeDate())
					.append('\t')
					.append(episode.downloadLink())
					.append('\t')
					.append(episode.downloadState().name())
					.append('\n');
		}

		try {
			Files.writeString(saveFilePath, saveFileContent.toString());
		} catch (IOException e) {
			Kfdl.LOGGER.error("Failed to write save file", e);
		}
	}
}

package com.github.nebelnidas.kfdl.core;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.WebsiteEpisodeData;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerEpisodeData;

public class Kfdl {
	public static final String SPREAKER_FEED_URL = "https://www.spreaker.com/show/5602119/episodes/feed";
	public static final Logger LOGGER = LoggerFactory.getLogger("Kontrafunk Downloader");
	private static final String kfAktuellUrlPrefix = "https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/";
	private static final String kfAktuellDownloadPrefix = "https://kontrafunk.radio/images/audio/sendungen/";
	private static final DateTimeFormatter kfAktuellUrlDateFormatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.GERMAN);

	public void run() {
		try {
			run0();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void run0() throws MalformedURLException, XMLStreamException, IOException {
		SpreakerEpisodeExtractor spreaker = new SpreakerEpisodeExtractor();

		List<SpreakerEpisodeData> entries = spreaker.getItems(new BufferedInputStream(new URL(SPREAKER_FEED_URL).openStream()));

		for (SpreakerEpisodeData entry : entries) {
			LOGGER.info("Found episode: {} - {}", entry.publicationDate(), entry.title());
		}

		for (SpreakerEpisodeData spreakerEntry : entries) {
			if (spreakerEntry.episodeType() == EpisodeType.WOCHENRÜCKBLICK) {
				continue;
			}

			WebsiteEpisodeData scrapedData = scrapeEpisodeData(spreakerEntry);
			MergedEpisodeData mergedData = new MergedEpisodeData(spreakerEntry, scrapedData);
			downloadEpisode(mergedData);
		}
	}

	private WebsiteEpisodeData scrapeEpisodeData(SpreakerEpisodeData spreakerData) throws IOException {
		String url = spreakerData.title().toLowerCase(Locale.GERMAN);

		url = url.substring(0, url.indexOf("vom") + 4)
				+ spreakerData.targetDate().format(kfAktuellUrlDateFormatter).toLowerCase(Locale.GERMAN);
		url = kfAktuellUrlPrefix + url
				.replace(" ", "-")
				.replace("ä", "ae")
				.replace("ö", "oe")
				.replace("ü", "ue")
				.replace(".", "");

		LOGGER.info("Scraping info for {}", spreakerData.title());

		try {
			return KontrafunkScraper.getEpisodeInfo(url, spreakerData);
		} catch (Exception e) {
			LOGGER.error("Failed to scrape info for {}: {}", spreakerData.title(), e.getMessage());
			return null;
		}
	}

	private void downloadEpisode(MergedEpisodeData episodeData) {
		LOGGER.info("Downloading...");

		try {
			downloadEpisode0(episodeData);
			LOGGER.info("Done");
		} catch (Exception e) {
			LOGGER.error("Failed to download episode: {}", e.getMessage());
		}
	}

	private void downloadEpisode0(MergedEpisodeData episodeData) throws IOException {
		String urlPrefix = kfAktuellDownloadPrefix
				+ episodeData.date().getYear()
				+ "/"
				+ episodeData.date().format(DateTimeFormatter.ofPattern("MM"))
				+ "/"
				+ episodeData.date().getDayOfMonth()
				+ "/"
				+ episodeData.date().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		int attempt = 0;

		while (attempt < 2) {
			URL url = new URL(urlPrefix + "_" + switch (attempt) {
				case 0 -> "Kontrafunk_aktuell";
				case 1 -> "Kontrafunk_aktuell_";
				default -> throw new IllegalStateException("Unexpected value: " + attempt);
			} + ".mp3");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				attempt++;
				continue;
			}

			downloadEpisode1(url, episodeData);
			return;
		}

		LOGGER.error("Failed to download episode: No download link found");
	}

	private void downloadEpisode1(URL url, MergedEpisodeData episodeData) throws IOException {
		ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());

		try (FileOutputStream fileOutputStream = new FileOutputStream(episodeData.title() + ".mp3")) {
			FileChannel fileChannel = fileOutputStream.getChannel();
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		}
	}
}

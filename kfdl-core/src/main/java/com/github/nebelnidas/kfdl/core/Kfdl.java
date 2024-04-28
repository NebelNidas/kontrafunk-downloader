package com.github.nebelnidas.kfdl.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.EpisodeInfo;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerItem;

public class Kfdl {
	public static final String SPREAKER_FEED_URL = "https://www.spreaker.com/show/5602119/episodes/feed";
	public static final Logger LOGGER = LoggerFactory.getLogger("Kontrafunk Downloader");
	private static final String kfAktuellUrlPrefix = "https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/";
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

		List<SpreakerItem> entries = spreaker.getItems(new BufferedInputStream(new URL(SPREAKER_FEED_URL).openStream()));

		for (SpreakerItem entry : entries) {
			LOGGER.info("Found episode: {} - {}", entry.publicationDate(), entry.title());
		}

		for (SpreakerItem entry : entries) {
			if (entry.episodeType() == EpisodeType.WOCHENRÜCKBLICK) {
				continue;
			}

			EpisodeInfo episodeInfo = scrapeEpisodeInfo(entry);
			int i = 0;
		}
	}

	private EpisodeInfo scrapeEpisodeInfo(SpreakerItem spreakerItem) throws IOException {
		String url = spreakerItem.title().toLowerCase(Locale.GERMAN);

		url = url.substring(0, url.indexOf("vom") + 4)
				+ spreakerItem.targetDate().format(kfAktuellUrlDateFormatter).toLowerCase(Locale.GERMAN);
		url = kfAktuellUrlPrefix + url
				.replace(" ", "-")
				.replace("ä", "ae")
				.replace("ö", "oe")
				.replace("ü", "ue")
				.replace(".", "");

		LOGGER.info("Scraping info for {}", spreakerItem.title());

		try {
			return KontrafunkScraper.getEpisodeInfo(url, spreakerItem);
		} catch (Exception e) {
			LOGGER.warn("Failed to scrape info for {}: {}", spreakerItem.title(), e.getMessage());
			return null;
		}
	}
}

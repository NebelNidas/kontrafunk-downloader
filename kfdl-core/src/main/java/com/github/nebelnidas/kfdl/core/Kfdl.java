package com.github.nebelnidas.kfdl.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.htmlunit.FailingHttpStatusCodeException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.WebsiteEpisodeData;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerEpisodeData;

public class Kfdl {
	public static final Logger LOGGER = LoggerFactory.getLogger("Kontrafunk Downloader");
	private static final String spreakerFeedUrl = "https://www.spreaker.com/show/5602119/episodes/feed";
	private static final String kfAktuellUrlPrefix = "https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/";
	private static final String kfAktuellDownloadPrefix = "https://kontrafunk.radio/images/audio/sendungen/";
	private static final DateTimeFormatter kfAktuellUrlDateFormatter = DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.GERMAN);
	private static final DateTimeFormatter kfAktuellUrlDateFormatter2 = DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.GERMAN);
	private static final DateTimeFormatter kfAktuellUrlDateFormatter3 = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.GERMAN);
	private static final DateTimeFormatter kfAktuellUrlDateFormatter4 = DateTimeFormatter.ofPattern("d-M-yyyy", Locale.GERMAN);
	private static final LocalDate lastDasMorgenmagazinDate = LocalDate.of(2022, 8, 26);
	private static final LocalDate lastMorgenmagazinDate = LocalDate.of(2022, 7, 15);
	private static final LocalDate firstDateWithOfficialDownload = LocalDate.of(2023, 7, 20);
	private SaveFileHandler saveFileHandler;
	private Downloader downloader;

	public Kfdl(Path workingDir, Path saveFilePath, int maxParallelDownloads) {
		this.saveFileHandler = new SaveFileHandler(saveFilePath);
		this.downloader = new Downloader(workingDir, maxParallelDownloads, saveFileHandler);
	}

	public void run() {
		try {
			run0();
		} catch (Exception e) {
			LOGGER.error("Encountered unhandled exception", e);
		}
	}

	private void run0() throws MalformedURLException, XMLStreamException, IOException {
		Set<LocalDate> alreadyIndexed = saveFileHandler.getEpisodes().stream()
				.map(SaveFileEntry::episodeDate)
				.collect(Collectors.toSet());

		Set<LocalDate> alreadyDownloaded = saveFileHandler.getEpisodes().stream()
				.filter(entry -> entry.downloadState() == DownloadState.SUCCESSFUL)
				.map(SaveFileEntry::episodeDate)
				.collect(Collectors.toSet());

		List<SpreakerEpisodeData> entriesToDownload = getSpreakerData().stream()
				.filter(entry -> entry.episodeType() != EpisodeType.WOCHENRÜCKBLICK)
				.filter(entry -> !alreadyDownloaded.contains(entry.date()))
				.collect(Collectors.toList());

		for (SpreakerEpisodeData spreakerEntry : entriesToDownload) {
			if (!alreadyIndexed.contains(spreakerEntry.date())) {
				LOGGER.info("Found new episode: {}", spreakerEntry.title());
			}
		}

		for (SpreakerEpisodeData spreakerEntry : entriesToDownload) {
			WebsiteEpisodeData scrapedData = scrapeEpisodeData(spreakerEntry);

			if (scrapedData == null) {
				continue;
			}

			MergedEpisodeData mergedData = new MergedEpisodeData(spreakerEntry, scrapedData);

			if (mergedData.defaultDownload().startsWith("/images/audio/sendungen/")) {
				mergedData = mergedData.withDefaultDownload(kfAktuellDownloadPrefix + scrapedData.downloadLink().substring(24));
			}

			scheduleDownload(mergedData);
		}

		LOGGER.debug("Tags:\n" + Tag.tagsById.toString());
		System.in.read();
		LOGGER.debug("People:\n" + Person.personsByName.toString());

		downloader.addOnFinish(() -> saveFileHandler.forceSave());
	}

	private List<SpreakerEpisodeData> getSpreakerData() throws UnsupportedEncodingException, MalformedURLException, XMLStreamException, IOException {
		InputStream inputStream = new URL(spreakerFeedUrl).openStream();
		Iterator<SpreakerEpisodeData> iterator = SpreakerEpisodeExtractor.iterateItems(inputStream);
		List<SpreakerEpisodeData> episodes = new ArrayList<>();

		while (iterator.hasNext()) {
			SpreakerEpisodeData episode = iterator.next();
			episodes.add(episode);
		}

		return episodes;
	}

	@Nullable
	private WebsiteEpisodeData scrapeEpisodeData(SpreakerEpisodeData spreakerData) throws IOException {
		LOGGER.debug("Scraping info for {}", spreakerData.title());
		int maxAttempts = 4;

		switch (spreakerData.date().toString()) {
			case "2022-08-12":
				return WebsiteEpisodeData.builder()
						.date(spreakerData.date())
						.host(Person.BENJAMIN_GOLLME)
						.guests(List.of(Person.WERNER_KIRSTEIN, Person.HANS_PETER_DIETZ, Person.OLIVER_HOLZER))
						.tags(Collections.emptyList())
						.build();
		}

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			String urlSuffix = switch (spreakerData.date().toString()) {
				case "2023-03-30" -> "kontrafunk-aktuell-vom30-maerz-2023";
				case "2022-11-14" -> "kontrafunk-aktuell-vom-14-november-2022-2";
				case "2022-10-06" -> "kontrafunk-aktuell-vom-6-oktober-2022-2";
				case "2022-09-20" -> "kontrafunk-aktuell-vom20-september-2022";
				case "2022-09-08" -> "kontrafunk-aktuell-vom-8-september-2022-2";
				case "2022-09-09" -> "kontrafunk-aktuell-vom-8-september-2022";
				case "2022-08-25" -> "das-morgenmagazin-vom-25-august-20222";
				case "2022-08-23" -> "das-morgenmagazin-vom-23-august-2022-2";
				default -> null;
			};

			if (urlSuffix != null) {
				attempt = maxAttempts;
			} else {
				DateTimeFormatter formatter = switch (attempt) {
					case 1 -> kfAktuellUrlDateFormatter;
					case 2 -> kfAktuellUrlDateFormatter2;
					case 3 -> kfAktuellUrlDateFormatter3;
					case 4 -> kfAktuellUrlDateFormatter4;
					default -> throw new IllegalStateException("Unexpected value: " + attempt);
				};

				String slugPrefix = spreakerData.date().isAfter(lastDasMorgenmagazinDate)
						|| spreakerData.date().toString().equals("2022-07-07")
						|| spreakerData.date().toString().equals("2022-07-06")
								? "kontrafunk-aktuell-vom-"
								: spreakerData.date().isAfter(lastMorgenmagazinDate)
										? "das-morgenmagazin-vom-"
										: "morgenmagazin-vom-";
				urlSuffix = slugPrefix + spreakerData.date()
						.format(formatter)
						.toLowerCase(Locale.GERMAN)
						.replace("ä", "ae");
			}

			String url = kfAktuellUrlPrefix + urlSuffix;
			WebsiteEpisodeData scrapedData;

			try {
				scrapedData = KontrafunkScraper.getEpisodeInfo(url, spreakerData);
				return scrapedData;
			} catch (Exception e) {
				if (attempt < maxAttempts && e instanceof FailingHttpStatusCodeException) {
					continue;
				}

				LOGGER.error("Failed to scrape info for {}", spreakerData.title(), e);
			}
		}

		return null;
	}

	private void scheduleDownload(MergedEpisodeData episodeData) {
		try {
			String url = getDownloadLink(episodeData);

			if (url == null) {
				LOGGER.error("Failed to download episode: No download link found");
				return;
			}

			episodeData = episodeData.withDefaultDownload(url);

			downloader.queue(episodeData);
		} catch (Exception e) {
			LOGGER.error("Failed to download episode", e);
		}
	}

	@Nullable
	private String getDownloadLink(MergedEpisodeData episodeData) throws IOException {
		String urlPrefix = kfAktuellDownloadPrefix
				+ episodeData.date().getYear()
				+ "/"
				+ episodeData.date().format(DateTimeFormatter.ofPattern("MM"))
				+ "/"
				+ episodeData.date().format(DateTimeFormatter.ofPattern("dd"))
				+ "/";
		String concatDate = episodeData.date().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		int maxAttempts = 7;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			String url = switch (episodeData.date().toString()) {
				case "2024-03-19" -> urlPrefix + "20230319_Kontrafunkt_aktuell_.mp3";
				case "2024-02-29" -> urlPrefix + "20240228_Kontrafunk_aktuell.mp3";
				case "2024-01-31" -> urlPrefix + "Kontrafunk_aktuell_20240131 final CUT.mp3";
				case "2023-11-23" -> urlPrefix + "Kontrafunk Aktuell 202331123.mp3";
				default -> {
					if (episodeData.date().isBefore(firstDateWithOfficialDownload)) {
						attempt = maxAttempts;
					}

					yield null;
				}
			};

			if (url != null) {
				attempt = maxAttempts;
			} else if (attempt < maxAttempts) {
				url = urlPrefix + switch (attempt) {
					case 1 -> concatDate + "_Kontrafunk_aktuell";
					case 2 -> concatDate + "_Kontrafunk_Aktuell";
					case 3 -> "Kontrafunk_aktuell_" + concatDate;
					case 4 -> "Kontrafunk_Aktuell_" + concatDate;
					case 5 -> "Kontrafunk Aktuell " + concatDate;
					case 6 -> "Kontrafunk _Aktuell_" + concatDate;
					case 7 -> "kontrafunk_aktuell_" + concatDate;
					default -> throw new IllegalStateException("Unexpected value: " + attempt);
				} + ".mp3";
			} else if (!episodeData.defaultDownload().contains("spreaker")) {
				url = episodeData.defaultDownload();
			} else {
				LOGGER.info("No official download link found for {}. Falling back to Spreaker.", episodeData.title());
				url = episodeData.spreakerDownload();
			}

			url = url.replace(" ", "%20");
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("HEAD");

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				continue;
			}

			return url;
		}

		return null;
	}
}

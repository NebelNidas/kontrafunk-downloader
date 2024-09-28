package com.github.nebelnidas.kfdl.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import lombok.Builder;
import lombok.NonNull;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomText;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.WebsiteEpisodeData.WebsiteEpisodeDataBuilder;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerEpisodeData;

public class KontrafunkScraper {
	// Freitag, 26. April 2024, 5:05 Uhr
	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, H:mm 'Uhr'", Locale.GERMAN);
	private static final LocalDate firstDateWithDownloadButton = LocalDate.of(2023, 7, 20);
	private static final LocalDate firstDateWithDescription = LocalDate.of(2022, 8, 17);
	private static final LocalDate firstDateWithTags = firstDateWithDescription;

	public static WebsiteEpisodeData getEpisodeInfo(String episodeUrl, SpreakerEpisodeData spreakerData) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		try (WebClient webClient = new WebClient()) {
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);

			WebsiteEpisodeDataBuilder builder = WebsiteEpisodeData.builder();
			HtmlPage page = webClient.getPage(episodeUrl);

			builder.url(episodeUrl);
			hydrateDate(builder, episodeUrl, page);
			hydratePeople(builder, episodeUrl, page, spreakerData);
			hydrateDescription(builder, episodeUrl, page);
			hydrateDownloadLink(builder, episodeUrl, page);
			hydrateTags(builder, episodeUrl, page);

			return builder.build();
		}
	}

	private static void hydrateDate(WebsiteEpisodeDataBuilder builder, String url, HtmlPage page) {
		HtmlElement date = page.getFirstByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[1]");

		if (date == null) {
			throw new IllegalStateException("No date found on " + url);
		}

		builder.date(LocalDate.parse(date.asNormalizedText(), dateFormatter));
	}

	private static void hydratePeople(WebsiteEpisodeDataBuilder builder, String url, HtmlPage page, SpreakerEpisodeData spreakerData) {
		List<HtmlElement> elements = page.getByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[last()]/div/span");

		if (elements.size() < 2 || elements.size() > 4) {
			switch (builder.date.toString()) {
				case "2022-11-01":
					builder.host(Person.MARCEL_JOPPA);
					builder.guests(List.of(Person.BENNY_PEISER, Person.CHRISTIAN_FIALA, Person.UTE_BERGNER));
					return;
				case "2022-08-16":
					builder.host(Person.MARCEL_JOPPA);
					builder.guests(List.of(Person.SUSANNE_DAGEN, Person.GUNTER_FRANK, Person.CORA_STEPHAN));
					return;
				default:
					throw new IllegalStateException("Unexpected number of potential participant spans (" + elements.size() + ") on " + url);
			}
		}

		DomNode current = elements.remove(0);
		String imGesprächMit = "im Gespräch mit";

		if (current.getNextSibling().asNormalizedText().equals(imGesprächMit)) {
			builder.host(Person.getOrCreate(current.asNormalizedText()));
			current = elements.remove(0);
		} else {
			builder.host(switch (spreakerData.publicationDate().toString()) {
				case "2024-04-16" -> Person.MARCEL_JOPPA;
				case "2023-12-27" -> Person.TIM_KRAUSE;
				case "2023-06-09" -> Person.JASMIN_KOSUBEK;
				case "2023-06-01" -> Person.JASMIN_KOSUBEK;
				case "2023-05-26" -> Person.JASMIN_KOSUBEK;
				case "2022-07-09" -> Person.MICHAEL_GÖRMANN;
				case "2022-07-07" -> Person.BENJAMIN_GOLLME;
				case "2022-07-03" -> Person.MICHAEL_GÖRMANN;
				case "2022-07-02" -> Person.MICHAEL_GÖRMANN;
				default -> throw new IllegalStateException("Host not found on " + url);
			});
		}

		List<DomNode> guestNodes = current.getChildNodes();
		List<Person> guests = new ArrayList<>();

		for (DomNode element : guestNodes) {
			if (element instanceof HtmlAnchor a) {
				guests.add(Person.getOrCreate(a.asNormalizedText()));
			} else if (element instanceof DomText text) {
				String textContent = text.asNormalizedText();
				assert textContent.isEmpty()
						|| textContent.equals(",")
						|| textContent.equals("und");
			} else {
				throw new IllegalStateException("Unexpected DOM node type " + element.getClass().getSimpleName() + " on " + url);
			}
		}

		builder.guests(guests);

		if (elements.isEmpty()) {
			return;
		}

		current = elements.remove(0);
		DomNode prev = current.getPreviousSibling();

		if (prev.asNormalizedText().equals("– mit einem Beitrag von")) {
			builder.beitragAuthor(Person.getOrCreate(current.asNormalizedText()));

			if (elements.isEmpty()) {
				return;
			}

			current = elements.remove(0);
			prev = current.getPreviousSibling();
		}

		if (prev.asNormalizedText().equals("– Kontrafunk-Kommentar:")) {
			builder.commentAuthor(Person.getOrCreate(current.asNormalizedText()));
		}

		assert elements.isEmpty();

		// if (elements.size() != 3) {
		// 	if (elements.size() == 2) {
		// 		if (page.getByXPath("//*[@id=\"id-article\"]/div[2]/div/div/div/ul/li/div/a/img").isEmpty()) {
		// 			// Host info is missing
		// 			elements.add(0, null);

		// 			host = switch (spreakerData.publicationDate().toString()) {
		// 				case "2024-04-16" -> Person.MARCEL_JOPPA;
		// 				case "2023-12-27" -> Person.TIM_KRAUSE;
		// 				case "2023-06-09" -> Person.JASMIN_KOSUBEK;
		// 				case "2023-06-01" -> Person.JASMIN_KOSUBEK;
		// 				case "2023-05-26" -> Person.JASMIN_KOSUBEK;
		// 				default -> throw new IllegalStateException("Missing host info on " + url);
		// 			};
		// 		} else {
		// 			HtmlElement element = page.getFirstByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[4]/div");
		// 			String text = element.asNormalizedText();

		// 			if (!text.contains("Kontrafunk-Kommentar")) {
		// 				// KF comment author is missing
		// 				elements.add(null);
		// 			}
		// 		}
		// 	}

		// 	if (elements.size() != 3) {
		// 		throw new IllegalStateException("Unexpected number of potential person elements (" + elements.size() + ") on " + url);
		// 	}
		// }

		// if (host == null) {
		// 	host = Person.getOrCreate(elements.get(0).getFirstChild().getNextSibling().asNormalizedText());
		// }

		// for (DomElement element : elements.get(1).getChildElements()) {
		// 	if (!(element instanceof HtmlAnchor a)) continue;

		// 	guests.add(Person.getOrCreate(a.asNormalizedText()));
		// }

		// if (elements.get(2) != null) {
		// 	commentAuthor = Person.getOrCreate(elements.get(2).getFirstChild().getNextSibling().asNormalizedText());
		// }
	}

	private static void hydrateDescription(WebsiteEpisodeDataBuilder builder, String url, HtmlPage page) {
		if (builder.date.isBefore(firstDateWithDescription)) {
			return;
		}

		int maxAttempts = 3;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			String xPath = switch (attempt) {
				case 1 -> "//*[@id=\"-interview-01\"]/div/div/div[1]/div/div[2]/p";
				case 2 -> "//*[@id=\"-interview-01\"]/div/div/div[1]/div/div[2]/div/div"; // https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/kontrafunk-aktuell-vom-19-dezember-2022
				case 3 -> "//*[@id=\"-interview-01\"]/div/div/div[1]/div/div[2]/div"; // https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/kontrafunk-aktuell-vom-21-november-2022
				default -> throw new IllegalStateException("Unexpected value: " + attempt);
			};

			List<DomElement> elements = page.getByXPath(xPath);

			if (elements.isEmpty()) {
				continue;
			}

			if (elements.size() > 3) {
				throw new IllegalStateException("Unexpected number of potential description elements (" + elements.size() + ") on " + url);
			}

			String description = elements.remove(0).asNormalizedText();

			for (DomElement element : elements) {
				description += "\n\n" + element.asNormalizedText();
			}

			builder.description(description);
			return;
		}

		throw new IllegalStateException("No description found on " + url);
	}

	private static void hydrateDownloadLink(WebsiteEpisodeDataBuilder builder, String url, HtmlPage page) {
		if (builder.date.isBefore(firstDateWithDownloadButton)) {
			return;
		}

		HtmlAnchor downloadButton = page.getFirstByXPath("//*[@id=\"-interview-01\"]/div/div/div[2]/div[2]/div/a");

		if (downloadButton == null) {
			switch (builder.date.toString()) {
				case "2024-01-22":
				case "2023-12-25":
				case "2023-12-21":
				case "2023-12-14":
				case "2023-11-03":
				case "2023-11-02":
				case "2023-11-01":
				case "2023-10-27":
				case "2023-10-26":
				case "2023-10-25":
				case "2023-10-23":
				case "2023-10-20":
				case "2023-10-19":
				case "2023-10-18":
				case "2023-10-17":
				case "2023-10-13":
				case "2023-10-12":
				case "2023-10-11":
				case "2023-10-10":
				case "2023-10-09":
				case "2023-10-05":
				case "2023-10-04":
				case "2023-10-03":
				case "2023-10-02":
				case "2023-09-29":
				case "2023-09-28":
				case "2023-09-27":
				case "2023-09-22":
				case "2023-09-21":
				case "2023-09-20":
				case "2023-09-19":
				case "2023-09-18":
				case "2023-09-15":
				case "2023-09-14":
				case "2023-09-13":
				case "2023-09-12":
				case "2023-09-11":
				case "2023-09-08":
				case "2023-09-07":
				case "2023-09-06":
				case "2023-09-05":
				case "2023-09-04":
				case "2023-09-01":
				case "2023-08-31":
				case "2023-08-30":
				case "2023-08-29":
				case "2023-08-25":
				case "2023-08-24":
				case "2023-08-23":
				case "2023-08-22":
				case "2023-08-21":
				case "2023-08-18":
				case "2023-08-17":
				case "2023-08-16":
				case "2023-08-15":
				case "2023-08-11":
				case "2023-08-09":
				case "2023-08-07":
				case "2023-08-04":
				case "2023-08-03":
				case "2023-08-02":
				case "2023-08-01":
				case "2023-07-31":
				case "2023-07-28":
				case "2023-07-26":
				case "2023-07-21":
					break; // Hosted by Spreaker
				default:
					Kfdl.LOGGER.warn("No download button found on " + url);
			}

			return;
		}

		String link = downloadButton.getHrefAttribute();

		if (!link.endsWith(".mp3")) {
			throw new IllegalStateException("Non-MP3 download link found on " + url);
		}

		builder.downloadLink(link);
	}

	private static void hydrateTags(WebsiteEpisodeDataBuilder builder, String url, HtmlPage page) {
		builder.tags(Collections.emptyList());

		if (builder.date.isBefore(firstDateWithTags)) {
			return;
		}

		switch (builder.date.toString()) {
			case "2023-06-14":
			case "2023-06-08":
			case "2023-06-05":
			case "2023-06-02":
			case "2023-06-01":
			case "2023-05-31":
			case "2022-12-30":
			case "2022-11-29":
			case "2022-11-28":
			case "2022-11-23":
			case "2022-11-21":
			case "2022-10-03":
				return;
		}

		List<HtmlAnchor> tagNodes = page.getByXPath("//*[@id=\"-interview-01\"]/div/div/div[1]/div/div[3]/span/span/span[2]/a");

		if (tagNodes.isEmpty()) {
			Kfdl.LOGGER.warn("No tags found on " + url);
			return;
		}

		List<Tag> tags = new ArrayList<>();

		for (DomNode tagNode : tagNodes) {
			tags.add(Tag.getOrCreate(tagNode.asNormalizedText()));
		}

		builder.tags(tags);
	}

	@Builder
	public record WebsiteEpisodeData(
			String url,
			@NonNull LocalDate date,
			@NonNull Person host,
			@NonNull List<Person> guests,
			Person beitragAuthor,
			Person commentAuthor,
			String description,
			String downloadLink,
			@NonNull List<Tag> tags) { }
}

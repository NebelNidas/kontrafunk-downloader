package com.github.nebelnidas.kfdl.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlPage;

import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerItem;

public class KontrafunkScraper {
	// Freitag, 26. April 2024, 5:05 Uhr
	private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, H:mm 'Uhr'", Locale.GERMAN);

	public static EpisodeInfo getEpisodeInfo(String episodeUrl, SpreakerItem spreakerInfo) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		try (WebClient webClient = new WebClient()) {
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);

			HtmlPage page = webClient.getPage(episodeUrl);
			String date = ((HtmlElement) page.getFirstByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[1]")).asNormalizedText();
			LocalDate parsedDate = LocalDate.parse(date, dateFormatter);

			List<HtmlElement> elements = page.getByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[4]/div/span");
			String host = null;
			String kfCommentAuthor = null;

			if (elements.size() != 3) {
				if (elements.size() == 2) {
					if (page.getByXPath("//*[@id=\"id-article\"]/div[2]/div/div/div/ul/li/div/a/img").isEmpty()) {
						// Host info is missing
						elements.add(0, null);

						host = switch (spreakerInfo.publicationDate().toString()) {
							case "2024-04-16" -> "Marcel Joppa";
							default -> null;
						};
					} else if (page.getByXPath("//*[@id=\"template-wI5pQLap#2\"]/div/div/div[4]/div/text()[2]").isEmpty()) {
						// KF comment author is missing
						elements.add(null);
					}
				}

				if (host == null) {
					throw new IllegalStateException("Unexpected number of elements in episode info: " + elements.size());
				}
			}

			if (host == null) {
				host = elements.get(0).getFirstChild().getNextSibling().asNormalizedText();
			}

			List<String> guests = new ArrayList<>();

			for (DomElement element : elements.get(1).getChildElements()) {
				if (!(element instanceof HtmlAnchor a)) continue;

				guests.add(a.asNormalizedText());
			}

			if (elements.size() == 3) {
				kfCommentAuthor = elements.get(2).getFirstChild().getNextSibling().asNormalizedText();
			}

			return new EpisodeInfo(parsedDate, host, guests, kfCommentAuthor);
		}
	}

	public record EpisodeInfo(
			LocalDate date,
			String host,
			List<String> guests,
			String kfCommentAuthor) { }
}

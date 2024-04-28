package com.github.nebelnidas.kfdl.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class SpreakerEpisodeExtractor {
	private static final DateTimeFormatter spreakerDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	private static final DateTimeFormatter titleDateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

	/**
	 * Parses the item tags from an XML feed.
	 *
	 * @param inputStream The input stream of the XML feed.
	 * @return A list of {@link SpreakerItem} objects.
	 * @throws UnsupportedEncodingException
	 */
	public List<SpreakerItem> getItems(InputStream inputStream) throws XMLStreamException, UnsupportedEncodingException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader reader = factory.createXMLStreamReader(new InputStreamReader(inputStream, "UTF-8"));
		List<SpreakerItem> items = new ArrayList<>();

		while (reader.hasNext()) {
			int event = reader.next();

			if (event == XMLStreamReader.START_ELEMENT && "item".equals(reader.getLocalName())) {
				String title = null;
				String description = null;
				String episodeApiLink = null;
				String pubDate = null;
				String fileUrl = null;
				String fileBytes = null;
				String fileMimeType = null;
				String durationInSeconds = null;

				while (reader.hasNext()) {
					event = reader.next();

					if (event == XMLStreamReader.START_ELEMENT) {
						String elementName = reader.getLocalName();

						if ("title".equals(elementName)) {
							title = reader.getElementText();
						} else if ("description".equals(elementName)) {
							description = reader.getElementText()
									.replace("<![CDATA[ ", "")
									.replace(" ]]>", "");
						} else if ("guid".equals(elementName)) {
							episodeApiLink = reader.getElementText();
						} else if ("pubDate".equals(elementName)) {
							pubDate = reader.getElementText();
						} else if ("enclosure".equals(elementName)) {
							fileUrl = reader.getAttributeValue(null, "url");
							fileBytes = reader.getAttributeValue(null, "length");
							fileMimeType = reader.getAttributeValue(null, "type");
						} else if ("itunes:duration".equals(elementName)) {
							durationInSeconds = reader.getElementText();
						}
					}

					if (event == XMLStreamReader.END_ELEMENT && "item".equals(reader.getLocalName())) {
						LocalDate publicationDate = LocalDate.parse(pubDate, spreakerDateFormatter);
						title = normalizeTitle(title, publicationDate);
						LocalDate targetDate = LocalDate.parse(title.substring(title.indexOf("vom") + 4), titleDateFormatter);

						items.add(new SpreakerItem(
								title,
								description,
								episodeApiLink,
								publicationDate,
								targetDate,
								fileUrl,
								fileBytes,
								fileMimeType,
								durationInSeconds,
								title.contains("Wochenrückblick") ? EpisodeType.WOCHENRÜCKBLICK : EpisodeType.AKTUELL));
					}
				}
			}
		}

		return items;
	}

	private String normalizeTitle(String text, LocalDate date) {
		switch (text) {
			case "KONTRAFUNK aktuell vom 3. Oktober":
			case "KONTRAFUNK: Der Wochenrückblick vom 30. September":
			case "KONTRAFUNK aktuell vom 7. September":
				text = text += " 2023";
				break;
			case "Wochenrückblick vom 16. Dezember 2023":
			case "Wochenrückblick vom 19. August 2023":
				text = "KONTRAFUNK: Der " + text;
				break;
			case "KONTRAFUNK aktuell vom 3.Juli 2023":
				text = text.replace("3.Juli", "3. Juli");
				break;
		}

		text = text
				.replaceAll("\\s{2,}", " ")
				.replace("Kontrafunk", "KONTRAFUNK")
				.replace(" der ", " Der ")
				.replace(" am ", " vom ");

		if (date.getYear() == 2022) {
			text = text
					.replace("2022_2", "2022")
					.replace(" aktuell: Der", ": Der")
					.replace(": Das Morgenmagazin", "")
					.replace("KONTRAFUNK vom", "KONTRAFUNK aktuell vom")
					.replace(" - ", " vom ");

			// aktuell <datum> -> aktuell vom <datum>
			if (text.matches(".*aktuell \\d+.*")) {
				String[] parts = text.split("aktuell");
				text = parts[0] + "aktuell vom" + parts[1];
			}

			// .22 -> .2022
			if (text.matches(".*vom (\\d|\\.)+\\.\\d\\d$")) {
				String[] parts = text.split("\\.");
				text = parts[0] + "." + parts[1] + ".20" + parts[2];
			}
		}

		// 9.8.2024 -> 09.8.2024
		if (text.matches(".*vom \\d\\..*")) {
			text = text.replace("vom ", "vom 0");
		}

		// 09.8.2023 -> 09.08.2023
		if (text.matches(".*vom \\d\\d\\.\\d\\..*")) {
			String[] parts = text.split("\\.");
			text = parts[0] + ".0" + parts[1] + "." + parts[2];
		}

		// 09.08.2023 -> 09. August 2023
		Pattern pattern = Pattern.compile(".*vom \\d\\d\\.\\d\\d\\.\\d\\d\\d\\d.*");

		if (pattern.matcher(text).matches()) {
			String[] parts = text.split("\\.");
			String month = parts[1];
			String monthName = switch (month) {
				case "01" -> "Januar";
				case "02" -> "Februar";
				case "03" -> "März";
				case "04" -> "April";
				case "05" -> "Mai";
				case "06" -> "Juni";
				case "07" -> "Juli";
				case "08" -> "August";
				case "09" -> "September";
				case "10" -> "Oktober";
				case "11" -> "November";
				case "12" -> "Dezember";
				default -> month;
			};
			text = parts[0] + ". " + monthName + " " + parts[2];
		}

		// 01. August.2023 -> 01. August 2023
		if (text.matches(".*vom \\d\\d\\.\\D+\\.\\d\\d\\d\\d$")) {
			String[] parts = text.split("\\.");
			text = parts[0] + "." + parts[1] + " " + parts[2];
		}

		if (!text.matches(".*vom \\d\\d\\. \\D+ \\d\\d\\d\\d$")) {
			throw new AssertionError("Title normalization failed: " + text);
		}

		return text;
	}

	public record SpreakerItem(
			String title,
			String description,
			String episodeApiLink,
			LocalDate publicationDate,
			LocalDate targetDate,
			String fileUrl,
			String fileBytes,
			String fileMimeType,
			String durationInSeconds,
			EpisodeType episodeType) { }
}

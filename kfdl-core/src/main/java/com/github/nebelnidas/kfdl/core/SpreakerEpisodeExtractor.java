package com.github.nebelnidas.kfdl.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class SpreakerEpisodeExtractor {
	private static final DateTimeFormatter spreakerDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
	private static final DateTimeFormatter titleDateFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN);

	private SpreakerEpisodeExtractor() {
	}

	/**
	 * Parses the item tags from an XML feed.
	 *
	 * @param inputStream The input stream of the XML feed.
	 * @return A list of {@link SpreakerEpisodeData} objects.
	 * @throws UnsupportedEncodingException
	 */
	public static Iterator<SpreakerEpisodeData> iterateItems(InputStream inputStream) throws XMLStreamException, UnsupportedEncodingException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader reader = factory.createXMLStreamReader(new InputStreamReader(inputStream, "UTF-8"));

		return new SpreakerIterator(reader);
	}

	private static class SpreakerIterator implements Iterator<SpreakerEpisodeData> {
		private final XMLStreamReader reader;
		private SpreakerEpisodeData next;

		SpreakerIterator(XMLStreamReader reader) {
			this.reader = reader;
		}

		@Override
		public boolean hasNext() {
			try {
				if (next == null) {
					next = readNext();
				}

				return next != null;
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public SpreakerEpisodeData next() {
			try {
				if (next == null) {
					next = readNext();
				}

				SpreakerEpisodeData ret = next;
				next = null;

				return ret;
			} catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}

		private SpreakerEpisodeData readNext() throws XMLStreamException {
			String title = null;
			String description = null;
			String episodeApiLink = null;
			String pubDate = null;
			String fileUrl = null;
			String fileMimeType = null;
			int fileBytes = -1;
			int durationInSeconds = -1;

			while (reader.hasNext()) {
				int event = reader.next();

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
						fileMimeType = reader.getAttributeValue(null, "type");
						fileBytes = Integer.parseInt(reader.getAttributeValue(null, "length"));
					} else if ("duration".equals(elementName)) {
						durationInSeconds = Integer.parseInt(reader.getElementText());
					}
				}

				if (event == XMLStreamReader.END_ELEMENT && "item".equals(reader.getLocalName())) {
					LocalDate publicationDate = LocalDate.parse(pubDate, spreakerDateFormatter);

					if (shouldSkipEpisode(title, publicationDate)) {
						continue;
					}

					title = fixTitle(title, publicationDate);
					LocalDate titleDate = getTitleDate(title, publicationDate);

					return new SpreakerEpisodeData(
							Objects.requireNonNull(title),
							description,
							Objects.requireNonNull(episodeApiLink),
							Objects.requireNonNull(publicationDate),
							Objects.requireNonNull(titleDate),
							Objects.requireNonNull(fileUrl),
							Objects.requireNonNull(fileMimeType),
							requireNotNegative(fileBytes),
							requireNotNegative(durationInSeconds),
							title.contains("Wochenrückblick") || titleDate.toString().equals("2022-07-23")
									? EpisodeType.WOCHENRÜCKBLICK
									: EpisodeType.AKTUELL);
				}
			}

			return null;
		}

		private boolean shouldSkipEpisode(String title, LocalDate date) {
			switch (title) {
				case "KONTRAFUNK aktuell: Vereiteltes Attentat auf Trump – Collin McMahon im Gespräch": // not a full episode
					return true;
				default:
					return false;
			}
		}

		private String fixTitle(String title, LocalDate date) {
			switch (title) {
				case "KONTRAFUNK aktuell vom 27. September 2024 Mittagsausgabe":
					title = title.replace(" Mittagsausgabe", "");
					break;
				case "KONTRAFUNK: Wochenrückblick vom 17. September 2024":
					assert date.toString().equals("2024-09-07");
					title = title.replace("17", "07");
					break;
				case "KONTRAFUNK: Wochenrückblick vom 21. Juli 2024":
					assert date.toString().equals("2024-07-20");
					title = title.replace("21", "20");
					break;
				case "KONTRAFUNK: Wochenrückblick vom 6. Juni 2024":
					assert date.toString().equals("2024-07-06");
					title = title.replace("Juni", "Juli");
					break;
				case "KONTRAFUNK aktuell vom 25.Juni 2024":
				case "KONTRAFUNK aktuell vom 19.Juni 2024":
					title = title.replace(".Juni", ". Juni");
					break;
				case "KONTRAFUNK: Wochenrückblick vom 14. Juni 2024":
					assert date.toString().equals("2024-06-15");
					title = title.replace("14", "15");
					break;
				case "KONTRAFUNK aktuell vom 11. Januar 2023":
					if (date.getYear() == 2024) {
						title = title.replace("2023", "2024");
					}

					break;
				case "KONTRAFUNK aktuell vom 3. Oktober":
				case "KONTRAFUNK: Wochenrückblick vom 30. September":
				case "KONTRAFUNK aktuell vom 7. September":
					assert date.getYear() == 2023;
					title += " 2023";
					break;
				case "Wochenrückblick vom 16. Dezember 2023":
				case "Wochenrückblick vom 19. August 2023":
					title = "KONTRAFUNK: Der " + title;
					break;
				case "KONTRAFUNK: Wochenrückblick vom 29. Juli 2023":
					if (date.toString().equals("2023-08-05")) {
						title = title.replace("29. Juli", "5. August");
					}

					break;
				case "KONTRAFUNK aktuell vom 3.Juli 2023":
					title = title.replace("3.Juli", "3. Juli");
					break;
				case "KONTRAFUNK: Wochenrückblick vom 24. Juni 2023":
					if (date.toString().equals("2023-07-01")) {
						title = title.replace("24. Juni", "1. Juli");
					}

					break;
				case "KONTRAFUNK: Wochenrückblick vom 10. Juni 2023":
					if (date.toString().equals("2023-06-17")) {
						title = title.replace("10", "17");
					}

					break;
				case "KONTRAFUNK: Der Wochenrückblick vom 18. Mai 2023":
					if (date.toString().equals("2023-03-18")) {
						title = title.replace("Mai", "März");
					}

					break;
				case "KONTRAFUNK aktuell vom 23. Juli 2023":
					assert date.getYear() == 2022;
					title = title.replace("2023", "2022");
					break;
			}

			title = title
					.replaceAll("\\s{2,}", " ")
					.replace("Kontrafunk", "KONTRAFUNK")
					.replace(" der ", " Der ")
					.replace(" am ", " vom ");

			if (date.getYear() == 2022) {
				title = title
						.replace("2022_2", "2022")
						.replace(" aktuell: Der", ": Der")
						.replace(": Das Morgenmagazin", "")
						.replace("KONTRAFUNK vom", "KONTRAFUNK aktuell vom")
						.replace(" - ", " vom ");

				// aktuell <datum> -> aktuell vom <datum>
				if (title.matches(".*aktuell \\d+.*")) {
					String[] parts = title.split("aktuell");
					title = parts[0] + "aktuell vom" + parts[1];
				}

				// .22 -> .2022
				if (title.matches(".*vom (\\d|\\.)+\\.\\d\\d$")) {
					String[] parts = title.split("\\.");
					title = parts[0] + "." + parts[1] + ".20" + parts[2];
				}
			}

			// 9.8.2024 -> 09.8.2024
			if (title.matches(".*vom \\d\\..*")) {
				title = title.replace("vom ", "vom 0");
			}

			// 09.8.2023 -> 09.08.2023
			if (title.matches(".*vom \\d\\d\\.\\d\\..*")) {
				String[] parts = title.split("\\.");
				title = parts[0] + ".0" + parts[1] + "." + parts[2];
			}

			// 09.08.2023 -> 09. August 2023
			Pattern pattern = Pattern.compile(".*vom \\d\\d\\.\\d\\d\\.\\d\\d\\d\\d.*");

			if (pattern.matcher(title).matches()) {
				String[] parts = title.split("\\.");
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
				title = parts[0] + ". " + monthName + " " + parts[2];
			}

			// 01. August.2023 -> 01. August 2023
			if (title.matches(".*vom \\d\\d\\.\\D+\\.\\d\\d\\d\\d$")) {
				String[] parts = title.split("\\.");
				title = parts[0] + "." + parts[1] + " " + parts[2];
			}

			if (!title.matches(".*vom \\d\\d\\. \\D+ \\d\\d\\d\\d$")) {
				throw new AssertionError("Title normalization failed: " + title);
			}

			return title;
		}

		private LocalDate getTitleDate(String normalizedTitle, LocalDate publicationDate) {
			LocalDate titleDate = LocalDate.parse(normalizedTitle.substring(normalizedTitle.indexOf("vom") + 4), titleDateFormatter);

			if (publicationDate.getMonthValue() != titleDate.getMonthValue()
					|| publicationDate.getMonthValue() != titleDate.getMonthValue()
					|| publicationDate.getDayOfMonth() != titleDate.getDayOfMonth()) {
				switch (normalizedTitle) {
					case "KONTRAFUNK aktuell vom 08. November 2023": // released 2023-11-07
					case "KONTRAFUNK aktuell vom 28. März 2023":     // released 2023-03-27
					case "KONTRAFUNK aktuell vom 16. November 2022": // released 2022-11-15
					case "KONTRAFUNK aktuell vom 28. Oktober 2022":  // released 2022-10-27
					case "KONTRAFUNK aktuell vom 10. Oktober 2022":  // released 2022-10-12
					case "KONTRAFUNK aktuell vom 23. Juni 2022":     // released 2022-06-25
					case "KONTRAFUNK aktuell vom 22. Juni 2022":     // released 2022-06-25
					case "KONTRAFUNK aktuell vom 21. Juni 2022":     // released 2022-06-25
						break;
					default:
						Kfdl.LOGGER.warn("Publication date and title date do not match for episode '{}': publication date is {}, title date is {}",
								normalizedTitle, publicationDate, titleDate);
				};
			}

			return titleDate;
		}
	}

	private static int requireNotNegative(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("Value must not be negative: " + value);
		}

		return value;
	}

	public record SpreakerEpisodeData(
			String title,
			String description,
			String episodeApiLink,
			LocalDate publicationDate,
			LocalDate date,
			String fileUrl,
			String fileMimeType,
			int fileBytes,
			int durationInSeconds,
			EpisodeType episodeType) { }
}

package com.github.nebelnidas.kfdl.core.impl.aktuell.scraper;

import java.time.LocalDate;

import com.github.nebelnidas.kfdl.core.scraper.ScrapeResult;
import com.github.nebelnidas.kfdl.core.scraper.Scraper;
import com.github.nebelnidas.kfdl.core.show.Show;

public record KfAktuellSpreakerScrapeResult(
	Scraper origin,
	String title,
	String description,
	String episodeApiLink,
	LocalDate publicationDate,
	LocalDate date,
	String fileUrl,
	String fileMimeType,
	int fileBytes,
	int durationInSeconds,
	Show show) implements ScrapeResult {
}

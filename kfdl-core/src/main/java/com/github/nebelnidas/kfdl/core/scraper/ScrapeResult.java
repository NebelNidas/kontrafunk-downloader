package com.github.nebelnidas.kfdl.core.scraper;

import java.time.LocalDate;

public interface ScrapeResult extends Comparable<ScrapeResult> {
	Scraper origin();
	LocalDate date();
	String title();

	@Override
	default int compareTo(ScrapeResult o) {
		return date().compareTo(o.date());
	}
}

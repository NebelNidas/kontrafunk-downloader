package com.github.nebelnidas.kfdl.core.scraper;

public interface TargetedScraper<T extends ScrapeTarget> extends Scraper {
	ScrapeResult scrape(T target);
}

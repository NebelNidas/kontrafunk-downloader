package com.github.nebelnidas.kfdl.core.scraper;

import java.util.Iterator;

public interface LinearScraper extends Scraper {
	Iterator<? extends ScrapeResult> scrape();
}

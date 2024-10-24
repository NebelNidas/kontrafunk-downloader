package com.github.nebelnidas.kfdl.core.scraper;

import java.util.Collection;

import com.github.nebelnidas.kfdl.core.show.Show;

public interface Scraper {
	String getLocalId();
	Collection<Show> getScrapeableShows();
}

package com.github.nebelnidas.kfdl.core.impl.aktuell.scraper;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.nebelnidas.kfdl.core.Person;
import com.github.nebelnidas.kfdl.core.Tag;
import com.github.nebelnidas.kfdl.core.scraper.ScrapeResult;

public interface KfAktuellKontrafunkScrapeResult extends ScrapeResult {
	@Nullable String url();
	Person host();
	List<Person> guests();
	@Nullable Person beitragAuthor();
	@Nullable Person commentAuthor();
	@Nullable String description();
	@Nullable String downloadLink();
	List<Tag> tags();
	@Nullable String nextEpisodeUrl();
}

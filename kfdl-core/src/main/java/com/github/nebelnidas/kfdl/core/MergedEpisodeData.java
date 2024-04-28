package com.github.nebelnidas.kfdl.core;

import java.time.LocalDate;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.WebsiteEpisodeData;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerEpisodeData;

public record MergedEpisodeData(
		String title,
		String description,
		LocalDate date,
		String fileUrl,
		EpisodeType episodeType,
		Person host,
		Person commentAuthor) {
	public MergedEpisodeData(SpreakerEpisodeData spreakerEntry, WebsiteEpisodeData scrapedData) {
		this(spreakerEntry.title(),
				spreakerEntry.description(),
				spreakerEntry.publicationDate(),
				spreakerEntry.fileUrl(),
				spreakerEntry.episodeType(),
				scrapedData.host(),
				scrapedData.commentAuthor());
	}
}

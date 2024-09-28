package com.github.nebelnidas.kfdl.core;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import lombok.With;

import com.github.nebelnidas.kfdl.core.KontrafunkScraper.WebsiteEpisodeData;
import com.github.nebelnidas.kfdl.core.SpreakerEpisodeExtractor.SpreakerEpisodeData;

@With
public record MergedEpisodeData(
		String siteUrl,
		String title,
		String description,
		LocalDate date,
		/** Spreaker's potentially cut version of the podcast. */
		String spreakerDownload,
		/** The full (uncut) version if available, otherwise the Spreaker URL. */
		String defaultDownload,
		EpisodeType episodeType,
		Person host,
		List<Person> guests,
		Person commentAuthor,
		List<Tag> tags) {
	public MergedEpisodeData(SpreakerEpisodeData spreakerEntry, WebsiteEpisodeData scrapedData) {
		this(scrapedData.url(),
				Objects.requireNonNull(spreakerEntry.title()),
				scrapedData.description() != null ? scrapedData.description() : spreakerEntry.description(),
				Objects.requireNonNull(spreakerEntry.publicationDate()),
				Objects.requireNonNull(spreakerEntry.fileUrl()),
				Objects.requireNonNull(scrapedData.downloadLink() != null ? scrapedData.downloadLink() : spreakerEntry.fileUrl()),
				Objects.requireNonNull(spreakerEntry.episodeType()),
				Objects.requireNonNull(scrapedData.host()),
				Objects.requireNonNull(scrapedData.guests()),
				scrapedData.commentAuthor(),
				Objects.requireNonNull(scrapedData.tags()));
	}
}

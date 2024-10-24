package com.github.nebelnidas.kfdl.core.impl.aktuell.episode;

import java.time.LocalDate;
import java.util.List;

import com.github.nebelnidas.kfdl.core.Person;
import com.github.nebelnidas.kfdl.core.Tag;
import com.github.nebelnidas.kfdl.core.episode.Episode;
import com.github.nebelnidas.kfdl.core.show.Show;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder
@Getter
@Accessors(fluent = true)
public class KfAktuellEpisode implements Episode {
	private Show show;
	private String webUrl;
	private String title;
	private String description;
	private LocalDate date;
	/** Spreaker's potentially cut version of the podcast. */
	private String spreakerDownload;
	/** The full (uncut) version if available, otherwise the Spreaker URL. */
	private String defaultDownload;
	private Person host;
	private List<Show> guests;
	private Show commentAuthor;
	private List<Tag> tags;
}

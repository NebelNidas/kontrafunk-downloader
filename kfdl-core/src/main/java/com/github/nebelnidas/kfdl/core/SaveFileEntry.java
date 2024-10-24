package com.github.nebelnidas.kfdl.core;

import java.time.LocalDate;

import lombok.With;

@With
public record SaveFileEntry(LocalDate episodeDate, String downloadLink, DownloadState downloadState) {
	public SaveFileEntry(MergedEpisodeData episode, DownloadState downloadState) {
		this(episode.date(), episode.defaultDownload(), downloadState);
	}
}

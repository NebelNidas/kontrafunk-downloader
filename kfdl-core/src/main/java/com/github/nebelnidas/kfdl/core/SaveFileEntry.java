package com.github.nebelnidas.kfdl.core;

import java.time.LocalDate;

public record SaveFileEntry(LocalDate episodeDate, String downloadLink, DownloadState downloadState) {
	public SaveFileEntry(MergedEpisodeData episode, DownloadState downloadState) {
		this(episode.date(), episode.defaultDownload(), downloadState);
	}

	public SaveFileEntry withDownloadState(DownloadState downloadState) {
		return new SaveFileEntry(episodeDate, downloadLink, downloadState);
	}

	public SaveFileEntry withDownloadLink(String downloadLink) {
		return new SaveFileEntry(episodeDate, downloadLink, downloadState);
	}

	public SaveFileEntry withEpisodeDate(LocalDate episodeDate) {
		return new SaveFileEntry(episodeDate, downloadLink, downloadState);
	}
}

package com.github.nebelnidas.kfdl.core;

public enum DownloadState {
	QUEUED,
	DOWNLOADING,
	SUCCESSFUL,
	FAILED;

	static DownloadState parse(String string) {
		for (DownloadState state : DownloadState.values()) {
			if (state.name().equals(string)) {
				return state;
			}
		}

		throw new IllegalArgumentException("Unknown download state: " + string);
	}
}

package com.github.nebelnidas.kfdl.core.episode;

import java.util.List;

import com.github.nebelnidas.kfdl.core.Person;

public interface EpisodeFragment {
	EpisodeVariant origin();

	String webUrl();
	String fileUrl();
	List<Person> guests();
}

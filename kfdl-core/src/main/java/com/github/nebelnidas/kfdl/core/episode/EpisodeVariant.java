package com.github.nebelnidas.kfdl.core.episode;

import java.util.List;

import com.github.nebelnidas.kfdl.core.Person;
import com.github.nebelnidas.kfdl.core.Tag;

public interface EpisodeVariant {
	Episode origin();

	String webUrl();
	String fileUrl();
	List<Person> guests();
	List<Tag> tags();
	List<EpisodeFragment> fragments();
}

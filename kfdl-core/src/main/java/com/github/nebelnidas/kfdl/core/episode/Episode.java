package com.github.nebelnidas.kfdl.core.episode;

import java.time.LocalDate;

import com.github.nebelnidas.kfdl.core.Person;
import com.github.nebelnidas.kfdl.core.show.Show;

public interface Episode {
	Show show();

	String webUrl();
	String title();
	String description();
	LocalDate date();
	Person host();
}

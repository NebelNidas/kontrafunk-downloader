package com.github.nebelnidas.kfdl.core;

import java.util.HashMap;
import java.util.Map;

public record Person(String name) {
	public static final Map<String, Person> personsByName = new HashMap<>();

	public static final Person MARCEL_JOPPA = getOrCreate("Marcel Joppa");

	public static Person getOrCreate(String name) {
		return personsByName.computeIfAbsent(name.trim(), Person::new);
	}
}

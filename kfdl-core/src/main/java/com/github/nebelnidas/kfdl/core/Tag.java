package com.github.nebelnidas.kfdl.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Value;

@Value
public class Tag {
	static final Map<String, Tag> tagsById = new HashMap<>();
	private final String id;
	private final String name;

	public static Tag getOrCreate(String name) {
		String trimmedName = name.trim();
		String id = trimmedName.toLowerCase(Locale.ROOT);
		return tagsById.computeIfAbsent(id, key -> new Tag(id, trimmedName));
	}
}

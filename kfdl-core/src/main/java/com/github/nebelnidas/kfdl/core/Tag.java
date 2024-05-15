package com.github.nebelnidas.kfdl.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Tag {
	private static final Map<String, Tag> tagsById = new HashMap<>();
	private final String id;
	private final String name;

	public static Tag getOrCreate(String name) {
		String trimmedName = name.trim();
		String id = trimmedName.toLowerCase(Locale.ROOT);
		return tagsById.computeIfAbsent(id, key -> new Tag(id, trimmedName));
	}

	public static Collection<Tag> values() {
		return Collections.unmodifiableCollection(tagsById.values());
	}
}

package com.github.nebelnidas.kfdl.core;

import java.util.HashMap;
import java.util.Map;

public record Person(String name) {
	static final Map<String, Person> personsByName = new HashMap<>();

	public static final Person BENJAMIN_GOLLME = getOrCreate("Benjamin Gollme");
	public static final Person MARCEL_JOPPA = getOrCreate("Marcel Joppa");
	public static final Person TIM_KRAUSE = getOrCreate("Tim Krause");
	public static final Person JASMIN_KOSUBEK = getOrCreate("Jasmin Kosubek");
	public static final Person MICHAEL_GÖRMANN = getOrCreate("Michael Görmann");
	public static final Person BENNY_PEISER = getOrCreate("Benny Peiser");
	public static final Person CHRISTIAN_FIALA = getOrCreate("Christian Fiala");
	public static final Person UTE_BERGNER = getOrCreate("Ute Bergner");
	public static final Person SUSANNE_DAGEN = getOrCreate("Susanne Dagen");
	public static final Person GUNTER_FRANK = getOrCreate("Gunter Frank");
	public static final Person CORA_STEPHAN = getOrCreate("Cora Stephan");
	public static final Person WERNER_KIRSTEIN = getOrCreate("Werner Kirstein");
	public static final Person HANS_PETER_DIETZ = getOrCreate("Hans Peter Dietz");
	public static final Person OLIVER_HOLZER = getOrCreate("Oliver Holzer");

	public static Person getOrCreate(String name) {
		return personsByName.computeIfAbsent(name.trim(), Person::new);
	}
}

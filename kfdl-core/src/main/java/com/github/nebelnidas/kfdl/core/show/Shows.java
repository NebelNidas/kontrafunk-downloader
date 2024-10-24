package com.github.nebelnidas.kfdl.core.show;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Shows {
	private static final Map<String, Show> showsByName = new HashMap<>();

	public static final Show POLITIK_UND_ZEITGESCHEHEN = register("Politik und Zeitgeschehen");
	public static final Show KONTRAFUNK_AKTUELL = register("Kontrafunk aktuell", POLITIK_UND_ZEITGESCHEHEN);
	public static final Show DIE_SONNTAGSRUNDE = register("Die Sonntagsrunden", POLITIK_UND_ZEITGESCHEHEN);
	public static final Show WOCHENRÜCKBLICK = register("Wochenrückblick", POLITIK_UND_ZEITGESCHEHEN);

	public static final Show KULTUR_UND_WISSENSCHAFT = register("Kultur und Wissenschaft");
	public static final Show AUDIMAX = register("Audimax", KULTUR_UND_WISSENSCHAFT);
	public static final Show LEIB_UND_SPEISE = register("Leib und Speise", KULTUR_UND_WISSENSCHAFT);
	public static final Show LESESTUNDE = register("Lesestunde", KULTUR_UND_WISSENSCHAFT);
	public static final Show MUSIKSTUNDE = register("Musikstunde", KULTUR_UND_WISSENSCHAFT);
	public static final Show TONDOKUMENT = register("Tondokument", KULTUR_UND_WISSENSCHAFT);
	public static final Show WELTWUNDERFORUM = register("Weltwunderforum", KULTUR_UND_WISSENSCHAFT);

	public static final Show LEBENSWELTEN = register("Lebenswelten");
	public static final Show BASTA_BERLIN = register("Basta Berlin", LEBENSWELTEN);
	public static final Show DER_RECHTSSTAAT = register("Der Rechtsstaat", LEBENSWELTEN);
	public static final Show DREILÄNDERECK = register("Dreiländereck", LEBENSWELTEN);
	public static final Show FERNRUF = register("Fernruf", LEBENSWELTEN);
	public static final Show WIRTSCHAFT_UND_GESELLSCHAFT = register("Wirtschaft und Gesellschaft", LEBENSWELTEN);
	public static final Show LEHRERZIMMER = register("Lehrerzimmer", LEBENSWELTEN);
	public static final Show MENSCHENBILDER = register("Menschenbilder", LEBENSWELTEN);
	public static final Show MENSCH_UND_MEDIZIN = register("Mensch und Medizin", LEBENSWELTEN);
	public static final Show UNTER_FREUNDEN = register("Unter Freunden", LEBENSWELTEN);

	public static final Show TALKSHOW = register("Talkshow");
	public static final Show WER_SPRICHT = register("Wer spricht?", TALKSHOW);
	public static final Show ARGOS_OHREN = register("Argos Ohren", TALKSHOW);
	public static final Show HALLO_KONTRAFUNK = register("Hallo Kontrafunk", TALKSHOW);
	public static final Show LUDGERS_WELT = register("Ludgers Welt", TALKSHOW);
	public static final Show MATUSSEK = register("Matussek!", TALKSHOW);
	public static final Show PHILOSOPHIEREN = register("Philosophieren", TALKSHOW);
	public static final Show YOYOGAGA = register("Yoyogaga", TALKSHOW);

	public static final Show SCHWEIZERZEIT = register("Schweizerzeit");
	public static final Show KIRCHE = register("Kirche im Kontrafunk");
	public static final Show SONDERSENDUNGEN = register("Sondersendungen");

	public static Show get(String name) {
		return showsByName.get(name.trim());
	}

	public static Show register(String name, Show... parents) {
		return showsByName.put(name.trim(), new Show(name, parents));
	}

	public static Collection<Show> values() {
		return Collections.unmodifiableCollection(showsByName.values());
	}
}

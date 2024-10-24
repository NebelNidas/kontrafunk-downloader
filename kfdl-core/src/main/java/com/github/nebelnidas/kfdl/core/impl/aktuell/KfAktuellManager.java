package com.github.nebelnidas.kfdl.core.impl.aktuell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.stream.Streams;
import org.apache.commons.lang3.tuple.Pair;

import com.github.nebelnidas.kfdl.core.Kfdl;
import com.github.nebelnidas.kfdl.core.episode.EpisodeVariant;
import com.github.nebelnidas.kfdl.core.impl.aktuell.scraper.KfAktuellKontrafunkScrapeResult;
import com.github.nebelnidas.kfdl.core.impl.aktuell.scraper.KfAktuellSpreakerScrapeResult;
import com.github.nebelnidas.kfdl.core.impl.aktuell.scraper.KfAktuellSpreakerScraper;
import com.github.nebelnidas.kfdl.core.impl.aktuell.scraper.LinearKfAktuellKontrafunkScraper;
import com.github.nebelnidas.kfdl.core.scraper.ScrapeResult;
import com.github.nebelnidas.kfdl.core.show.Shows;

public class KfAktuellManager {
	private List<EpisodeVariant> episodes = new ArrayList<>();

	public void group() {
		List<Pair<KfAktuellSpreakerScrapeResult, KfAktuellKontrafunkScrapeResult>> scrapeResults = new ArrayList<>();

		Iterator<KfAktuellSpreakerScrapeResult> spreaker = new KfAktuellSpreakerScraper().scrape();
		Iterator<KfAktuellKontrafunkScrapeResult> kontrafunk = new LinearKfAktuellKontrafunkScraper().scrape();

		List<KfAktuellSpreakerScrapeResult> spreakerItems = Streams.of(spreaker)
				.filter(item -> item.show().isOfType(Shows.KONTRAFUNK_AKTUELL))
				.sorted((a, b) -> b.compareTo(a))
				.toList();
		List<ScrapeResult> partiallyMissingItems = new ArrayList<>();

		while (kontrafunk.hasNext()) {
			KfAktuellKontrafunkScrapeResult kontrafunkItem = kontrafunk.next();

			int index = Collections.binarySearch(spreakerItems, kontrafunkItem, (a, b) -> b.compareTo(a));
			KfAktuellSpreakerScrapeResult spreakerItem = index >= 0 ? spreakerItems.remove(index) : null;

			if (spreakerItem == null) {
				partiallyMissingItems.add(kontrafunkItem);
			}

			scrapeResults.add(Pair.of(spreakerItem, kontrafunkItem));
		}

		spreakerItems.forEach(item -> scrapeResults.add(Pair.of(item, null)));

		scrapeResults.sort((a, b) -> {
			ScrapeResult left = a.getLeft() != null ? a.getLeft() : a.getRight();
			ScrapeResult right = b.getLeft() != null ? b.getLeft() : b.getRight();

			return right.compareTo(left);
		});

		partiallyMissingItems.addAll(spreakerItems);
		partiallyMissingItems.sort((a, b) -> b.compareTo(a));

		for (ScrapeResult item : partiallyMissingItems) {
			Kfdl.LOGGER.warn("Episode supplied only by {}: {}", item.origin().getLocalId(), item.title());
		}
	}

	public void merge() {
	}
}

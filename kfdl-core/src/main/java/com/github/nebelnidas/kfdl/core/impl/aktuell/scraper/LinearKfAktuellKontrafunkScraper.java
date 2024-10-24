package com.github.nebelnidas.kfdl.core.impl.aktuell.scraper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import lombok.SneakyThrows;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;

import com.github.nebelnidas.kfdl.core.scraper.LinearScraper;
import com.github.nebelnidas.kfdl.core.show.Show;
import com.github.nebelnidas.kfdl.core.show.Shows;

public class LinearKfAktuellKontrafunkScraper implements LinearScraper {
	private static final String kfAktuellOverviewUrl = "https://kontrafunk.radio/de/sendung-nachhoeren/politik-und-zeitgeschehen/kontrafunk-aktuell/";

	@Override
	public String getLocalId() {
		return "kontrafunk-website";
	}

	@Override
	public Collection<Show> getScrapeableShows() {
		return Set.of(Shows.KONTRAFUNK_AKTUELL);
	}

	@Override
	@SneakyThrows
	public Iterator<KfAktuellKontrafunkScrapeResult> scrape() {
		String latestEpisodeUrl;

		try (WebClient webClient = new WebClient()) {
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);

			HtmlPage page = webClient.getPage(kfAktuellOverviewUrl);
			latestEpisodeUrl = getLatestEpisodeUrl(page);
		}

		return new ScrapeResultIterator(latestEpisodeUrl);
	}

	private String getLatestEpisodeUrl(HtmlPage page) {
		HtmlAnchor link = page.getFirstByXPath("//*[@id=\"js-bd589\"]/div[1]/div/a");

		if (link == null) {
			throw new IllegalStateException("Could not find link to latest Kontrafunk Aktuell episode");
		}

		return link.getHrefAttribute();
	}

	private static class ScrapeResultIterator implements Iterator<KfAktuellKontrafunkScrapeResult> {
		private String nextEpisodeUrl;

		ScrapeResultIterator(String nextEpisodeUrl) {
			this.nextEpisodeUrl = nextEpisodeUrl;
		}

		@Override
		public boolean hasNext() {
			return nextEpisodeUrl != null;
		}

		@Override
		public KfAktuellKontrafunkScrapeResult next() {
			if (nextEpisodeUrl == null) {
				return null;
			}

			KfAktuellScrapeTarget target = new KfAktuellScrapeTarget(nextEpisodeUrl);
			KfAktuellKontrafunkScrapeResult ret = new TargetedKfAktuellKontrafunkScraper().scrape(target);
			nextEpisodeUrl = ret.nextEpisodeUrl();

			return ret;
		}
	}
}

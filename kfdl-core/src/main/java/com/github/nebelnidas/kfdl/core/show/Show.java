package com.github.nebelnidas.kfdl.core.show;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Show {
	private final String id;
	private final List<Show> parents = Collections.synchronizedList(new ArrayList<>());

	Show(String id) {
		this(id, new Show[0]);
	}

	Show(String id, Show... parents) {
		this.id = id;
		this.parents.addAll(List.of(parents));
	}

	public String getId() {
		return this.id;
	}

	public boolean isOfType(Show show) {
		return this.equals(show) || this.hasParent(show);
	}

	public boolean isOfType(String showId) {
		return this.id.equals(showId) || this.hasParent(showId);
	}

	public boolean hasParent(Show show) {
		synchronized (this.parents) {
			return this.parents.stream()
					.filter((parent) -> parent.equals(show) || parent.hasParent(show))
					.findAny()
					.isPresent();
		}
	}

	public boolean hasParent(String showId) {
		synchronized (this.parents) {
			return this.parents.stream()
					.filter((parent) -> parent.getId().equals(showId) || parent.hasParent(showId))
					.findAny()
					.isPresent();
		}
	}

	public void addParent(Show show) {
		this.parents.add(show);
	}

	/**
	 * {@return an unmodifiable view of the parent list}
	 */
	public List<Show> getParents() {
		return Collections.unmodifiableList(this.parents);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Show)) {
			return false;
		}

		Show other = (Show) obj;

		boolean sameId = other.getId().equals(this.id);
		boolean sameParents = other.getParents().equals(parents);

		return sameId && sameParents;
	}
}

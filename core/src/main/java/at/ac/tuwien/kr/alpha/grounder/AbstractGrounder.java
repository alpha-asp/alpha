package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.CorePredicate;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public abstract class AbstractGrounder implements Grounder {
	protected final java.util.function.Predicate<CorePredicate> filter;

	protected AbstractGrounder(java.util.function.Predicate<CorePredicate> filter) {
		this.filter = filter;
	}

	protected AbstractGrounder() {
		this(p -> true);
	}
}
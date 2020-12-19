package at.ac.tuwien.kr.alpha.core.common;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySortedSet;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import at.ac.tuwien.kr.alpha.core.atoms.CoreAtom;
import at.ac.tuwien.kr.alpha.core.util.Util;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
// TODO bring this into public non-core API by using something like an "answer-set-view"
public class BasicAnswerSet implements CoreAnswerSet {
	public static final BasicAnswerSet EMPTY = new BasicAnswerSet(emptySortedSet(), emptyMap());

	private final SortedSet<CorePredicate> predicates;
	private final Map<CorePredicate, SortedSet<CoreAtom>> predicateInstances;

	public BasicAnswerSet(SortedSet<CorePredicate> predicates, Map<CorePredicate, SortedSet<CoreAtom>> predicateInstances) {
		this.predicates = predicates;
		this.predicateInstances = predicateInstances;
	}

	@Override
	public SortedSet<CorePredicate> getPredicates() {
		return predicates;
	}

	@Override
	public SortedSet<CoreAtom> getPredicateInstances(CorePredicate predicate) {
		return predicateInstances.get(predicate);
	}

	@Override
	public boolean isEmpty() {
		return predicates.isEmpty();
	}

	@Override
	public String toString() {
		if (predicates.isEmpty()) {
			return "{}";
		}

		final StringBuilder sb = new StringBuilder("{ ");
		for (Iterator<CorePredicate> iterator = predicates.iterator(); iterator.hasNext();) {
			CorePredicate predicate = iterator.next();
			Set<CoreAtom> instances = getPredicateInstances(predicate);

			if (instances == null || instances.isEmpty()) {
				sb.append(predicate.getName());
				continue;
			}

			for (Iterator<CoreAtom> instanceIterator = instances.iterator(); instanceIterator.hasNext();) {
				sb.append(instanceIterator.next());
				if (instanceIterator.hasNext()) {
					sb.append(", ");
				}
			}

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" }");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BasicAnswerSet)) {
			return false;
		}

		BasicAnswerSet that = (BasicAnswerSet) o;

		if (!predicates.equals(that.predicates)) {
			return false;
		}

		return predicateInstances.equals(that.predicateInstances);
	}

	@Override
	public int hashCode() {
		return  31 * predicates.hashCode() + predicateInstances.hashCode();
	}

	@Override
	public int compareTo(CoreAnswerSet other) {
		final SortedSet<CorePredicate> predicates = this.getPredicates();
		int result = Util.compareSortedSets(predicates, other.getPredicates());

		if (result != 0) {
			return result;
		}

		for (CorePredicate predicate : predicates) {
			result = Util.compareSortedSets(this.getPredicateInstances(predicate), other.getPredicateInstances(predicate));

			if (result != 0) {
				return result;
			}
		}

		return 0;
	}
}
package at.ac.tuwien.kr.alpha.core.grounder;

import static at.ac.tuwien.kr.alpha.core.util.Util.oops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import at.ac.tuwien.kr.alpha.core.common.terms.CoreTerm;
import at.ac.tuwien.kr.alpha.core.common.terms.VariableTermImpl;

/**
 * A variable substitution allowing variables to occur on the right-hand side. Chains of variable substitutions are
 * resolved automatically, i.e., adding the substitutions (X -> A) and (A -> d) results in (X -> d), (A -> d).
 * Copyright (c) 2018-2020, the Alpha Team.
 */
public class Unifier extends Substitution {

	private final TreeMap<VariableTermImpl, List<VariableTermImpl>> rightHandVariableOccurrences;

	private Unifier(TreeMap<VariableTermImpl, CoreTerm> substitution, TreeMap<VariableTermImpl, List<VariableTermImpl>> rightHandVariableOccurrences) {
		if (substitution == null) {
			throw oops("Substitution is null.");
		}
		this.substitution = substitution;
		this.rightHandVariableOccurrences = rightHandVariableOccurrences;
	}

	public Unifier() {
		this(new TreeMap<>(), new TreeMap<>());
	}

	public Unifier(Unifier clone) {
		this(new TreeMap<>(clone.substitution), new TreeMap<>(clone.rightHandVariableOccurrences));
	}

	public Unifier(Substitution clone) {
		this(new TreeMap<>(clone.substitution), new TreeMap<>());
	}


	public Unifier extendWith(Substitution extension) {
		for (Map.Entry<VariableTermImpl, CoreTerm> extensionVariable : extension.substitution.entrySet()) {
			this.put(extensionVariable.getKey(), extensionVariable.getValue());
		}
		return this;
	}

	/**
	 * Returns a list of all variables occurring in that unifier, i.e., variables that are mapped and those that occur (nested) in the right-hand side of the unifier.
	 * @return the list of variables occurring somewhere in the unifier.
	 */
	@Override
	public Set<VariableTermImpl> getMappedVariables() {
		Set<VariableTermImpl> ret = new HashSet<>();
		for (Map.Entry<VariableTermImpl, CoreTerm> substitution : substitution.entrySet()) {
			ret.add(substitution.getKey());
			ret.addAll(substitution.getValue().getOccurringVariables());
		}
		return ret;
	}


	@Override
	public <T extends Comparable<T>> CoreTerm put(VariableTermImpl variableTerm, CoreTerm term) {
		// If term is not ground, store it for right-hand side reverse-lookup.
		if (!term.isGround()) {
			for (VariableTermImpl rightHandVariable : term.getOccurringVariables()) {
				rightHandVariableOccurrences.putIfAbsent(rightHandVariable, new ArrayList<>());
				rightHandVariableOccurrences.get(rightHandVariable).add(variableTerm);
			}
		}
		// Note: We're destroying type information here.
		CoreTerm ret = substitution.put(variableTerm, term);

		// Check if the just-assigned variable occurs somewhere in the right-hand side already.
		List<VariableTermImpl> rightHandOccurrences = rightHandVariableOccurrences.get(variableTerm);
		if (rightHandOccurrences != null) {
			// Replace all occurrences on the right-hand side with the just-assigned term.
			for (VariableTermImpl rightHandOccurrence : rightHandOccurrences) {
				// Substitute the right hand where this assigned variable occurs with the new value and store it.
				CoreTerm previousRightHand = substitution.get(rightHandOccurrence);
				if (previousRightHand == null) {
					// Variable does not occur on the lef-hand side, skip.
					continue;
				}
				substitution.put(rightHandOccurrence, previousRightHand.substitute(this));
			}
		}

		return ret;
	}

	/**
	 * Merge substitution right into left as used in the AnalyzeUnjustified.
	 * Left mappings are seen as equalities, i.e.,
	 * if left has A -> B and right has A -> t then the result will have A -> t and B -> t.
	 * If both substitutions are inconsistent, i.e., A -> t1 in left and A -> t2 in right, then null is returned.
	 * @param left
	 * @param right
	 * @return
	 */
	public static Unifier mergeIntoLeft(Unifier left, Unifier right) {
		// Note: we assume both substitutions are free of chains, i.e., no A->B, B->C but A->C, B->C.
		Unifier ret = new Unifier(left);
		for (Map.Entry<VariableTermImpl, CoreTerm> mapping : right.substitution.entrySet()) {
			VariableTermImpl variable = mapping.getKey();
			CoreTerm term = mapping.getValue();
			// If variable is unset, simply add.
			if (!ret.isVariableSet(variable)) {
				ret.put(variable, term);
				continue;
			}
			// Variable is already set.
			CoreTerm setTerm = ret.eval(variable);
			if (setTerm instanceof VariableTermImpl) {
				// Variable maps to another variable in left.
				// Add a new mapping of the setTerm variable into our right-assigned term.
				ret.put((VariableTermImpl) setTerm, term);
				// Note: Unifier.put takes care of resolving the chain variable->setTerm->term.
				continue;
			}
			// Check for inconsistency.
			if (setTerm != term) {
				return null;
			}
			// Now setTerm equals term, no action needed.
		}
		return ret;
	}
}

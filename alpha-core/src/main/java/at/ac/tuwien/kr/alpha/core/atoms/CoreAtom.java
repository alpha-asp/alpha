/**
 * Copyright (c) 2016-2020, the Alpha Team.
 * All rights reserved.
 * <p>
 * Additional changes made by Siemens.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p>
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.core.atoms;

import java.util.List;
import java.util.Set;

import at.ac.tuwien.kr.alpha.api.program.Atom;
import at.ac.tuwien.kr.alpha.core.common.CorePredicate;
import at.ac.tuwien.kr.alpha.core.common.terms.CoreTerm;
import at.ac.tuwien.kr.alpha.core.common.terms.VariableTermImpl;
import at.ac.tuwien.kr.alpha.core.grounder.Substitution;
import at.ac.tuwien.kr.alpha.core.grounder.Unifier;

/**
 * An Atom is the common superclass of all representations of ASP atoms used by Alpha.
 */
public abstract class CoreAtom implements Atom, Comparable<CoreAtom> {

	/**
	 * Creates a new Atom that represents this Atom, but has the given term list instead.
	 *
	 * @param terms the terms to set.
	 * @return a new Atom with the given terms set.
	 */
	public abstract CoreAtom withTerms(List<CoreTerm> terms);

	/**
	 * Returns the set of all variables occurring in the Atom.
	 */
	public Set<VariableTermImpl> getOccurringVariables() {
		return toLiteral().getOccurringVariables();
	}

	/**
	 * This method applies a substitution to the atom. Note that, depending on the atom and the substitution, the
	 * resulting atom may still contain variables.
	 *
	 * @param substitution the variable substitution to apply.
	 * @return the atom resulting from the application of the substitution.
	 */
	public abstract CoreAtom substitute(Substitution substitution);

	public abstract List<CoreTerm> getTerms();

	public abstract CorePredicate getPredicate();

	/**
	 * Returns whether this atom is ground, i.e., variable-free.
	 *
	 * @return true iff the terms of this atom contain no {@link VariableTermImpl}.
	 */
	public abstract boolean isGround();

	/**
	 * Creates a non-negated literal containing this atom.
	 */
	public CoreLiteral toLiteral() {
		return toLiteral(true);
	}

	public abstract CoreLiteral toLiteral(boolean positive);

	public CoreAtom renameVariables(String newVariablePrefix) {
		Unifier renamingSubstitution = new Unifier();
		int counter = 0;
		for (VariableTermImpl variable : getOccurringVariables()) {
			renamingSubstitution.put(variable, VariableTermImpl.getInstance(newVariablePrefix + counter++));
		}
		return this.substitute(renamingSubstitution);
	}

	@Override
	public int compareTo(CoreAtom o) {
		if (o == null) {
			return 1;
		}

		final List<? extends CoreTerm> aTerms = this.getTerms();
		final List<? extends CoreTerm> bTerms = o.getTerms();

		if (aTerms.size() != bTerms.size()) {
			return Integer.compare(aTerms.size(), bTerms.size());
		}

		int result = this.getPredicate().compareTo(o.getPredicate());

		if (result != 0) {
			return result;
		}

		for (int i = 0; i < aTerms.size(); i++) {
			result = aTerms.get(i).compareTo(o.getTerms().get(i));
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public abstract int hashCode();

}

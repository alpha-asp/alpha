package at.ac.tuwien.kr.alpha.common.terms;

import at.ac.tuwien.kr.alpha.common.Interner;
import at.ac.tuwien.kr.alpha.grounder.IntIdGenerator;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class Variable extends Term {
	private static final Interner<Variable> INTERNER = new Interner<>();

	private static final String ANONYMOUS_VARIABLE_PREFIX = "_";
	private static final IntIdGenerator ANONYMOUS_VARIABLE_COUNTER = new IntIdGenerator();

	private final String variableName;

	private Variable(String variableName) {
		this.variableName = variableName;
	}

	public static Variable getInstance(String variableName) {
		return INTERNER.intern(new Variable(variableName));
	}

	public static Variable getAnonymousInstance() {
		return getInstance(ANONYMOUS_VARIABLE_PREFIX + ANONYMOUS_VARIABLE_COUNTER.getNextId());
	}

	@Override
	public boolean isGround() {
		return false;
	}

	@Override
	public List<Variable> getOccurringVariables() {
		return Collections.singletonList(this);
	}

	@Override
	public Term substitute(Substitution substitution) {
		Term groundTerm = substitution.eval(this);
		if (groundTerm == null) {
			// If variable is not substituted, keep term as is.
			return this;
		}
		return  groundTerm;
	}

	@Override
	public String toString() {
		return variableName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Variable that = (Variable) o;

		return variableName.equals(that.variableName);
	}

	@Override
	public int hashCode() {
		return variableName.hashCode();
	}

	@Override
	public int compareTo(Term o) {
		if (this == o) {
			return 0;
		}

		if (!(o instanceof Variable)) {
			return super.compareTo(o);
		}

		Variable other = (Variable)o;
		return variableName.compareTo(other.variableName);
	}
}
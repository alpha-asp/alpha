package at.ac.tuwien.kr.alpha.common.atoms;

import at.ac.tuwien.kr.alpha.common.BinaryOperator;
import at.ac.tuwien.kr.alpha.common.predicates.Predicate;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;

import java.util.List;

/**
 * Represents the head of a choice rule.
 * Copyright (c) 2017, the Alpha Team.
 */
public class ChoiceHead implements Atom {
	private final List<ChoiceElement> choiceElements;
	private final Term lowerBound;
	private final BinaryOperator lowerOp;
	private final Term upperBound;
	private final BinaryOperator upperOp;

	public static class ChoiceElement {
		public final BasicAtom choiceAtom;
		public final List<Literal> conditionLiterals;

		public ChoiceElement(BasicAtom choiceAtom, List<Literal> conditionLiterals) {
			this.choiceAtom = choiceAtom;
			this.conditionLiterals = conditionLiterals;
		}
	}

	public BinaryOperator getLowerOp() {
		return lowerOp;
	}

	public BinaryOperator getUpperOp() {
		return upperOp;
	}

	public List<ChoiceElement> getChoiceElements() {
		return choiceElements;
	}

	public Term getLowerBound() {
		return lowerBound;
	}

	public Term getUpperBound() {
		return upperBound;
	}

	public ChoiceHead(List<ChoiceElement> choiceElements, Term lowerBound, BinaryOperator lowerOp, Term upperBound, BinaryOperator upperOp) {
		this.choiceElements = choiceElements;
		this.lowerBound = lowerBound;
		this.lowerOp = lowerOp;
		this.upperBound = upperBound;
		this.upperOp = upperOp;
	}

	@Override
	public Predicate getPredicate() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Term> getTerms() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isGround() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isInternal() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<VariableTerm> getBindingVariables() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<VariableTerm> getNonBindingVariables() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Atom substitute(Substitution substitution) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareTo(Atom o) {
		throw new UnsupportedOperationException();
	}
}
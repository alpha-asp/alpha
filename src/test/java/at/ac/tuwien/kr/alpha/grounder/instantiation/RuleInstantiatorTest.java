package at.ac.tuwien.kr.alpha.grounder.instantiation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import at.ac.tuwien.kr.alpha.common.ComparisonOperator;
import at.ac.tuwien.kr.alpha.common.Predicate;
import at.ac.tuwien.kr.alpha.common.atoms.BasicAtom;
import at.ac.tuwien.kr.alpha.common.atoms.BasicLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonAtom;
import at.ac.tuwien.kr.alpha.common.atoms.ComparisonLiteral;
import at.ac.tuwien.kr.alpha.common.atoms.Literal;
import at.ac.tuwien.kr.alpha.common.terms.ConstantTerm;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import at.ac.tuwien.kr.alpha.common.terms.VariableTerm;
import at.ac.tuwien.kr.alpha.grounder.Substitution;
import at.ac.tuwien.kr.alpha.grounder.WorkingMemory;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationAtom;
import at.ac.tuwien.kr.alpha.grounder.atoms.EnumerationLiteral;

public class RuleInstantiatorTest {

	@Test
	public void instantiateSatisfiedFixedInterpretationLiteral() {
		ComparisonAtom equalsThree = new ComparisonAtom(ConstantTerm.getInstance(3), VariableTerm.getInstance("THREE"), ComparisonOperator.EQ);
		Literal lit = new ComparisonLiteral(equalsThree, true);
		Substitution substitution = new Substitution();
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy(null), false);
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, null);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<Substitution> resultSubstitutions = result.getSubstitutions();
		Assert.assertEquals(1, resultSubstitutions.size());
		Substitution extendedSubstitution = resultSubstitutions.get(0);
		Assert.assertTrue(extendedSubstitution.isVariableSet(VariableTerm.getInstance("THREE")));
		Assert.assertEquals(ConstantTerm.getInstance(3), extendedSubstitution.eval(VariableTerm.getInstance("THREE")));
	}

	@Test
	public void instantiateUnsatisfiedFixedInterpretationLiteral() {
		ComparisonAtom fiveEqualsThree = new ComparisonAtom(VariableTerm.getInstance("FIVE"), VariableTerm.getInstance("THREE"), ComparisonOperator.EQ);
		Literal lit = new ComparisonLiteral(fiveEqualsThree, true);
		Substitution substitution = new Substitution();
		substitution.put(VariableTerm.getInstance("FIVE"), ConstantTerm.getInstance(5));
		substitution.put(VariableTerm.getInstance("THREE"), ConstantTerm.getInstance(3));
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy());
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, null);
		Assert.assertEquals(LiteralInstantiationResult.Type.STOP_BINDING, result.getType());
	}

	@Test
	public void instantiateEnumLiteral() {
		VariableTerm enumTerm = VariableTerm.getInstance("E");
		VariableTerm idTerm = VariableTerm.getInstance("X");
		VariableTerm indexTerm = VariableTerm.getInstance("I");
		List<Term> termList = new ArrayList<>();
		termList.add(enumTerm);
		termList.add(idTerm);
		termList.add(indexTerm);
		EnumerationAtom enumAtom = new EnumerationAtom(termList);
		EnumerationLiteral lit = new EnumerationLiteral(enumAtom);
		Substitution substitution = new Substitution();
		substitution.put(enumTerm, ConstantTerm.getSymbolicInstance("enum1"));
		substitution.put(idTerm, ConstantTerm.getSymbolicInstance("someElement"));
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy());
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, null);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<Substitution> resultSubstitutions = result.getSubstitutions();
		Assert.assertEquals(1, resultSubstitutions.size());
		Assert.assertTrue(resultSubstitutions.get(0).isVariableSet(indexTerm));
	}

	@Test
	public void cautiousVerifyPositiveGroundLiteralSatisfied() {
		Predicate p = Predicate.getInstance("p", 2);
		VariableTerm x = VariableTerm.getInstance("X");
		VariableTerm y = VariableTerm.getInstance("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new Substitution();
		substitution.put(x, ConstantTerm.getSymbolicInstance("x"));
		substitution.put(y, ConstantTerm.getSymbolicInstance("y"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), ConstantTerm.getSymbolicInstance("y")), true);
		InstanceStorageView storageView = new BasicInstanceStorageView(workingMemory);
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy());
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, storageView);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<Substitution> substitutions = result.getSubstitutions();
		Assert.assertEquals(1, substitutions.size());
		Substitution resultSubstitution = substitutions.get(0);
		// with the given input substitution, lit is ground and satisfied,
		// we expect the instantiator to verify that
		Assert.assertEquals(substitution, resultSubstitution);
	}

	@Test
	public void cautiousVerifyPositiveGroundLiteralUnsatisfied() {
		Predicate p = Predicate.getInstance("p", 2);
		VariableTerm x = VariableTerm.getInstance("X");
		VariableTerm y = VariableTerm.getInstance("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new Substitution();
		substitution.put(x, ConstantTerm.getSymbolicInstance("x"));
		substitution.put(y, ConstantTerm.getSymbolicInstance("y"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		InstanceStorageView storageView = new BasicInstanceStorageView(workingMemory);
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy());
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, storageView);
		// with the given input substitution, lit is ground, but not satisfied,
		// we expect the instantiator to verify that and return an empty list of
		// substitutions
		Assert.assertEquals(LiteralInstantiationResult.Type.STOP_BINDING, result.getType());
	}

	@Test
	public void cautiousInstantiatePositiveBasicLiteral() {
		Predicate p = Predicate.getInstance("p", 2);
		VariableTerm x = VariableTerm.getInstance("X");
		VariableTerm y = VariableTerm.getInstance("Y");
		Literal lit = new BasicLiteral(new BasicAtom(p, x, y), true);
		Substitution substitution = new Substitution();
		substitution.put(x, ConstantTerm.getSymbolicInstance("x"));
		WorkingMemory workingMemory = new WorkingMemory();
		workingMemory.initialize(p);
		workingMemory.addInstance(new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), ConstantTerm.getSymbolicInstance("y")), true);
		workingMemory.addInstance(new BasicAtom(p, ConstantTerm.getSymbolicInstance("x"), ConstantTerm.getSymbolicInstance("z")), true);
		InstanceStorageView storageView = new BasicInstanceStorageView(workingMemory);
		RuleInstantiator instantiator = new RuleInstantiator(new CautiousInstantiationStrategy());
		LiteralInstantiationResult result = instantiator.instantiateLiteral(lit, substitution, storageView);
		Assert.assertEquals(LiteralInstantiationResult.Type.CONTINUE, result.getType());
		List<Substitution> substitutions = result.getSubstitutions();
		Assert.assertEquals(2, substitutions.size());
		boolean ySubstituted = false;
		boolean zSubstituted = false;
		for (Substitution resultSubstitution : substitutions) {
			Assert.assertTrue(resultSubstitution.isVariableSet(y));
			if (resultSubstitution.eval(y).equals(ConstantTerm.getSymbolicInstance("y"))) {
				ySubstituted = true;
			} else if (resultSubstitution.eval(y).equals(ConstantTerm.getSymbolicInstance("z"))) {
				zSubstituted = true;
			} else {
				Assert.fail("Invalid substitution for variable Y");
			}
		}
		Assert.assertTrue(ySubstituted && zSubstituted);
	}
}

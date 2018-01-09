/**
 * Copyright (c) 2018 Siemens AG
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.common.Assignment;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.common.atoms.HeuristicAtom;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.grounder.atoms.RuleAtom;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;
import at.ac.tuwien.kr.alpha.solver.ThriceTruth;
import at.ac.tuwien.kr.alpha.solver.learning.GroundConflictNoGoodLearner.ConflictAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

import static at.ac.tuwien.kr.alpha.common.Literals.atomOf;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.FALSE;
import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.TRUE;

/**
 * Analyses {@link HeuristicAtom}s to follow domain-specific heuristics specified within the input program.
 * 
 * Each rule can contain a negative {@code _h(W,L)} atom where {@code W} denotes the rule's weight and {@code L} denotes the rule's level.
 * Both values default to 1.
 * When asked for a choice, the domain-specific heuristics will choose to fire from the applicable rules with the highest weight one of those with the highest
 * level.
 * TODO: use domain-specific heuristics per default; when multiple rules are in line for choosing, use fallback heuristics (which is specified as usual)
 * TODO: use fallback heuristic also if no rules with heuristic value >1 can be found (because rules without heuristic atoms are currently not recorded by DomainHeuristic)
 */
public class DomainSpecific implements BranchingHeuristic {
	private static final Logger LOGGER = LoggerFactory.getLogger(DomainSpecific.class);
	static final int DEFAULT_CHOICE_ATOM = 0;

	private final Assignment assignment;
	private final ChoiceManager choiceManager;

	/**
	 * data structure: weight -> { level -> { rules } }
	 */
	private final SortedMap<Integer, SortedMap<Integer, Set<Integer>>> mapWeightLevelChoicePoint = new TreeMap<>(Comparator.reverseOrder());

	private final Set<Integer> knownChoicePoints = new HashSet<>();

	public Grounder getGrounder() {
		return grounder;
	}

	private final Grounder grounder;

	DomainSpecific(Assignment assignment, ChoiceManager choiceManager, Grounder grounder) {
		this.assignment = assignment;
		this.choiceManager = choiceManager;
		this.grounder = grounder;
	}

	@Override
	public void violatedNoGood(NoGood violatedNoGood) {
	}

	@Override
	public void analyzedConflict(ConflictAnalysisResult analysisResult) {
	}

	@Override
	public void newNoGood(NoGood newNoGood) {
		analyseNoGoodAndRecordChoicePoint(newNoGood);
	}

	@Override
	public void newNoGoods(Collection<NoGood> newNoGoods) {
		newNoGoods.forEach(this::newNoGood);
	}
	
	@Override
	public int chooseAtom() {
		// iterate through weights in decreasing order, iterate through levels in decreasing order, return first applicable choice point
		for (Entry<Integer, SortedMap<Integer, Set<Integer>>> entryMapLevelChoicePoint : mapWeightLevelChoicePoint.entrySet()) {
			for (Entry<Integer, Set<Integer>> entryChoicePoints : entryMapLevelChoicePoint.getValue().entrySet()) {
				for (Integer choicePoint : entryChoicePoints.getValue()) {
					if (choiceManager.isActiveChoiceAtom(choicePoint) && isUnassigned(choicePoint)) {
						return choicePoint;
						// TODO: use fallback heuristics to choose between equally good choice points
					}
				}
			}
		}
		return DEFAULT_CHOICE_ATOM;
	}
	
	@Override
	public boolean chooseSign(int atom) {
		// TODO: return true (except if falling back to other heuristic)
		return true;
	}

	protected void analyseNoGoodAndRecordChoicePoint(NoGood noGood) {
		for (Integer literal : noGood) {
			int atom = atomOf(literal);
			if (isChoicePoint(atom) && !knownChoicePoints.contains(atom)) {
				RuleAtom choicePoint = (RuleAtom) grounder.getAtomStore().get(atom);
				recordChoicePointAndHeuristicValues(atom, choicePoint);
			}
		}
	}

	private boolean isChoicePoint(int atom) {
		// we cannot use:
		// return choiceManager.isAtomChoice(atom);
		// because choiceManager does not know about choice points before the first choice is made
		// TODO improve!?
		return grounder.getAtomStore().get(atom) instanceof RuleAtom;
	}

	private void recordChoicePointAndHeuristicValues(int choicePointId, RuleAtom choicePointAtom) {
		HeuristicAtom heuristicAtom = choicePointAtom.getGroundHeuristic();
		recordChoicePointAndHeuristicValues(choicePointId, heuristicAtom.getWeight(), heuristicAtom.getLevel());
	}

	private void recordChoicePointAndHeuristicValues(int choicePoint, Integer weight, Integer level) {
		knownChoicePoints.add(choicePoint);

		if (!mapWeightLevelChoicePoint.containsKey(weight)) {
			mapWeightLevelChoicePoint.put(weight, new TreeMap<>());
		}

		Map<Integer, Set<Integer>> mapLevelChoicePoint = mapWeightLevelChoicePoint.get(weight);
		if (!mapLevelChoicePoint.containsKey(level)) {
			mapLevelChoicePoint.put(level, new HashSet<>());
		}

		mapLevelChoicePoint.get(level).add(choicePoint);
	}

	protected boolean isUnassigned(int atom) {
		ThriceTruth truth = assignment.getTruth(atom);
		return truth != FALSE && truth != TRUE; // do not use assignment.isAssigned(atom) because we may also choose MBTs
	}
}

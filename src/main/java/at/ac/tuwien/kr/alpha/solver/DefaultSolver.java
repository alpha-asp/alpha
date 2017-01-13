/**
 * Copyright (c) 2016-2017, the Alpha Team.
 * All rights reserved.
 *
 * Additional changes made by Siemens.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
package at.ac.tuwien.kr.alpha.solver;

import at.ac.tuwien.kr.alpha.common.AnswerSet;
import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.solver.heuristics.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

import static at.ac.tuwien.kr.alpha.solver.ThriceTruth.*;

/**
 * The new default solver employed in Alpha.
 * Copyright (c) 2016, the Alpha Team.
 */
public class DefaultSolver extends AbstractSolver {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSolver.class);

	private final NoGoodStore<ThriceTruth> store;
	private final ChoiceStack choiceStack;
	private final Assignment assignment;
	private final GroundConflictNoGoodLearner learner;
	private final BranchingHeuristic branchingHeuristic;
	private final BranchingHeuristic fallbackBranchingHeuristic;
	private final ChoiceManager choiceManager;

	private boolean initialize = true;

	private boolean didChange;

	private int decisionCounter;

	public DefaultSolver(Grounder grounder, Random random) {
		super(grounder);

		this.assignment = new BasicAssignment(grounder);
		this.store = new BasicNoGoodStore(assignment, grounder);
		this.choiceStack = new ChoiceStack(grounder);
		this.learner = new GroundConflictNoGoodLearner(assignment, store);
		this.branchingHeuristic = new BerkMin(assignment, this::isAtomChoicePoint, this::isAtomActiveChoicePoint, random);
		this.choiceManager = new ChoiceManager(assignment);
		this.fallbackBranchingHeuristic = new NaiveHeuristic(choiceManager);
	}

	@Override
	protected boolean tryAdvance(Consumer<? super AnswerSet> action) {
		// Initially, get NoGoods from grounder.
		if (initialize) {
			if (!obtainNoGoodsFromGrounder()) {
				// NoGoods are unsatisfiable.
				return false;
			}
			initialize = false;
		} else {
			// We already found one Answer-Set and are requested to find another one
			doBacktrack();
			if (isSearchSpaceExhausted()) {
				return false;
			}
		}

		int nextChoice;
		boolean afterAllAtomsAssigned = false;

		// Try all assignments until grounder reports no more NoGoods and all of them are satisfied
		while (true) {
			if (!propagationFixpointReached() && store.getViolatedNoGood() == null) {
				// Ask the grounder for new NoGoods, then propagate (again).
				LOGGER.trace("Doing propagation step.");
				updateGrounderAssignment();
				if (!obtainNoGoodsFromGrounder()) {
					// NoGoods are unsatisfiable.
					return false;
				}
				if (store.propagate()) {
					didChange = true;
				}
				LOGGER.debug("Assignment after propagation is: {}", assignment);
			} else if (store.getViolatedNoGood() != null) {
				NoGood violatedNoGood = store.getViolatedNoGood();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("NoGood violated ({}) by wrong choices ({} violated): {}", grounder.noGoodToString(violatedNoGood), choiceStack);
				}
				LOGGER.debug("Violating assignment is: {}", assignment);
				branchingHeuristic.violatedNoGood(violatedNoGood);

				if (!afterAllAtomsAssigned) {
					if (!learnBackjumpAddFromConflict()) {
						// NoGoods are unsatisfiable.
						return false;
					}
					didChange = true;
				} else {
					// Will not learn from violated NoGood, do simple backtrack.
					LOGGER.debug("NoGood was violated after all unassigned atoms were assigned to false; will not learn from it; skipping.");
					doBacktrack();
					afterAllAtomsAssigned = false;
					if (isSearchSpaceExhausted()) {
						return false;
					}
				}
			} else if ((nextChoice = computeChoice()) != 0) {
				LOGGER.debug("Doing choice.");
				doChoice(nextChoice);
				// Directly propagate after choice.
				if (store.propagate()) {
					didChange = true;
				}
			} else if (!allAtomsAssigned()) {
				LOGGER.debug("Closing unassigned known atoms (assigning FALSE).");
				assignUnassignedToFalse();
				afterAllAtomsAssigned = true;
				didChange = true;
			} else if (assignment.getMBTCount() == 0) {
				AnswerSet as = translate(assignment.getTrueAssignments());
				LOGGER.debug("Answer-Set found: {}", as);
				LOGGER.debug("Choices of Answer-Set were: {}", choiceStack);
				action.accept(as);
				return true;
			} else {
				LOGGER.debug("Backtracking from wrong choices ({} MBTs): {}", assignment.getMBTCount(), choiceStack);
				doBacktrack();
				afterAllAtomsAssigned = false;
				if (isSearchSpaceExhausted()) {
					return false;
				}
			}
		}
	}

	/**
	 * Analyzes the conflict and either learns a new NoGood (causing backjumping and addition to the NoGood store),
	 * or backtracks the guess causing the conflict.
	 * @return false iff the analysis result shows that the set of NoGoods is unsatisfiable.
	 */
	private boolean learnBackjumpAddFromConflict() {
		LOGGER.debug("Analyzing conflict.");
		GroundConflictNoGoodLearner.ConflictAnalysisResult analysisResult = learner.analyzeConflictingNoGood(store.getViolatedNoGood());
		if (analysisResult.isUnsatisfiable) {
			// Halt if unsatisfiable.
			return false;
		}

		branchingHeuristic.analyzedConflict(analysisResult);
		if (analysisResult.learnedNoGood == null) {
			LOGGER.debug("Conflict results from wrong guess, backjumping and removing guess.");
			LOGGER.debug("Backjumping to decision level: {}", analysisResult.backjumpLevel);
			doBackjump(analysisResult.backjumpLevel);
			store.backtrack();
			LOGGER.debug("Backtrack: Removing last choice because of conflict, setting decision level to {}.", assignment.getDecisionLevel());
			choiceStack.remove();
			choiceManager.backtrack();
			LOGGER.debug("Backtrack: choice stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);
			if (!store.propagate()) {
				throw new RuntimeException("Nothing to propagate after backtracking from conflict-causing guess. Should not happen.");
			}
		} else {
			NoGood learnedNoGood = analysisResult.learnedNoGood;
			LOGGER.debug("Learned NoGood is: {}", learnedNoGood);
			int backjumpingDecisionLevel = analysisResult.backjumpLevel;
			LOGGER.debug("Computed backjumping level: {}", backjumpingDecisionLevel);
			doBackjump(backjumpingDecisionLevel);

			int learnedNoGoodId = grounder.registerOutsideNoGood(learnedNoGood);
			NoGoodStore.ConflictCause conflictCause = store.add(learnedNoGoodId, learnedNoGood);
			if (conflictCause != null) {
				throw new RuntimeException("Learned NoGood is violated after backjumping, should not happen.");
			}
		}
		return true;
	}

	private void doBackjump(int backjumpingDecisionLevel) {
		LOGGER.debug("Backjumping to decisionLevel: {}.", backjumpingDecisionLevel);
		if (backjumpingDecisionLevel < 0) {
			throw new RuntimeException("Backjumping decision level less than 0, should not happen.");
		}
		// Remove everything above the backjumpingDecisionLevel, but keep the backjumpingDecisionLevel unchanged.
		while (assignment.getDecisionLevel() > backjumpingDecisionLevel) {
			store.backtrack();
			choiceStack.remove();
			choiceManager.backtrack();
		}
	}

	private void assignUnassignedToFalse() {
		for (Integer atom : unassignedAtoms) {
			assignment.assign(atom, FALSE, null);
		}
	}

	private List<Integer> unassignedAtoms;
	private boolean allAtomsAssigned() {
		unassignedAtoms = grounder.getUnassignedAtoms(assignment);
		return unassignedAtoms.isEmpty();
	}

	private void doBacktrack() {
		boolean repeatBacktrack;	// Iterative implementation of recursive backtracking.
		do {
			repeatBacktrack = false;
			if (isSearchSpaceExhausted()) {
				return;
			}

			int lastGuessedAtom = choiceStack.peekAtom();
			boolean lastGuessedValue = choiceStack.peekValue();
			Assignment.Entry lastChoiceEntry = assignment.get(lastGuessedAtom);

			store.backtrack();
			LOGGER.debug("Backtrack: Removing last choice, setting decision level to {}.", assignment.getDecisionLevel());

			boolean backtrackedAlready = choiceStack.peekBacktracked();
			choiceStack.remove();
			choiceManager.backtrack();
			LOGGER.debug("Backtrack: choice stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);

			if (!backtrackedAlready) {
				// Chronological backtracking: guess inverse now.
				// Guess FALSE if the previous guess was for TRUE and the atom was not already MBT at that time.
				if (lastGuessedValue && MBT.equals(assignment.getTruth(lastGuessedAtom))) {
					LOGGER.debug("Backtrack: inverting last guess not possible, atom was MBT before guessing TRUE.");
					LOGGER.debug("Recursive backtracking.");
					repeatBacktrack = true;
					continue;
				}
				// If choice was assigned at lower decision level (due to added NoGoods), no inverted guess should be done.
				if (lastChoiceEntry.getImpliedBy() != null) {
					LOGGER.debug("Last choice now is implied by: {}.", lastChoiceEntry.getImpliedBy());
					if (lastChoiceEntry.getDecisionLevel() == assignment.getDecisionLevel()) {
						throw new RuntimeException("Choice was assigned but not at a lower decision level. This should not happen.");
					}
					LOGGER.debug("Choice was assigned at a lower decision level");
					LOGGER.debug("Recursive backtracking.");
					repeatBacktrack = true;
					continue;
				}

				decisionCounter++;
				boolean newGuess = !lastGuessedValue;
				assignment.guess(lastGuessedAtom, newGuess);
				LOGGER.debug("Backtrack: setting decision level to {}.", assignment.getDecisionLevel());
				LOGGER.debug("Backtrack: inverting last guess. Now: {}={}@{}", grounder.atomToString(lastGuessedAtom), newGuess, assignment.getDecisionLevel());
				choiceStack.pushBacktrack(lastGuessedAtom, newGuess);
				choiceManager.nextDecisionLevel();
				didChange = true;
				LOGGER.debug("Backtrack: choice stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);
				LOGGER.debug("Backtrack: {} choices so far.", decisionCounter);
			} else {
				LOGGER.debug("Recursive backtracking.");
				repeatBacktrack = true;
			}
		} while (repeatBacktrack);
	}

	private void updateGrounderAssignment() {
		grounder.updateAssignment(assignment.getNewAssignmentsIterator());
	}

	/**
	 * Obtains new NoGoods from grounder and adds them to the NoGoodStore and the heuristics.
	 * @return false iff the set of NoGoods is detected to be unsatisfiable.
	 */
	private boolean obtainNoGoodsFromGrounder() {
		Map<Integer, NoGood> obtained = grounder.getNoGoods();
		LOGGER.debug("Obtained NoGoods from grounder: {}", obtained);

		if (!obtained.isEmpty()) {
			// Record to detect propagation fixpoint, checking if new NoGoods were reported would be better here.
			didChange = true;
		}

		if (!addAllNoGoodsAndTreatContradictions(obtained)) {
			return false;
		}
		branchingHeuristic.newNoGoods(obtained.values());

		// Record choice atoms.
		final Pair<Map<Integer, Integer>, Map<Integer, Integer>> choiceAtoms = grounder.getChoiceAtoms();
		choiceManager.addChoiceInformation(choiceAtoms);
		return true;
	}

	/**
	 * Adds all NoGoods in the given map to the NoGoodStore and treats eventual contradictions.
	 * If the set of NoGoods is unsatisfiable, this method returns false.
	 * @param obtained
	 * @return false iff the new set of NoGoods is detected to be unsatisfiable.
	 */
	private boolean addAllNoGoodsAndTreatContradictions(Map<Integer, NoGood> obtained) {
		LinkedList<Map.Entry<Integer, NoGood>> noGoodsToAdd = new LinkedList<>(obtained.entrySet());
		while (!noGoodsToAdd.isEmpty()) {
			Map.Entry<Integer, NoGood> noGoodEntry = noGoodsToAdd.poll();
			NoGoodStore.ConflictCause conflictCause = store.add(noGoodEntry.getKey(), noGoodEntry.getValue());
			if (conflictCause == null) {
				// There is no conflict, all is fine. Just skip conflict treatment and carry on.
				continue;
			}

			LOGGER.debug("Adding obtained NoGoods from grounder violates current assignment: learning, backjumping, and adding again.");
			if (conflictCause.violatedGuess != null) {
				LOGGER.debug("Added NoGood {} violates guess {}.", noGoodEntry.getKey(), conflictCause.violatedGuess);
				LOGGER.debug("Backjumping to decision level: {}", conflictCause.violatedGuess.getDecisionLevel());
				doBackjump(conflictCause.violatedGuess.getDecisionLevel());
				store.backtrack();
				LOGGER.debug("Backtrack: Removing last choice because of conflict with newly added NoGoods, setting decision level to {}.", assignment.getDecisionLevel());
				choiceStack.remove();
				choiceManager.backtrack();
				LOGGER.debug("Backtrack: choice stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);
			} else {
				LOGGER.debug("Violated NoGood is {}. Analyzing the conflict.", conflictCause.violatedNoGood);
				GroundConflictNoGoodLearner.ConflictAnalysisResult conflictAnalysisResult = null;
				conflictAnalysisResult = learner.analyzeConflictingNoGood(conflictCause.violatedNoGood);
				if (conflictAnalysisResult.isUnsatisfiable) {
					// Halt if unsatisfiable.
					return false;
				}
				LOGGER.debug("Backjumping to decision level: {}", conflictAnalysisResult.backjumpLevel);
				doBackjump(conflictAnalysisResult.backjumpLevel);
				if (conflictAnalysisResult.clearLastGuessAfterBackjump) {
					store.backtrack();
					LOGGER.debug("Backtrack: Removing last choice because of conflict with newly added NoGoods, setting decision level to {}.", assignment.getDecisionLevel());
					choiceStack.remove();
					choiceManager.backtrack();
					LOGGER.debug("Backtrack: choice stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);
				}
				// If NoGood was learned, add it to the store.
				// Note that the learned NoGood may cause further conflicts, since propagation on lower decision levels is lazy, hence backtracking once might not be enough to remove the real conflict cause.
				if (conflictAnalysisResult.learnedNoGood != null) {
					noGoodsToAdd.addFirst(new AbstractMap.SimpleEntry<>(grounder.registerOutsideNoGood(conflictAnalysisResult.learnedNoGood), conflictAnalysisResult.learnedNoGood));
				}
			}
			if (store.add(noGoodEntry.getKey(), noGoodEntry.getValue()) != null) {
				throw new RuntimeException("Re-adding of former conflicting NoGood still causes conflicts. This should not happen.");
			}
		}
		return true;
	}

	private boolean isSearchSpaceExhausted() {
		return assignment.getDecisionLevel() == 0;
	}

	private boolean propagationFixpointReached() {
		// Check if anything changed: didChange is updated in places of change.
		boolean changeCopy = didChange;
		didChange = false;
		return !changeCopy;
	}

	private boolean isAtomChoicePoint(int atom) {
		return choiceManager.isAtomChoice(atom);
	}

	private boolean isAtomActiveChoicePoint(int atom) {
		return choiceManager.isActiveChoiceAtom(atom);
	}

	private void doChoice(int nextChoice) {
		decisionCounter++;
		boolean sign = branchingHeuristic.chooseSign(nextChoice);
		assignment.guess(nextChoice, sign);
		choiceStack.push(nextChoice, sign);
		choiceManager.nextDecisionLevel();
		// Record change to compute propagation fixpoint again.
		didChange = true;
		LOGGER.debug("Choice: guessing {}={}@{}", grounder.atomToString(nextChoice), sign, assignment.getDecisionLevel());
		LOGGER.debug("Choice: stack size: {}, choice stack: {}", choiceStack.size(), choiceStack);
		LOGGER.debug("Choice: {} choices so far.", decisionCounter);
	}

	private int computeChoice() {
		// Update ChoiceManager.
		Iterator<Assignment.Entry> it = assignment.getNewAssignmentsIterator2();
		while (it.hasNext()) {
			choiceManager.updateAssignment(it.next().getAtom());
		}
		// Run Heuristics.
		int berkminChoice = branchingHeuristic.chooseAtom();
		if (berkminChoice != BerkMin.DEFAULT_CHOICE_ATOM) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Atom chosen by BerkMin: {}", grounder.atomToString(berkminChoice));
			}
			return berkminChoice;
		}

		//TODO: remove fallback as soon as we are sure that BerkMin will always choose an atom
		LOGGER.debug("Falling back to NaiveHeuristics.");
		return fallbackBranchingHeuristic.chooseAtom();
	}
}

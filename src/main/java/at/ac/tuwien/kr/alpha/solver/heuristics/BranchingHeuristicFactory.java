/**
 * Copyright (c) 2017 Siemens AG
 * All rights reserved.
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
package at.ac.tuwien.kr.alpha.solver.heuristics;

import at.ac.tuwien.kr.alpha.grounder.Grounder;
import at.ac.tuwien.kr.alpha.solver.Assignment;
import at.ac.tuwien.kr.alpha.solver.ChoiceManager;

import java.util.Random;

public final class BranchingHeuristicFactory {

	public static final String NAIVE = "naive";
	public static final String BERKMIN = "berkmin";
	public static final String BERKMINLITERAL = "berkminliteral";
	public static final String ALPHA = "alpha";
	public static final String ALPHA_RANDOM_SIGN = "alpha-rs";
	public static final String ALPHA_ACTIVE_RULE = "alpha-ar";

	public static BranchingHeuristic getInstance(String name, Grounder grounder, Assignment assignment, ChoiceManager choiceManager, Random random) {
		switch (name.toLowerCase()) {
		case NAIVE:
			return new NaiveHeuristic(choiceManager);
		case BERKMIN:
			return new BerkMin(assignment, choiceManager, random);
		case BERKMINLITERAL:
			return new BerkMinLiteral(assignment, choiceManager, random);
		case ALPHA:
			return new AlphaHeuristic(assignment, choiceManager, random);
		case ALPHA_RANDOM_SIGN:
			return new AlphaRandomSignHeuristic(assignment, choiceManager, random);
		case ALPHA_ACTIVE_RULE:
			return new AlphaActiveRuleHeuristic(assignment, choiceManager, random);
		}
		throw new IllegalArgumentException("Unknown branching heuristic requested.");
	}
}

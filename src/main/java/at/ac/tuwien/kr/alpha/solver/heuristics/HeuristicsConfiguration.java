/**
 * Copyright (c) 2018-2019 Siemens AG
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

import at.ac.tuwien.kr.alpha.solver.BinaryNoGoodPropagationEstimation.Strategy;
import at.ac.tuwien.kr.alpha.solver.heuristics.BranchingHeuristicFactory.Heuristic;

/**
 * Configuration class holding parameters for {@link BranchingHeuristic}s.
 */
public class HeuristicsConfiguration {

	private Heuristic heuristic;
	private boolean respectDomspecHeuristics;
	private Strategy momsStrategy;

	/**
	 * @param heuristic
	 * @param respectDomspecHeuristics
	 * @param momsStrategy
	 */
	public HeuristicsConfiguration(Heuristic heuristic, boolean respectDomspecHeuristics, Strategy momsStrategy) {
		super();
		this.heuristic = heuristic;
		this.respectDomspecHeuristics = respectDomspecHeuristics;
		this.momsStrategy = momsStrategy;
	}

	public Heuristic getHeuristic() {
		return heuristic;
	}

	/**
	 * @param heuristic the heuristic to set
	 */
	public void setHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic;
	}

	public boolean isRespectDomspecHeuristics() {
		return respectDomspecHeuristics;
	}

	public void setRespectDomspecHeuristics(boolean respectDomspecHeuristics) {
		this.respectDomspecHeuristics = respectDomspecHeuristics;
	}

	/**
	 * @return the momsStrategy
	 */
	public Strategy getMomsStrategy() {
		return momsStrategy;
	}

	/**
	 * @param momsStrategy the momsStrategy to set
	 */
	public void setMomsStrategy(Strategy momsStrategy) {
		this.momsStrategy = momsStrategy;
	}

	public static HeuristicsConfigurationBuilder builder() {
		return new HeuristicsConfigurationBuilder();
	}

}

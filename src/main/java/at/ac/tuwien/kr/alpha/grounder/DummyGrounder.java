package at.ac.tuwien.kr.alpha.grounder;

import at.ac.tuwien.kr.alpha.common.NoGood;
import at.ac.tuwien.kr.alpha.grounder.parser.ParsedProgram;

/**
 * Copyright (c) 2016, the Alpha Team.
 */
public class DummyGrounder extends AbstractGrounder {


	@Override
	public void initialize(ParsedProgram program) {

	}

	@Override
	public NoGood[] getMoreNoGoods() {
		return null;
	}

	@Override
	public void updateAssignments(int[] atomIds, boolean[] truthValues) {

	}

	@Override
	public void forgetAssignments(int[] atomIds) {

	}

	@Override
	public void printAnswerSet(int[] trueAtomIds) {

	}
}

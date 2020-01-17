/*
 *  Copyright (c) 2020 Siemens AG
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1) Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *
 *  2) Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package at.ac.tuwien.kr.alpha.common.heuristics;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static at.ac.tuwien.kr.alpha.Util.join;

public class HeuristicDirectiveBody {

	private final List<HeuristicDirectiveAtom> bodyAtomsPositive;
	private final List<HeuristicDirectiveAtom> bodyAtomsNegative;

	public HeuristicDirectiveBody(List<HeuristicDirectiveAtom> bodyAtomsPositive, List<HeuristicDirectiveAtom> bodyAtomsNegative) {
		this.bodyAtomsPositive = Collections.unmodifiableList(bodyAtomsPositive);
		this.bodyAtomsNegative = Collections.unmodifiableList(bodyAtomsNegative);
	}

	public HeuristicDirectiveBody(List<HeuristicDirectiveLiteral> bodyLiterals) {
		this(
				bodyLiterals.stream().filter(l -> !l.isNegated()).map(HeuristicDirectiveLiteral::getAtom).collect(Collectors.toList()),
				bodyLiterals.stream().filter(l -> l.isNegated()).map(HeuristicDirectiveLiteral::getAtom).collect(Collectors.toList())
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HeuristicDirectiveBody that = (HeuristicDirectiveBody) o;
		return bodyAtomsPositive.equals(that.bodyAtomsPositive) &&
				bodyAtomsNegative.equals(that.bodyAtomsNegative);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodyAtomsPositive, bodyAtomsNegative);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(join("", bodyAtomsPositive, ""));
		if (!bodyAtomsPositive.isEmpty() && !bodyAtomsNegative.isEmpty()) {
			sb.append(", ");
		}
		sb.append(join("", bodyAtomsNegative, ""));
		return sb.toString();
	}
}
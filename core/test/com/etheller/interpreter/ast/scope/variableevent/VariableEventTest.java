package com.etheller.interpreter.ast.scope.variableevent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VariableEventTest {
	private static final double BASE_VALUE = 100.0;
	private static final double EPSILON = 1e-9;

	@Test
	void equalAndNotEqualUseEpsilonComparison() {
		final VariableEvent equal = new VariableEvent(null, CLimitOp.EQUAL, BASE_VALUE);
		final VariableEvent notEqual = new VariableEvent(null, CLimitOp.NOT_EQUAL, BASE_VALUE);

		assertTrue(equal.isMatching(BASE_VALUE + (EPSILON / 2)));
		assertFalse(notEqual.isMatching(BASE_VALUE + (EPSILON / 2)));
	}

	@Test
	void strictComparisonsDoNotTriggerInsideEpsilonWindow() {
		final VariableEvent greaterThan = new VariableEvent(null, CLimitOp.GREATER_THAN, BASE_VALUE);
		final VariableEvent lessThan = new VariableEvent(null, CLimitOp.LESS_THAN, BASE_VALUE);

		assertFalse(greaterThan.isMatching(BASE_VALUE + (EPSILON / 2)));
		assertFalse(lessThan.isMatching(BASE_VALUE - (EPSILON / 2)));
	}

	@Test
	void nonStrictComparisonsStillTriggerInsideEpsilonWindow() {
		final VariableEvent greaterThanOrEqual = new VariableEvent(null, CLimitOp.GREATER_THAN_OR_EQUAL, BASE_VALUE);
		final VariableEvent lessThanOrEqual = new VariableEvent(null, CLimitOp.LESS_THAN_OR_EQUAL, BASE_VALUE);

		assertTrue(greaterThanOrEqual.isMatching(BASE_VALUE + (EPSILON / 2)));
		assertTrue(lessThanOrEqual.isMatching(BASE_VALUE - (EPSILON / 2)));
	}
}

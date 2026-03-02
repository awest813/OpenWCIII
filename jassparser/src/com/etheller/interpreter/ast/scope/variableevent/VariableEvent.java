package com.etheller.interpreter.ast.scope.variableevent;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;

public class VariableEvent {
	private static final double REAL_COMPARISON_EPSILON = 1e-9;

	private final Trigger trigger;
	private final CLimitOp limitOp;
	private final double doubleValue;

	public VariableEvent(final Trigger trigger, final CLimitOp limitOp, final double doubleValue) {
		this.trigger = trigger;
		this.limitOp = limitOp;
		this.doubleValue = doubleValue;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public CLimitOp getLimitOp() {
		return this.limitOp;
	}

	public double getDoubleValue() {
		return this.doubleValue;
	}

	public boolean isMatching(final double realValue) {
		final double difference = Math.abs(realValue - this.doubleValue);
		final boolean approximatelyEqual = difference <= REAL_COMPARISON_EPSILON;
		switch (this.limitOp) {
		case EQUAL:
			return approximatelyEqual;
		case GREATER_THAN:
			return (realValue > this.doubleValue) && !approximatelyEqual;
		case GREATER_THAN_OR_EQUAL:
			return (realValue > this.doubleValue) || approximatelyEqual;
		case LESS_THAN:
			return (realValue < this.doubleValue) && !approximatelyEqual;
		case LESS_THAN_OR_EQUAL:
			return (realValue < this.doubleValue) || approximatelyEqual;
		case NOT_EQUAL:
			return !approximatelyEqual;
		}
		throw new IllegalStateException();
	}

	public void fire(final GlobalScope globalScope) {
		final TriggerExecutionScope triggerScope = new TriggerExecutionScope(this.trigger);
		globalScope.queueTrigger(null, null, this.trigger, triggerScope, triggerScope);
	}
}

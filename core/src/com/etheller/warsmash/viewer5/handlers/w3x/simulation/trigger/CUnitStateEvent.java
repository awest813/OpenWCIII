package com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.interpreter.ast.scope.variableevent.VariableEvent;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;

public final class CUnitStateEvent extends VariableEvent {
	private final GlobalScope globals;
	private final CUnit unit;
	private final CUnitState state;
	private boolean wasMatching;
	private boolean removed;

	public CUnitStateEvent(final GlobalScope globals, final Trigger trigger, final CUnit unit, final CUnitState state,
			final CLimitOp limit, final double threshold) {
		super(trigger, limit, threshold);
		this.globals = globals;
		this.unit = unit;
		this.state = state;
		this.wasMatching = isMatching(unit.getUnitState(state));
	}

	public void update() {
		if (this.removed) return;
		final boolean matching = isMatching(this.unit.getUnitState(this.state));
		final boolean crossed = matching && !this.wasMatching;
		this.wasMatching = matching;
		if (crossed) {
			final var scope = CommonTriggerExecutionScope.unitStateScope(getTrigger(), this.unit, this.state);
			this.globals.queueTrigger(null, null, getTrigger(), scope, scope);
		}
	}

	public void remove() {
		this.removed = true;
	}
}

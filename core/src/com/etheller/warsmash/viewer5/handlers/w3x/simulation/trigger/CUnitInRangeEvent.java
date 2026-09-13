package com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;

/**
 * A TriggerRegisterUnitInRange registration: watches a circle around one unit
 * and fires when another unit enters it.
 *
 * <p>
 * Retail fires once per unit entering the circle, not repeatedly while it sits
 * inside, and fires again only after that unit has left and come back. The
 * triggering unit is the one that entered, which is what the campaign scripts
 * compare against. The watched unit itself is never a triggering unit.
 */
public class CUnitInRangeEvent implements CUnitEnumFunction {
	private final GlobalScope globalScope;
	private final Trigger trigger;
	private final TriggerBooleanExpression filter;
	private final CUnit whichUnit;
	private final float range;
	private final Set<CUnit> unitsInRange = new HashSet<>();
	private final Set<CUnit> unitsInRangeThisTick = new HashSet<>();

	public CUnitInRangeEvent(final GlobalScope globalScope, final Trigger trigger, final CUnit whichUnit,
			final float range, final TriggerBooleanExpression filter) {
		this.globalScope = globalScope;
		this.trigger = trigger;
		this.whichUnit = whichUnit;
		this.range = range;
		this.filter = filter;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public void update(final CSimulation simulation) {
		if ((this.whichUnit == null) || this.whichUnit.isDead()) {
			this.unitsInRange.clear();
			return;
		}
		this.unitsInRangeThisTick.clear();
		simulation.getWorldCollision().enumUnitsInRange(this.whichUnit.getX(), this.whichUnit.getY(), this.range, this);
		// Anything that left the circle can fire again next time it comes back.
		final Iterator<CUnit> alreadyInRange = this.unitsInRange.iterator();
		while (alreadyInRange.hasNext()) {
			if (!this.unitsInRangeThisTick.contains(alreadyInRange.next())) {
				alreadyInRange.remove();
			}
		}
	}

	@Override
	public boolean call(final CUnit enumUnit) {
		if ((enumUnit == this.whichUnit) || enumUnit.isDead()) {
			return false;
		}
		this.unitsInRangeThisTick.add(enumUnit);
		if (this.unitsInRange.add(enumUnit)) {
			fire(enumUnit);
		}
		return false;
	}

	private void fire(final CUnit enteringUnit) {
		// common.j has no event constant for this registration, so there is no id
		// for GetTriggerEventId to report.
		final CommonTriggerExecutionScope eventScope = CommonTriggerExecutionScope.unitTriggerScope(null, this.trigger,
				enteringUnit);
		this.globalScope.queueTrigger(this.filter,
				CommonTriggerExecutionScope.filterScope(TriggerExecutionScope.EMPTY, enteringUnit), this.trigger,
				eventScope, eventScope);
	}
}

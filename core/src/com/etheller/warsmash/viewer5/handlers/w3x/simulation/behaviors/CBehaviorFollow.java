package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors;

import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetStillAliveVisitor;

public class CBehaviorFollow extends CAbstractRangedBehavior {

	private int highlightOrderId;
	private boolean justAutoAttacked = false;
	private boolean initializing;

	public CBehaviorFollow(final CUnit unit) {
		super(unit);
	}

	public CBehavior reset(CSimulation game, final int highlightOrderId, final CWidget target) {
		this.highlightOrderId = highlightOrderId;
		this.justAutoAttacked = false;
		this.initializing = true;
		try {
			return innerReset(game, target);
		}
		finally {
			this.initializing = false;
		}
	}

	@Override
	public int getHighlightOrderId() {
		return this.highlightOrderId;
	}

	@Override
	public boolean isWithinRange(final CSimulation simulation) {
		if (this.justAutoAttacked || (!this.initializing
				&& (this.justAutoAttacked = this.unit.autoAcquireTargets(simulation, false)))) {
			return true;
		}
		return this.unit.canReach(this.target, this.unit.getAcquisitionRange());
	}

	@Override
	protected CBehavior update(final CSimulation simulation, final boolean withinFacingWindow) {

		if (this.justAutoAttacked) {
			this.justAutoAttacked = false;
			return this.unit.getCurrentBehavior();
		}
		this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1.0f, false);
		return this;
	}

	@Override
	protected CBehavior updateOnInvalidTarget(final CSimulation simulation) {
		this.unit.setDefaultBehavior(this.unit.getStopBehavior());
		return this.unit.pollNextOrderBehavior(simulation);
	}

	@Override
	protected boolean checkTargetStillValid(final CSimulation simulation) {
		return this.target.visit(AbilityTargetStillAliveVisitor.INSTANCE);
	}

	@Override
	protected void resetBeforeMoving(final CSimulation simulation) {
		// Combat and spells reuse the same move behavior; restore the followed target.
		this.unit.getMoveBehavior().reset(this.target, this, false);
	}

	@Override
	public void begin(final CSimulation game) {

	}

	@Override
	public void end(final CSimulation game, final boolean interrupted) {

	}

	@Override
	public void endMove(final CSimulation game, final boolean interrupted) {

	}

	@Override
	public boolean interruptable() {
		return true;
	}

	@Override
	public CBehaviorCategory getBehaviorCategory() {
		return CBehaviorCategory.MOVEMENT;
	}

}

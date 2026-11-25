package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractGenericSingleIconNoSmartActiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilitySpell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityShadowMeld extends AbstractGenericSingleIconNoSmartActiveAbility implements CAbilitySpell {
	private final StateModBuff invisibility = new StateModBuff(StateModBuffType.INVISIBLE, 1);
	private HideBehavior hideBehavior;
	private int fadeTicks;
	private int dayNightTicks;
	private int actionTicks;
	private int readyTick;
	private boolean day;
	private boolean concealed;
	private boolean removed;

	public CAbilityShadowMeld(final int handleId, final War3ID code, final War3ID alias) {
		super(handleId, code, alias);
	}

	@Override
	public void populate(final GameObject data, final int level) {
		this.fadeTicks = ticks(data.getFieldAsFloat(AbilityFields.DATA_A + level, 0));
		this.dayNightTicks = ticks(data.getFieldAsFloat(AbilityFields.DATA_B + level, 0));
		this.actionTicks = ticks(data.getFieldAsFloat(AbilityFields.DATA_C + level, 0));
	}

	private static int ticks(final float duration) {
		return (int) Math.ceil(Math.max(0, duration) / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.removed = false;
		this.hideBehavior = new HideBehavior(unit);
		this.day = game.isDay();
		this.readyTick = game.getGameTurnTick() + this.fadeTicks;
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		this.removed = true;
		setConcealed(game, unit, false);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		setConcealed(game, unit, false);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		final int tick = game.getGameTurnTick();
		if (this.day != game.isDay()) {
			this.day = game.isDay();
			this.readyTick = tick + this.dayNightTicks;
			unit.notifyOrdersChanged();
		}
		final CBehavior behavior = unit.getCurrentBehavior();
		final boolean idle = behavior == null || behavior.getBehaviorCategory() == CBehaviorCategory.IDLE;
		if (!idle || isDisabled()) {
			this.readyTick = Math.max(this.readyTick, tick + this.fadeTicks + this.actionTicks);
		}
		setConcealed(game, unit, !this.day && !isDisabled() && idle && tick >= this.readyTick);
	}

	private void setConcealed(final CSimulation game, final CUnit unit, final boolean concealed) {
		if (this.concealed != concealed) {
			this.concealed = concealed;
			if (concealed) {
				unit.addStateModBuff(this.invisibility);
			}
			else {
				unit.removeStateModBuff(this.invisibility);
			}
			unit.computeUnitState(game, StateModBuffType.INVISIBLE);
		}
	}

	@Override
	protected void innerCheckCanUse(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (game.isDay()) {
			receiver.disabled();
		}
		else {
			receiver.useOk();
		}
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.targetOk(null);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		return this.hideBehavior;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final CWidget target) {
		return null;
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final AbilityPointTarget point) {
		return null;
	}

	@Override
	public void onCancelFromQueue(final CSimulation game, final CUnit unit, final int orderId) {
	}

	@Override
	public boolean isToggleOn() {
		return false;
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.ambush;
	}

	@Override
	public boolean isPhysical() {
		return true;
	}

	@Override
	public boolean isUniversal() {
		return false;
	}

	@Override
	public CAbilityCategory getAbilityCategory() {
		return CAbilityCategory.CORE;
	}

	private final class HideBehavior implements CBehavior {
		private final CUnit unit;

		private HideBehavior(final CUnit unit) {
			this.unit = unit;
		}

		@Override
		public CBehavior update(final CSimulation game) {
			if (game.isDay() || CAbilityShadowMeld.this.removed || isDisabled() || !this.unit.getOrderQueue().isEmpty()) {
				return this.unit.pollNextOrderBehavior(game);
			}
			return this;
		}

		@Override
		public void begin(final CSimulation game) {
			this.unit.getUnitAnimationListener().playAnimation(false, PrimaryTag.STAND, SequenceUtils.EMPTY, 1, true);
		}

		@Override
		public void end(final CSimulation game, final boolean interrupted) {
		}

		@Override
		public int getHighlightOrderId() {
			return OrderIds.ambush;
		}

		@Override
		public boolean interruptable() {
			return true;
		}

		@Override
		public CBehaviorCategory getBehaviorCategory() {
			return CBehaviorCategory.IDLE;
		}

		@Override
		public <T> T visit(final CBehaviorVisitor<T> visitor) {
			return visitor.accept(this);
		}
	}
}

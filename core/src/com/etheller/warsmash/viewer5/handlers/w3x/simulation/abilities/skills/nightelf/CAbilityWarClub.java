package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.EnumSet;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityWarClub extends CAbilityTargetSpellBase implements CUnitAttackPostDamageListener {
	public static final War3ID CODE = War3ID.fromString("Agra");
	private int ripTicks;
	private int animationTicks;
	private int ripAt;
	private int finishAt;
	private boolean treeTaken;
	private int strikes;
	private int remainingStrikes;

	public CAbilityWarClub(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.grabtree;
	}

	@Override
	public boolean isPhysical() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.ripTicks = Math.round(definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0) / WarsmashConstants.SIMULATION_STEP_TIME);
		this.animationTicks = Math.round(definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0) / WarsmashConstants.SIMULATION_STEP_TIME);
		this.strikes = definition.getFieldAsInteger(AbilityFields.DATA_E + level, 0);
		setTargetsAllowed(EnumSet.of(CTargetType.TREE));
		setCastingSecondaryTags(SequenceUtils.SPELL_EATTREE);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		final float closeEnough = game.getGameplayConstants().getCloseEnoughRange();
		if (getCastRange() < closeEnough) setCastRange(getCastRange() + closeEnough);
		super.onAdd(game, unit);
		unit.addPostDamageListener(this);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		clearClub(unit);
		unit.removePostDamageListener(this);
		super.onRemove(game, unit);
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		clearClub(unit);
		super.onDeath(game, unit);
	}

	@Override
	public void onSetUnitType(final CSimulation game, final CUnit unit) {
		clearClub(unit);
	}

	public boolean hasClub() {
		return this.remainingStrikes > 0;
	}

	@Override
	public void checkCanTarget(final CSimulation game, final CUnit unit, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		super.checkCanTarget(game, unit, orderId == OrderIds.smart && !hasClub() ? getBaseOrderId() : orderId, target, receiver);
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CDestructable tree && tree.getDestType().getTargetedAs().contains(CTargetType.TREE)) {
			receiver.targetOk(tree);
		}
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_TREE);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit unit, final AbilityTarget target) {
		this.ripAt = game.getGameTurnTick() + this.ripTicks;
		this.finishAt = game.getGameTurnTick() + Math.max(this.ripTicks, this.animationTicks);
		this.treeTaken = false;
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit unit, final AbilityTarget target) {
		final int tick = game.getGameTurnTick();
		if (!this.treeTaken && tick >= this.ripAt) {
			this.treeTaken = true;
			final var siege = unit.getUnitSpecificAttacks().stream()
					.filter(attack -> attack.getAttackType() == CAttackType.SIEGE).findFirst().orElse(null);
			if (!(target instanceof CDestructable tree) || tree.isDead() || tree.isInvulnerable() || siege == null || this.strikes <= 0) return false;
			tree.setLife(game, 0);
			this.remainingStrikes = this.strikes;
			unit.setUnitSpecificCurrentAttacks(List.of(siege));
			unit.notifyAttacksChanged();
			if (unit.getUnitAnimationListener().addSecondaryTag(SecondaryTag.ALTERNATE)) {
				unit.getUnitAnimationListener().forceResetCurrentAnimation();
			}
		}
		return tick < this.finishAt;
	}

	@Override
	public void onHit(final CSimulation game, final CUnit unit, final AbilityTarget target, final float damage) {
		if (this.remainingStrikes == 1) clearClub(unit);
		else if (hasClub()) this.remainingStrikes--;
	}

	private void clearClub(final CUnit unit) {
		if (!hasClub()) return;
		this.remainingStrikes = 0;
		unit.setUnitSpecificCurrentAttacks(CUnitData.getEnabledAttacks(unit.getUnitSpecificAttacks(), unit.getUnitType().getAttacksEnabled()));
		unit.notifyAttacksChanged();
		if (unit.getUnitAnimationListener().removeSecondaryTag(SecondaryTag.ALTERNATE)) {
			unit.getUnitAnimationListener().forceResetCurrentAnimation();
		}
	}
}

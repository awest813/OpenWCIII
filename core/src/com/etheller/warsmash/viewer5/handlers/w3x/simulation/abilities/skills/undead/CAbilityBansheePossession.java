package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral.darkranger.CAbilityCharm;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityBansheePossession extends CAbilityTargetSpellBase {
	private static final int CHANNEL_TICKS = Math.round(4 / WarsmashConstants.SIMULATION_STEP_TIME);
	private static final CUnitAttackDamageTakenModificationListener TAKE_EXTRA_ATTACK_DAMAGE =
			(game, attacker, target, isAttack, isRanged, attackType, damageType, damage) -> {
				if (isAttack) damage.addDamageMultiplier(1.66f);
				return damage;
			};
	private final StateModBuff stun = new StateModBuff(StateModBuffType.STUN, 1);
	private List<CUnitTypeRequirement> requirements;
	private int maximumCreepLevel;
	private int completeTick;
	private CUnit victim;

	public CAbilityBansheePossession(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.possession; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.maximumCreepLevel = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.requirements = definition.getFieldAsBoolean(AbilityFields.CHECK_DEPENDENCIES, 0)
				? CUnitData.parseRequirements(definition.getFieldAsList(AbilityFields.REQUIREMENTS),
						definition.getFieldAsList(AbilityFields.REQUIREMENT_LEVELS))
				: List.of();
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit banshee) {
		final var player = game.getPlayer(banshee.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit banshee, final AbilityActivationReceiver receiver) {
		final var player = game.getPlayer(banshee.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit banshee, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit unit)) receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
		else if (unit.getPlayerIndex() >= WarsmashConstants.MAX_PLAYERS - 4 && unit.getUnitType().getLevel() > this.maximumCreepLevel) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
		}
		else receiver.targetOk(target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit banshee, final AbilityTarget target) {
		if (!(target instanceof CUnit unit) || !unit.checkForAbilityEffectReaction(game, banshee, this)) return false;
		this.victim = unit;
		this.completeTick = game.getGameTurnTick() + CHANNEL_TICKS;
		unit.addStateModBuff(this.stun);
		unit.computeUnitState(game, this.stun.getBuffType());
		banshee.addDamageTakenModificationListener(TAKE_EXTRA_ATTACK_DAMAGE);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit banshee, final AbilityTarget target) {
		final CUnit unit = this.victim;
		if (unit == null || unit.isDead() || unit.isHidden() || !unit.canBeTargetedBy(game, banshee, false,
				getTargetsAllowed(), BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return false;
		if (game.getGameTurnTick() < this.completeTick) return true;
		clearChannel(game, banshee);
		game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		CAbilityCharm.transferUnit(game, banshee, unit);
		game.removeUnit(banshee);
		return false;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit banshee, final AbilityTarget target, final boolean interrupted) {
		clearChannel(game, banshee);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit banshee) {
		clearChannel(game, banshee);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit banshee) {
		if (this.victim != null && (banshee.getCurrentBehavior() == null
				|| banshee.getCurrentBehavior().getHighlightOrderId() != getBaseOrderId())) clearChannel(game, banshee);
	}

	private void clearChannel(final CSimulation game, final CUnit banshee) {
		if (this.victim == null) return;
		banshee.removeDamageTakenModificationListener(TAKE_EXTRA_ATTACK_DAMAGE);
		this.victim.removeStateModBuff(this.stun);
		this.victim.computeUnitState(game, this.stun.getBuffType());
		this.victim = null;
	}
}

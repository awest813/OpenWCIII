package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.CAbilityItemOrb;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/** Poison delivered by a successful weapon attack. */
public final class CAbilityAttackPoison extends CAbilityPassiveSpellBase {
	private static final War3ID VENOM_POISON = War3ID.fromString("Apo2");
	private float damagePerSecond;
	private boolean equippedVenom;
	private float movementSlow;
	private float attackSlow;
	private List<CUnitTypeRequirement> requirements;
	private final CUnitAttackPostDamageListener onAttackHit = this::onAttackHit;

	public CAbilityAttackPoison(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.damagePerSecond = ability.getFieldAsFloat("DataA" + level, 0);
		this.movementSlow = ability.getFieldAsFloat("DataB" + level, 0);
		this.attackSlow = ability.getFieldAsFloat("DataC" + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(ability.getFieldAsList("targs")));
		}
		this.requirements = CUnitData.parseRequirements(ability.getFieldAsList("Requires"),
				ability.getFieldAsList("Requiresamount"));
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		for (final CUnitTypeRequirement requirement : this.requirements) {
			if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) return false;
		}
		return true;
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.equippedVenom = item != null && VENOM_POISON.equals(getCode());
		if (this.equippedVenom) setPermanent(true);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addPostDamageListener(this.onAttackHit);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removePostDamageListener(this.onAttackHit);
	}

	private void onAttackHit(final CSimulation game, final CUnit attacker, final AbilityTarget hit, final float damage) {
		if (damage <= 0 || isDisabled() || !isRequirementsMet(game, attacker)) return;
		if (this.equippedVenom) {
			final CAbilityItemOrb preferred = CAbilityItemOrb.preferredOrb(attacker);
			if (preferred != null && !CAbilityItemOrb.VENOM.equals(preferred.getCode())) return;
		}
		final CUnit target = hit.visit(AbilityTargetVisitor.UNIT);
		if (target == null || target.isDead() || target.isInvulnerable() || target.isUnitAlly(game.getPlayer(attacker.getPlayerIndex()))
				|| !target.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;
		final float duration = getDurationForTarget(target);
		if (duration <= 0) return;
		for (final var ability : target.getAbilities()) {
			if (ability instanceof Poison poison && poison.attacker == attacker && poison.getCode().equals(getCode())) {
				poison.refreshDuration(game);
				return;
			}
		}
		target.add(game, new Poison(game.getHandleIdAllocator().createId(), getCode(), getAlias(), attacker, duration,
				this.damagePerSecond, this.movementSlow, this.attackSlow));
	}

	public static final class Poison extends CBuffTimed {
		private static final int PULSE_TICKS = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private final CUnit attacker;
		private final float damage;
		private final NonStackingStatBuff movement;
		private final NonStackingStatBuff attack;
		private int nextPulse;

		private Poison(final int handleId, final War3ID code, final War3ID alias, final CUnit attacker,
				final float duration, final float damage, final float movementSlow, final float attackSlow) {
			super(handleId, code, alias, duration);
			this.attacker = attacker;
			this.damage = damage;
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "slowPoison", -movementSlow);
			this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "slowPoison", -attackSlow);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			if (this.movement.getValue() != 0) unit.addNonStackingStatBuff(this.movement);
			if (this.attack.getValue() != 0) unit.addNonStackingStatBuff(this.attack);
			this.nextPulse = game.getGameTurnTick() + PULSE_TICKS;
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			if (this.movement.getValue() != 0) unit.removeNonStackingStatBuff(this.movement);
			if (this.attack.getValue() != 0) unit.removeNonStackingStatBuff(this.attack);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (game.getGameTurnTick() >= this.nextPulse) {
				this.nextPulse += PULSE_TICKS;
				if (!unit.isInvulnerable() && unit.getLife() > 2 && this.damage > 0) {
					unit.setLife(game, Math.max(2, unit.getLife() - this.damage));
				}
			}
			super.onTick(game, unit);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}

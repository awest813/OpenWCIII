package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackFinalDamageTakenModificationListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;

/** Grants spell immunity in the original campaigns and absorbs damage when a capacity is authored. */
public final class CAbilityAntiMagicShell extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bams");
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID buffId;
	private float capacity;

	public CAbilityAntiMagicShell(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.antimagicshell;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		final String authoredBuff = definition.getField(AbilityFields.BUFF + level);
		this.buffId = authoredBuff.length() == 4 ? War3ID.fromString(authoredBuff) : DEFAULT_BUFF;
		this.capacity = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit == null || !unit.checkForAbilityEffectReaction(game, caster, this)) {
			return false;
		}
		apply(game, unit, this.buffId, getDurationForTarget(unit), this.capacity);
		return false;
	}

	public static void apply(final CSimulation game, final CUnit unit, final War3ID buffId,
			final float duration, final float capacity) {
		if (duration <= 0) return;
		final Shell previous = unit.getFirstAbilityOfType(Shell.class);
		if (previous != null) {
			unit.remove(game, previous);
		}
		unit.add(game, new Shell(game.getHandleIdAllocator().createId(),
				War3ID.NONE.equals(buffId) ? DEFAULT_BUFF : buffId, duration, capacity));
	}

	public static boolean grantsMagicImmunity(final CUnit unit) {
		final Shell shell = unit.getFirstAbilityOfType(Shell.class);
		return shell != null && shell.magicImmunity;
	}

	private static final class Shell extends CBuffTimed implements CUnitAttackFinalDamageTakenModificationListener {
		private final StateModBuff protection = new StateModBuff(StateModBuffType.MAGIC_IMMUNE, 1);
		private final boolean magicImmunity;
		private float remaining;

		Shell(final int handleId, final War3ID alias, final float duration, final float capacity) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.magicImmunity = capacity <= 0;
			this.remaining = capacity;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			if (this.magicImmunity) {
				unit.addStateModBuff(this.protection);
				unit.computeUnitState(game, StateModBuffType.MAGIC_IMMUNE);
			}
			else {
				unit.addFinalDamageTakenModificationListener(this);
			}
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			if (this.magicImmunity) {
				unit.removeStateModBuff(this.protection);
				unit.computeUnitState(game, StateModBuffType.MAGIC_IMMUNE);
			}
			else {
				unit.removeFinalDamageTakenModificationListener(this);
			}
		}

		@Override
		public float onDamage(final CSimulation game, final CUnit attacker, final CUnit target, final boolean isAttack,
				final boolean isRanged, final CAttackType attackType, final CDamageType damageType, final float damage) {
			if (damage <= 0 || (attackType != CAttackType.SPELLS && attackType != CAttackType.MAGIC)) {
				return damage;
			}
			final float absorbed = Math.min(this.remaining, damage);
			this.remaining -= absorbed;
			if (this.remaining <= 0) {
				target.remove(game, this);
			}
			return damage - absorbed;
		}

		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.BENEFICIAL;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/** The Necromancer's two researched, dispellable enchantments. */
public final class CAbilityNecromancerEnchantment extends CAbilityTargetSpellBase {
	private static final War3ID CRIPPLE_BUFF = War3ID.fromString("Bcri");
	private static final War3ID FRENZY_BUFF = War3ID.fromString("Buhf");
	private static final String CRIPPLE_ART = "Abilities\\Spells\\Undead\\Cripple\\CrippleTarget.mdl";
	private static final String FRENZY_ART = "Abilities\\Spells\\Undead\\UnholyFrenzy\\UnholyFrenzyTarget.mdl";
	private final boolean cripple;
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID buffId;
	private float primaryEffect;
	private float secondaryEffect;
	private float damagePenalty;

	public CAbilityNecromancerEnchantment(final int handleId, final War3ID alias, final boolean cripple) {
		super(handleId, alias);
		this.cripple = cripple;
	}

	@Override
	public int getBaseOrderId() {
		return this.cripple ? OrderIds.cripple : OrderIds.unholyfrenzy;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.primaryEffect = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.secondaryEffect = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damagePenalty = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(
					CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				owner.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final var owner = game.getPlayer(unit.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (owner.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit) receiver.targetOk(target);
		else receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit == null || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final War3ID code = this.cripple ? CRIPPLE_BUFF : FRENZY_BUFF;
		final boolean authoredBuff = this.buffId != null && !War3ID.NONE.equals(this.buffId);
		for (final var ability : List.copyOf(unit.getAbilities())) {
			if (ability instanceof Enchantment previous && previous.getCode().equals(code)) unit.remove(game, previous);
		}
		final var stats = new ArrayList<NonStackingStatBuff>();
		final String key = code.toString();
		final float attackSpeed = this.cripple ? -this.secondaryEffect : this.primaryEffect;
		stats.add(new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, key, attackSpeed));
		if (this.cripple) {
			stats.add(new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, key, -this.primaryEffect));
			stats.add(new NonStackingStatBuff(NonStackingStatBuffType.ALLATKPCT, key, -this.damagePenalty));
		}
		unit.add(game, new Enchantment(game.getHandleIdAllocator().createId(), code, authoredBuff ? this.buffId : code,
				getDurationForTarget(unit), stats, caster, this.cripple ? 0 : this.secondaryEffect,
				authoredBuff ? null : this.cripple ? CRIPPLE_ART : FRENZY_ART));
		return false;
	}

	private static final class Enchantment extends CBuffTimed {
		private static final int DAMAGE_INTERVAL_TICKS = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		private final List<NonStackingStatBuff> stats;
		private final CUnit source;
		private final float damagePerSecond;
		private final String fallbackArt;
		private int nextDamage;

		private Enchantment(final int handleId, final War3ID code, final War3ID alias, final float duration,
				final List<NonStackingStatBuff> stats, final CUnit source, final float damagePerSecond, final String fallbackArt) {
			super(handleId, code, alias, duration);
			this.stats = stats;
			this.source = source;
			this.damagePerSecond = damagePerSecond;
			this.fallbackArt = fallbackArt;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.stats.forEach(unit::addNonStackingStatBuff);
			this.nextDamage = game.getGameTurnTick() + DAMAGE_INTERVAL_TICKS;
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			this.stats.forEach(unit::removeNonStackingStatBuff);
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			final int tick = game.getGameTurnTick();
			if (this.damagePerSecond > 0 && tick >= this.nextDamage
					&& (tick == this.nextDamage || getDurationRemaining(game, unit) > 0)) {
				this.nextDamage = tick + DAMAGE_INTERVAL_TICKS;
				unit.damage(game, this.source, false, false, CAttackType.SPELLS, CDamageType.UNIVERSAL, null,
						this.damagePerSecond);
			}
			if (!unit.isDead()) super.onTick(game, unit);
		}

		@Override
		protected SimulationRenderComponent createArt(final CSimulation game, final CUnit unit) {
			return this.fallbackArt == null ? super.createArt(game, unit)
					: game.createPersistentEffectOnUnit(unit, this.fallbackArt);
		}

		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return CRIPPLE_BUFF.equals(getCode()) ? DispelPolarity.HARMFUL : DispelPolarity.BENEFICIAL;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.CAbilityPurge;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich.CBuffFrost;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.listeners.CUnitAttackPostDamageListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/** Applies the authored weapon bonus and effects of the original inventory orbs. */
public final class CAbilityItemOrb extends CAbilityPassiveSpellBase implements CUnitAttackPostDamageListener {
	public static final War3ID FIRE = War3ID.fromString("AIfb");
	public static final War3ID FROST = War3ID.fromString("AIob");
	public static final War3ID VENOM = War3ID.fromString("AIpb");
	public static final War3ID SPELL = War3ID.fromString("AIsb");
	public static final War3ID LEGACY_LIGHTNING = War3ID.fromString("AIlb");
	public static final War3ID LEGACY_PURGE = War3ID.fromString("AIlp");
	private static final War3ID SLOW_BUFF = War3ID.fromString("Bslo");
	private static final War3ID TIMED_LIFE = War3ID.fromString("BTLF");
	private final NonStackingStatBuff attackBonus;
	private int alternateAttackMask;
	private float radius;
	private String projectileArt;
	private War3ID effectAbility;
	private int unitChance;
	private int heroChance;
	private int summonedChance;
	private CItem item;
	private CAbilityPurge legacyPurge;

	public CAbilityItemOrb(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
		this.attackBonus = new NonStackingStatBuff(NonStackingStatBuffType.ALLATK, NonStackingStatBuff.ALLOW_STACKING_KEY, 0);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		if (LEGACY_PURGE.equals(getCode())) {
			this.legacyPurge = new CAbilityPurge(getHandleId(), getAlias());
			this.legacyPurge.populate(data, level);
		}
		else {
			this.attackBonus.setValue(data.getFieldAsInteger(AbilityFields.DATA_A + level, 0));
		}
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		final int attackIndex = data.getFieldAsInteger(AbilityFields.DATA_E + level, 0);
		this.alternateAttackMask = attackIndex >= 1 && attackIndex <= 2 ? 1 << (attackIndex - 1) : 0;
		this.projectileArt = data.getFieldAsString("Missileart", 0);
		final String effect = data.readSLKTag(AbilityFields.UNIT_ID + level);
		this.effectAbility = effect.length() == 4 ? War3ID.fromString(effect) : null;
		this.unitChance = data.getFieldAsInteger(AbilityFields.DATA_B + level, 0);
		this.heroChance = data.getFieldAsInteger(AbilityFields.DATA_C + level, 0);
		this.summonedChance = data.getFieldAsInteger(AbilityFields.DATA_D + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	public int getAlternateAttackMask() {
		return this.alternateAttackMask;
	}

	public static CAbilityItemOrb preferredOrb(final CUnit source) {
		final var inventory = source.getInventoryData();
		return inventory == null ? null : inventory.getFirstEquippedAbility(CAbilityItemOrb.class);
	}

	public static String equippedProjectileArt(final CUnit source) {
		final CAbilityItemOrb orb = preferredOrb(source);
		return orb == null || orb.projectileArt == null || orb.projectileArt.isBlank() ? null : orb.projectileArt;
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.item = item;
		setPermanent(item != null);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		unit.addNonStackingStatBuff(this.attackBonus);
		if (!VENOM.equals(getCode())) unit.addPostDamageListener(this);
		unit.computeUnitState(game, StateModBuffType.DISABLE_ATTACK);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		unit.removeNonStackingStatBuff(this.attackBonus);
		if (!VENOM.equals(getCode())) unit.removePostDamageListener(this);
		unit.computeUnitState(game, StateModBuffType.DISABLE_ATTACK);
	}

	@Override
	public void onHit(final CSimulation game, final CUnit attacker, final AbilityTarget target, final float damage) {
		if (damage <= 0) return;
		final CUnit hit = target.visit(AbilityTargetVisitor.UNIT);
		if (hit == null || attacker == hit) return;
		final var owner = game.getPlayer(attacker.getPlayerIndex());
		if (owner.hasAlliance(hit.getPlayerIndex(), CAllianceType.PASSIVE)) return;
		if (this.legacyPurge != null) {
			final CAbilityItemOrb preferred = preferredOrb(attacker);
			if ((preferred == null || preferred == this || this.item != null && this.item == preferred.item)
					&& !hit.isDead() && !hit.isMagicImmune() && !hit.isInvulnerable()
					&& hit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				this.legacyPurge.doEffect(game, attacker, hit);
			}
			return;
		}
		if (SPELL.equals(getCode())) {
			applySpellEffect(game, attacker, hit);
			return;
		}
		if (FROST.equals(getCode())) {
			final CAbilityItemOrb preferred = preferredOrb(attacker);
			if ((preferred == null || preferred == this) && hit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				CBuffFrost.apply(game, hit, getDurationForTarget(hit));
				game.createTemporarySpellEffectOnUnit(hit, getAlias(), CEffectType.SPECIAL);
			}
			return;
		}
		if (this.radius <= 0 || this.attackBonus.getValue() <= 0) return;
		game.createTemporarySpellEffectOnUnit(hit, getAlias(), CEffectType.SPECIAL);
		game.getWorldCollision().enumUnitsInRange(hit.getX(), hit.getY(), this.radius, nearby -> {
			if (nearby != hit && !nearby.isDead() && !nearby.isHidden()
					&& !owner.hasAlliance(nearby.getPlayerIndex(), CAllianceType.PASSIVE)
					&& nearby.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				nearby.damage(game, attacker, false, true, CAttackType.SPELLS, CDamageType.FIRE, null, this.attackBonus.getValue());
			}
			return false;
		});
	}

	private void applySpellEffect(final CSimulation game, final CUnit attacker, final CUnit hit) {
		final CAbilityItemOrb preferred = preferredOrb(attacker);
		if (this.effectAbility == null || preferred != null && preferred != this) return;
		final GameObject effect = game.getAbilityData().getDefinition(this.effectAbility);
		if (effect == null || hit.isInvulnerable() || hit.isMagicImmune()) return;
		final String code = effect.readSLKTag("code");
		final boolean summonsDarkMinion = "ANbs".equals(code);
		if (summonsDarkMinion) {
			if (!hit.isDead() || hit.isHero() || !hit.isRaisable() || hit.isSummonedOrWard()) return;
		}
		else if (hit.isDead() || !hit.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
				BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;
		final int chance = hit.isHero() ? this.heroChance : hit.isSummonedOrWard() ? this.summonedChance : this.unitChance;
		if (chance <= 0 || chance < 100 && game.getSeededRandom().nextInt(100) >= chance) return;
		final var targets = CTargetType.parseTargetTypeSet(effect.getFieldAsList(AbilityFields.TARGETS_ALLOWED + 1));
		if (targets.isEmpty()) targets.addAll(CTargetType.parseTargetTypeSet(effect.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		if (summonsDarkMinion) {
			if (hit.checkForAbilityEffectReaction(game, attacker, this)) summonDarkMinion(game, attacker, hit, effect);
		}
		else if (hit.canBeTargetedBy(game, attacker, false, targets, BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
			if ("Aslo".equals(code) && hit.checkForAbilityEffectReaction(game, attacker, this)) {
				applySlow(game, hit, effect);
			}
			else if ("Aprg".equals(code) && game.getAbilityData().createAbility(this.effectAbility,
					game.getHandleIdAllocator().createId()) instanceof CAbilityPurge purge) {
				purge.doEffect(game, attacker, hit);
			}
		}
	}

	private static void applySlow(final CSimulation game, final CUnit target, final GameObject effect) {
		final float duration = effect.readSLKTagFloat((target.isHero() ? AbilityFields.HERO_DURATION : AbilityFields.DURATION) + 1);
		if (duration <= 0) return;
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(effect, 1);
		final War3ID alias = buff == null || War3ID.NONE.equals(buff) ? SLOW_BUFF : buff;
		final OrbSlow previous = target.getFirstAbilityOfType(OrbSlow.class);
		if (previous != null) target.remove(game, previous);
		target.add(game, new OrbSlow(game.getHandleIdAllocator().createId(), alias, duration,
				effect.readSLKTagFloat(AbilityFields.DATA_A + 1), effect.readSLKTagFloat(AbilityFields.DATA_B + 1)));
	}

	private static void summonDarkMinion(final CSimulation game, final CUnit attacker, final CUnit victim, final GameObject effect) {
		final String minionType = effect.readSLKTag(AbilityFields.UNIT_ID + 1);
		final float duration = effect.readSLKTagFloat(AbilityFields.DATA_C + 1);
		if (minionType.length() != 4 || duration <= 0) return;
		final CUnit minion = game.createUnitSimple(War3ID.fromString(minionType), attacker.getPlayerIndex(),
				victim.getX(), victim.getY(), victim.getFacing());
		minion.addClassification(CUnitClassification.SUMMONED);
		minion.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), TIMED_LIFE, duration, true));
		attacker.fireSummonEvents(game, minion);
	}

	private static final class OrbSlow extends CBuffTimed {
		private final NonStackingStatBuff movement;
		private final NonStackingStatBuff attack;

		private OrbSlow(final int handleId, final War3ID alias, final float seconds, final float movement, final float attack) {
			super(handleId, SLOW_BUFF, alias, seconds);
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "slow", -movement);
			this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "slow", -attack);
		}

		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.HARMFUL;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.movement);
			unit.addNonStackingStatBuff(this.attack);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.movement);
			unit.removeNonStackingStatBuff(this.attack);
		}
	}
}

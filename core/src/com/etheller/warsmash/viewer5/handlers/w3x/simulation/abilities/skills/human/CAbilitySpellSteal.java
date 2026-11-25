package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human;

import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff.DispelPolarity;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff.ABGenericTimedBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilitySpellSteal extends CAbilityTargetSpellBase implements CAutocastAbility {
	public static final War3ID CODE = War3ID.fromString("Asps");
	private static final War3ID SLOW = War3ID.fromString("Bslo");
	private static final War3ID POLYMORPH = War3ID.fromString("Aply");
	private static final Set<War3ID> TRANSFERABLE_BUFFS = Set.of(War3ID.fromString("Bblo"), War3ID.fromString("Bfae"),
			War3ID.fromString("Bcri"), POLYMORPH);
	private float radius;
	private boolean autocast;

	public CAbilitySpellSteal(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.spellsteal; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.spellstealon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.spellstealoff; }
	@Override public AutocastType getAutocastType() { return AutocastType.NEARESTVALID; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }
	@Override public boolean isUniversal() { return true; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		setAutoCastOn(unit, true);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setAutoCastOn(unit, false);
	}

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		if (this.autocast == enabled) return;
		this.autocast = enabled;
		unit.setAutocastAbility(enabled ? this : null);
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit source)) receiver.orderIdNotAccepted();
		else if (findTransfer(game, caster, source) != null) receiver.targetOk(source);
		else receiver.targetCheckFailed(isFriendly(game, caster, source)
				? CommandStringErrorKeys.MUST_TARGET_A_FRIENDLY_UNIT_WITH_NEGATIVE_BUFFS
				: CommandStringErrorKeys.MUST_TARGET_AN_ENEMY_UNIT_WITH_POSITIVE_BUFFS);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && caster.canReach(unit, getCastRange()) && unit.isVisible(game, caster.getPlayerIndex())) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit unit, final int orderId, final AbilityPointTarget point,
			final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (target instanceof CUnit source && source.checkForAbilityEffectReaction(game, caster, this)) {
			final Transfer transfer = findTransfer(game, caster, source);
			if (transfer == null) return false;
			if (transfer.buff() instanceof CBuffTimed buff) buff.transferTo(game, source, transfer.recipient());
			else if (transfer.buff() instanceof ABGenericTimedBuff buff) buff.transferTo(game, source, transfer.recipient());
			game.createTemporarySpellEffectOnUnit(transfer.recipient(), getAlias(), CEffectType.TARGET);
		}
		return false;
	}

	private Transfer findTransfer(final CSimulation game, final CUnit caster, final CUnit source) {
		final boolean friendlySource = isFriendly(game, caster, source);
		final DispelPolarity desired = friendlySource ? DispelPolarity.HARMFUL : DispelPolarity.BENEFICIAL;
		for (final var ability : source.getAbilities()) {
			if (!(ability instanceof CBuff buff) || polarity(buff) != desired || buff.getDurationRemaining(game, source) <= 0) continue;
			final CUnit[] closest = new CUnit[1];
			game.getWorldCollision().enumUnitsInRange(source.getX(), source.getY(), this.radius, candidate -> {
				if (candidate == source || candidate.isDead() || candidate.isHidden() || candidate.isInvulnerable()
						|| candidate.isBuilding() || isFriendly(game, caster, candidate) == friendlySource
						|| friendlySource && candidate.isMagicImmune()
						|| POLYMORPH.equals(buff.getCode()) && candidate.isHero()
						|| candidate.getAbilities().stream().anyMatch(other -> buff.getAlias().equals(other.getAlias()))) return false;
				if (closest[0] == null || source.distanceSquaredNoCollision(candidate) < source.distanceSquaredNoCollision(closest[0])) {
					closest[0] = candidate;
				}
				return false;
			});
			if (closest[0] != null) return new Transfer(buff, closest[0]);
		}
		return null;
	}

	private static DispelPolarity polarity(final CBuff buff) {
		if (buff instanceof ABGenericTimedBuff && SLOW.equals(buff.getAlias()) && buff.isDispellable()) return buff.getDispelPolarity();
		if (buff instanceof CBuffTimed && buff.isDispellable() && !buff.isHeroSpellBuff()
				&& TRANSFERABLE_BUFFS.contains(buff.getCode())) return buff.getDispelPolarity();
		return DispelPolarity.UNCLASSIFIED;
	}

	private static boolean isFriendly(final CSimulation game, final CUnit caster, final CUnit unit) {
		return game.getPlayer(caster.getPlayerIndex()).hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE);
	}

	private record Transfer(CBuff buff, CUnit recipient) {
	}
}

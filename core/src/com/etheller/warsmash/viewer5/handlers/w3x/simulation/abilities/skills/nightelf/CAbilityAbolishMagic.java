package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff.DispelPolarity;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityAntiMagicShell;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityAbolishMagic extends CAbilityTargetSpellBase implements CAutocastAbility {
	public static final War3ID CODE = War3ID.fromString("Aadm");
	private List<CUnitTypeRequirement> requirements = List.of();
	private float summonDamage;
	private boolean autocast;

	public CAbilityAbolishMagic(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.autodispel; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.autodispelon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.autodispeloff; }
	@Override public AutocastType getAutocastType() { return AutocastType.NEARESTVALID; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }
	@Override public boolean isUniversal() { return true; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.summonDamage = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		return this.requirements.stream().allMatch(requirement -> isRequirementMet(game, unit, requirement));
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		for (final var requirement : this.requirements) {
			if (!isRequirementMet(game, unit, requirement)) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	private static boolean isRequirementMet(final CSimulation game, final CUnit unit, final CUnitTypeRequirement requirement) {
		return game.getPlayer(unit.getPlayerIndex()).getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel();
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		super.onAdd(game, unit);
		setAutoCastOn(unit, true);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setAutoCastOn(unit, false);
		super.onRemove(game, unit);
	}

	@Override
	public void setAutoCastOn(final CUnit unit, final boolean enabled) {
		if (this.autocast == enabled) return;
		this.autocast = enabled;
		unit.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && unit.isVisible(game, caster.getPlayerIndex())
				&& caster.canReach(unit, getCastRange()) && hasAutocastBuff(unit, polarity(game, caster, unit))) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit unit, final int orderId, final AbilityPointTarget target,
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
		if (target instanceof CUnit unit && unit.checkForAbilityEffectReaction(game, caster, this)) {
			final DispelPolarity polarity = polarity(game, caster, unit);
			final boolean hadImmuneShell = CAbilityAntiMagicShell.grantsMagicImmunity(unit);
			unit.dispelBuffs(game, buff -> buff.getDispelPolarity() == polarity);
			if (!hadImmuneShell && polarity == DispelPolarity.BENEFICIAL && unit.isSummonedOrWard() && this.summonDamage > 0) {
				unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, this.summonDamage);
			}
			game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		}
		else if (target instanceof CDestructable tree) tree.dispelBuffs(game);
		return false;
	}

	private static DispelPolarity polarity(final CSimulation game, final CUnit caster, final CUnit target) {
		return game.getPlayer(caster.getPlayerIndex()).hasAlliance(target.getPlayerIndex(), CAllianceType.PASSIVE)
				? DispelPolarity.HARMFUL : DispelPolarity.BENEFICIAL;
	}

	private static boolean hasAutocastBuff(final CUnit target, final DispelPolarity polarity) {
		return target.getAbilities().stream().anyMatch(ability -> ability instanceof CBuff buff && buff.isDispellable()
				&& buff.getDispelPolarity() == polarity && !buff.isHeroSpellBuff());
	}
}

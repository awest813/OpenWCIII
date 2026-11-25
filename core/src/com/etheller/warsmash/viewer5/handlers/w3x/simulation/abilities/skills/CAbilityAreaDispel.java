package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityCyclone;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityAntiMagicShell;
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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityAreaDispel extends CAbilityPointTargetSpellBase {
	private final boolean detonate;
	private final boolean devour;
	private final int orderId;
	private boolean powerup;
	private List<CUnitTypeRequirement> requirements = List.of();
	private float radius;
	private float manaLoss;
	private float summonDamage;
	private float lifePerUnit;
	private float manaPerUnit;
	private float lifePerBuff;
	private float manaPerBuff;

	public CAbilityAreaDispel(final int handleId, final War3ID alias, final int orderId) {
		super(handleId, alias);
		this.orderId = orderId;
		this.detonate = orderId == OrderIds.detonate;
		this.devour = orderId == OrderIds.devourmagic;
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.powerup = item != null && item.getItemType().isUseAutomaticallyWhenAcquired();
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId, final AbilityTarget target) {
		if (this.powerup && orderId == this.orderId) {
			// Pickup removes the temporary spell before the ordinary cast behavior could run.
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		if (this.devour) {
			this.lifePerUnit = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
			this.manaPerUnit = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
			this.lifePerBuff = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
			this.manaPerBuff = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
			this.summonDamage = definition.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		}
		else {
			this.manaLoss = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
			this.summonDamage = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		}
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			getTargetsAllowed().addAll(
					CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
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
		final float x = this.detonate || this.powerup ? caster.getX() : target.getX();
		final float y = this.detonate || this.powerup ? caster.getY() : target.getY();
		game.spawnSpellEffectOnPoint(x, y, 0, getAlias(), CEffectType.SPECIAL, 0);
		final var recipients = new ArrayList<CUnit>();
		final var cycloneTargets = getTargetsAllowed().clone();
		cycloneTargets.add(CTargetType.INVULNERABLE);
		cycloneTargets.add(CTargetType.VULNERABLE);
		final var shellTargets = getTargetsAllowed().clone();
		shellTargets.remove(CTargetType.NON_MAGIC_IMMUNE);
		game.getWorldCollision().enumUnitsInRange(x, y, this.radius, unit -> {
			if ((!this.detonate || unit != caster) && !unit.isDead() && !unit.isHidden()
					&& unit.canBeTargetedBy(game, caster, false, CAbilityCyclone.hasDispellableLift(unit) ? cycloneTargets
							: CAbilityAntiMagicShell.grantsMagicImmunity(unit) ? shellTargets : getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				recipients.add(unit);
			}
			return false;
		});
		final var owner = game.getPlayer(caster.getPlayerIndex());
		for (final CUnit unit : recipients) {
			final boolean hostile = !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE);
			final int buffsRemoved = dispelUnit(game, caster, unit, this.manaLoss, this.summonDamage, hostile);
			if (this.devour && hostile && buffsRemoved > 0) {
				caster.heal(game, this.lifePerUnit + buffsRemoved * this.lifePerBuff);
				caster.restoreMana(game, this.manaPerUnit + buffsRemoved * this.manaPerBuff);
			}
		}
		if (this.devour || getTargetsAllowed().contains(CTargetType.TREE)) {
			game.getWorldCollision().enumDestructablesInRange(x, y, this.radius, tree -> {
				if (!tree.isDead()) tree.dispelBuffs(game);
				return false;
			});
		}
		if (this.detonate) {
			caster.setExplodesOnDeath(true);
			caster.kill(game);
		}
		return false;
	}

	static int dispelUnit(final CSimulation game, final CUnit caster, final CUnit target,
			final float manaLoss, final float summonDamage, final boolean hostile) {
		final boolean hadSpellImmuneShell = CAbilityAntiMagicShell.grantsMagicImmunity(target);
		final int buffsRemoved = target.dispelBuffs(game);
		if (hadSpellImmuneShell) return buffsRemoved;
		if (manaLoss > 0 && target.getMana() > 0) target.setMana(Math.max(0, target.getMana() - manaLoss));
		if (summonDamage > 0 && target.isSummonedOrWard() && hostile) {
			target.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, summonDamage);
		}
		return buffsRemoved;
	}
}

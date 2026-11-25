package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.human.paladin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public class CAbilityResurrect extends CAbilityNoTargetSpellBase {
	private float areaOfEffect;
	private int numberOfCorpsesRaised;

	public CAbilityResurrect(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.resurrection;
	}

	@Override
	public void populateData(final GameObject worldEditorAbility, final int level) {
		this.numberOfCorpsesRaised = worldEditorAbility.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		if (!getTargetsAllowed().contains(CTargetType.DEAD)) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(worldEditorAbility.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
		getTargetsAllowed().add(CTargetType.DEAD);
		getTargetsAllowed().add(CTargetType.FRIEND);
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		final boolean[] hasTarget = { false };
		game.getWorldCollision().enumCorpsesInRange(caster.getX(), caster.getY(), this.areaOfEffect, unit -> {
			hasTarget[0] = canRaise(game, caster, unit);
			return hasTarget[0];
		});
		if (this.numberOfCorpsesRaised > 0 && hasTarget[0]) receiver.targetOk(null);
		else receiver.targetCheckFailed(CommandStringErrorKeys.THERE_ARE_NO_CORPSES_OF_FRIENDLY_UNITS_NEARBY);
	}

	private boolean canRaise(final CSimulation game, final CUnit caster, final CUnit corpse) {
		return !corpse.isHidden() && corpse.isRaisable() && corpse.isDecays() && !corpse.isHero() && !corpse.isBuilding()
				&& corpse.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset());
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit caster, final AbilityTarget target) {
		final List<CUnit> corpses = new ArrayList<>();
		simulation.getWorldCollision().enumCorpsesInRange(caster.getX(), caster.getY(), this.areaOfEffect, unit -> {
			if (canRaise(simulation, caster, unit)) corpses.add(unit);
			return false;
		});
		corpses.sort(Comparator.<CUnit>comparingInt(unit -> unit.getUnitType().getLevel()).reversed()
				.thenComparingDouble(caster::distanceSquaredNoCollision).thenComparingInt(CUnit::getHandleId));
		for (int index = 0; index < Math.min(this.numberOfCorpsesRaised, corpses.size()); index++) {
			final CUnit unit = corpses.get(index);
			unit.resurrect(simulation);
			final var owner = simulation.getPlayer(unit.getPlayerIndex());
			owner.setUnitFoodUsed(unit, unit.getUnitType().getFoodUsed());
			owner.setUnitFoodMade(unit, unit.getUnitType().getFoodMade());
			owner.addTechtreeUnlocked(simulation, unit.getTypeId());
			simulation.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
		}
		simulation.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}
}

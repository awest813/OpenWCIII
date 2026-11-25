package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.ArrayList;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityTornadoBuildingDamage extends CAbilityPassiveSpellBase {
	private float outerDamage;
	private float mediumRadius;
	private float mediumDamage;
	private float innerRadius;
	private float innerDamage;
	private int nextDamageTick;

	public CAbilityTornadoBuildingDamage(final int handleId, final War3ID alias) {
		super(handleId, War3ID.fromString("Atdg"), alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.outerDamage = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.mediumRadius = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.mediumDamage = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.innerRadius = definition.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		this.innerDamage = definition.getFieldAsFloat(AbilityFields.DATA_E + level, 0);
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit tornado) {
		this.nextDamageTick = game.getGameTurnTick() + (int) Math.ceil(1 / WarsmashConstants.SIMULATION_STEP_TIME);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit tornado) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextDamageTick || getAreaOfEffect() <= 0) return;
		this.nextDamageTick = tick + (int) Math.ceil(1 / WarsmashConstants.SIMULATION_STEP_TIME);
		final var structures = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(tornado.getX(), tornado.getY(), getAreaOfEffect(), unit -> {
			if (unit.isBuilding() && !unit.isDead() && !unit.isHidden()
					&& unit.canBeTargetedBy(game, tornado, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				structures.add(unit);
			}
			return false;
		});
		for (final CUnit structure : structures) {
			final double distance = tornado.distanceSquaredNoCollision(structure);
			final float damage = this.innerRadius > 0 && distance <= this.innerRadius * this.innerRadius ? this.innerDamage
					: distance <= this.mediumRadius * this.mediumRadius ? this.mediumDamage : this.outerDamage;
			if (damage > 0) structure.damage(game, tornado, false, false, CAttackType.SPELLS, CDamageType.DEMOLITION, null, damage);
		}
	}
}

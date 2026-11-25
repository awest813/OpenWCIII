package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;

/** Graveyard corpse production. */
public final class CAbilityCreateCorpse extends CAbilityPassiveSpellBase {
	private static final War3ID CODE = War3ID.fromString("Agyd");
	private int maximumCorpses;
	private int intervalTicks;
	private float graveRadius;
	private float corpseRadius;
	private War3ID corpseType;
	private int nextSpawn = -1;

	public CAbilityCreateCorpse(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final float cooldown = definition.getFieldAsFloat(AbilityFields.COOLDOWN + level, 0);
		this.intervalTicks = Math.max(1, (int) Math.ceil(cooldown / WarsmashConstants.SIMULATION_STEP_TIME));
		this.maximumCorpses = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.graveRadius = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.corpseRadius = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.corpseType = definition.getFieldAsWar3ID(AbilityFields.UNIT_ID + level, 0);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
		this.nextSpawn = -1;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit graveyard) {
		if (isDisabled() || graveyard.isConstructingOrUpgrading() || graveyard.isHidden()) {
			this.nextSpawn = -1;
			return;
		}
		final int tick = game.getGameTurnTick();
		if (this.nextSpawn < 0) this.nextSpawn = tick + this.intervalTicks;
		if (tick < this.nextSpawn) return;
		this.nextSpawn = tick + this.intervalTicks;
		if (this.maximumCorpses <= 0 || this.corpseRadius <= 0 || this.corpseType == null
				|| War3ID.NONE.equals(this.corpseType)) return;
		final var corpseUnitType = game.getUnitData().getUnitType(this.corpseType);
		if (corpseUnitType == null || !corpseUnitType.isRaise()) return;
		final var nearbyCorpses = game.getUnitsIncludingPending().stream().filter(unit -> unit.isDead()
				&& !unit.isHidden() && unit.isRaisable() && game.containsUnit(unit)
				&& graveyard.distanceSquaredNoCollision(unit) <= this.corpseRadius * this.corpseRadius).toList();
		if (nearbyCorpses.size() >= this.maximumCorpses) return;
		final float radius = this.graveRadius > 0 ? Math.min(this.graveRadius, this.corpseRadius) : this.corpseRadius;
		final float initialAngle = game.getSeededRandom().nextFloat() * 360;
		final float collision = corpseUnitType.getCollisionSize();
		for (int index = 0; index < 12; index++) {
			final float facing = initialAngle + index * 30;
			final double radians = Math.toRadians(facing);
			final float x = graveyard.getX() + (float) Math.cos(radians) * radius;
			final float y = graveyard.getY() + (float) Math.sin(radians) * radius;
			if (!game.getPathingGrid().isPathable(x, y, PathingGrid.MovementType.FOOT, collision)
					|| nearbyCorpses.stream().anyMatch(unit -> unit.distanceSquaredNoCollision(x, y) < collision * collision)) {
				continue;
			}
			final CUnit corpse = game.createUnitSimple(this.corpseType, graveyard.getPlayerIndex(), x, y, facing % 360);
			if (corpse != null) corpse.kill(game);
			return;
		}
	}
}

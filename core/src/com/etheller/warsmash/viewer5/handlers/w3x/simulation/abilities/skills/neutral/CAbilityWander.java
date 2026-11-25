package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.Random;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;

public final class CAbilityWander extends CAbilityPassiveSpellBase {
	public static final War3ID CRITTER_CODE = War3ID.fromString("Awan");
	public static final War3ID TORNADO_CODE = War3ID.fromString("Atwa");
	private final boolean critter;
	private Random critterRandom;
	private int nextWander;
	private float originX;
	private float originY;

	public CAbilityWander(final int handleId, final War3ID alias, final War3ID code) {
		super(handleId, code, alias);
		this.critter = CRITTER_CODE.equals(code);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		// Unlike Tornado Wander, the stock critter ability has no movement fields.
		if (this.critter) {
			setDuration(3);
			setCastRange(96);
			setAreaOfEffect(256);
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.originX = unit.getX();
		this.originY = unit.getY();
		this.nextWander = game.getGameTurnTick() + interval();
		if (this.critter) {
			this.critterRandom = new Random(unit.getHandleId());
			this.nextWander += this.critterRandom.nextInt(interval());
		}
	}

	@Override
	public void onTick(final CSimulation game, final CUnit unit) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextWander || getDuration() <= 0 || getCastRange() <= 0 || unit.isMovementDisabled()
				|| unit.isHidden() || unit.isPaused()) return;
		final var behavior = unit.getCurrentBehavior();
		if (behavior != null && behavior.getBehaviorCategory() != CBehaviorCategory.IDLE) return;
		this.nextWander = tick + interval();
		final float radius = getAreaOfEffect();
		if (radius > 0 && unit.distanceSquaredNoCollision(this.originX, this.originY) > radius * radius) {
			this.originX = unit.getX();
			this.originY = unit.getY();
		}
		final var grid = game.getPathingGrid();
		final float distance = Math.min(getCastRange(), unit.getSpeed() * getDuration());
		final var random = this.critter ? this.critterRandom : game.getSeededRandom();
		final double heading = random.nextDouble() * Math.PI * 2;
		for (int direction = 0; direction < 8; direction++) {
			final double angle = heading + direction * Math.PI / 4;
			final float x = unit.getX() + (float) Math.cos(angle) * distance;
			final float y = unit.getY() + (float) Math.sin(angle) * distance;
			if (radius > 0 && Math.hypot(x - this.originX, y - this.originY) > radius) continue;
			if (grid.contains(x, y) && grid.isPathable(x, y, unit.getMovementType(), unit.getUnitType().getCollisionSize())
					&& unit.order(game, OrderIds.move, new AbilityPointTarget(x, y))) return;
		}
	}

	private int interval() {
		return Math.max(1, (int) Math.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME));
	}
}

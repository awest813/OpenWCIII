package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.AbstractCAbilityBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.build.CAbilityBuildInProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.AbilityDisableWhileUnderConstructionVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.skills.CBehaviorTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemTinyBuilding extends CAbilityPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIbl");
	private String[] buildingIds;
	private CItem item;

	public CAbilityItemTinyBuilding(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemtinystructures;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final String units = definition.getField(AbilityFields.UNIT_ID + level);
		this.buildingIds = units == null ? new String[0] : units.split(",");
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.item = item;
	}

	@Override
	public boolean consumesItemChargeOnEffect() {
		return true;
	}

	private CUnitType buildingType(final CSimulation game, final CUnit caster) {
		if (this.buildingIds.length == 0) return null;
		final var race = game.getPlayer(caster.getPlayerIndex()).getRace();
		final int raceIndex = race == null ? caster.getUnitType().getRace().ordinal() : race.ordinal();
		final int index = raceIndex >= 0 && raceIndex < this.buildingIds.length ? raceIndex : 0;
		final String rawId = this.buildingIds[index].trim();
		return rawId.length() == 4 ? game.getUnitData().getUnitType(War3ID.fromString(rawId)) : null;
	}

	private boolean canPlace(final CSimulation game, final CUnitType type, final AbilityPointTarget point) {
		return type != null && type.isBuilding() && game.getPathingGrid().contains(point.x, point.y)
				&& game.getPathingGrid().checkPathingTexture(point.x, point.y,
						(int) game.getGameplayConstants().getBuildingAngle(), type.getBuildingPathingPixelMap(),
						type.getPreventedPathingTypes(), type.getRequiredPathingTypes(), game.getWorldCollision(), null);
	}

	@Override
	protected CBehaviorTargetSpellBase createBehavior(final CUnit caster) {
		return new CBehaviorTargetSpellBase(caster, this) {
			@Override
			public boolean isWithinRange(final CSimulation game) {
				final CUnitType type = buildingType(game, caster);
				if (type == null || type.getBuildingPathingPixelMap() == null
						|| type.getBuildingPathingPixelMap() == PathingGrid.BLANK_PATHING) return super.isWithinRange(game);
				return caster.canReachToPathing(getCastRange(), game.getGameplayConstants().getBuildingAngle(),
						type.getBuildingPathingPixelMap(), this.target.getX(), this.target.getY());
			}
		};
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget point, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		final CUnitType type = buildingType(game, caster);
		final AbilityPointTarget snapped = snap(point, type);
		if (canPlace(game, type, snapped)) receiver.targetOk(snapped);
		else receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_BUILD_THERE);
	}

	@Override
	public CBehavior begin(final CSimulation game, final CUnit caster, final int orderId, final AbilityPointTarget point) {
		return super.begin(game, caster, orderId, snap(point, buildingType(game, caster)));
	}

	private static AbilityPointTarget snap(final AbilityPointTarget point, final CUnitType type) {
		final AbilityPointTarget snapped = new AbilityPointTarget(point);
		if (type != null) AbstractCAbilityBuild.roundTargetPoint(snapped, type);
		return snapped;
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnitType type = buildingType(game, caster);
		final AbilityPointTarget point = (AbilityPointTarget) target;
		if (!canPlace(game, type, point)) {
			game.getCommandErrorListener().showInterfaceError(caster.getPlayerIndex(), CommandStringErrorKeys.UNABLE_TO_BUILD_THERE);
			return false;
		}
		final var inventory = caster.getInventoryData();
		if (this.item != null && (inventory == null || !inventory.consumeItemCharge(game, caster, this.item))) return false;
		final CUnit building = game.createUnit(type.getTypeId(), caster.getPlayerIndex(), point.x, point.y,
				game.getGameplayConstants().getBuildingAngle());
		if (building == null) return false;
		building.startItemConstruction(getDuration());
		building.setLife(game, building.getMaximumLife() * WarsmashConstants.BUILDING_CONSTRUCT_START_LIFE);
		building.add(game, new CAbilityBuildInProgress(game.getHandleIdAllocator().createId(), false));
		for (final var ability : building.getAbilities()) {
			ability.visit(AbilityDisableWhileUnderConstructionVisitor.INSTANCE);
		}
		building.checkDisabledAbilities(game, false);
		final var player = game.getPlayer(caster.getPlayerIndex());
		player.setUnitFoodUsed(building, type.getFoodUsed());
		player.addTechtreeInProgress(type.getTypeId());
		player.fireConstructStartEvents(building, game, building);
		game.unitConstructedEvent(caster, building);
		return false;
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import java.util.ArrayList;
import java.util.Comparator;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityUnitOrPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorHumanBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorHumanRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorOrcBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorRepair;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.build.CBehaviorUndeadBuild;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public final class CAbilityItemTownPortal extends CAbilityUnitOrPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AItp");
	private static final float DEFAULT_CHANNEL_SECONDS = 5;
	private final StateModBuff protection = new StateModBuff(StateModBuffType.INVULNERABLE, 1);
	private int unitLimit;
	private boolean cluster;
	private float radius;
	private CUnit townHall;
	private int arrivalTick;
	private SimulationRenderComponent departureArt;
	private SimulationRenderComponent arrivalArt;

	public CAbilityItemTownPortal(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemtownportal;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.unitLimit = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.cluster = definition.getFieldAsBoolean(AbilityFields.DATA_B + level, 0);
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit hall && eligibleTownHall(game, caster, hall)) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_FRIENDLY_TOWN_HALL);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		if (nearbyTownHall(game, caster, target) != null) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_FRIENDLY_TOWN_HALL);
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		if (defaultTownHall(game, caster) != null) receiver.targetOk(null);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_FRIENDLY_TOWN_HALL);
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit caster, final int orderId) {
		final CUnit hall = defaultTownHall(game, caster);
		return hall == null ? caster.pollNextOrderBehavior(game) : super.begin(game, caster, orderId, hall);
	}

	@Override
	public boolean blocksOrdersWhileChanneling() {
		return true;
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit hallTarget = target.visit(AbilityTargetVisitor.UNIT);
		this.townHall = hallTarget == null ? nearbyTownHall(game, caster, target) : hallTarget;
		if (this.townHall == null || !eligibleTownHall(game, caster, this.townHall)) return false;
		this.arrivalTick = game.getGameTurnTick() + (int) Math.ceil(DEFAULT_CHANNEL_SECONDS / WarsmashConstants.SIMULATION_STEP_TIME);
		caster.addStateModBuff(this.protection);
		caster.computeUnitState(game, StateModBuffType.INVULNERABLE);
		this.departureArt = game.spawnSpellEffectOnPoint(caster.getX(), caster.getY(), 0, getAlias(), CEffectType.AREA_EFFECT, 0);
		this.arrivalArt = game.spawnSpellEffectOnPoint(target.getX(), target.getY(), 0, getAlias(), CEffectType.AREA_EFFECT, 0);
		return true;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!eligibleTownHall(game, caster, this.townHall)) return false;
		if (game.getGameTurnTick() < this.arrivalTick) return true;
		final float sourceX = caster.getX();
		final float sourceY = caster.getY();
		final float targetX = target.getX();
		final float targetY = target.getY();
		final var travelers = new ArrayList<CUnit>();
		if (this.unitLimit > 1) {
			game.getWorldCollision().enumUnitsInRange(sourceX, sourceY, this.radius, unit -> {
				if (unit != caster && unit.getPlayerIndex() == caster.getPlayerIndex() && canTravel(unit)) travelers.add(unit);
				return travelers.size() >= this.unitLimit - 1;
			});
		}
		caster.setPointAndCheckUnstuck(targetX, targetY, game);
		for (final CUnit unit : travelers) {
			final float offsetX = this.cluster ? 0 : unit.getX() - sourceX;
			final float offsetY = this.cluster ? 0 : unit.getY() - sourceY;
			unit.order(game, null, false);
			unit.setPointAndCheckUnstuck(targetX + offsetX, targetY + offsetY, game);
		}
		return false;
	}

	@Override
	public void doChannelEnd(final CSimulation game, final CUnit caster, final AbilityTarget target, final boolean interrupted) {
		caster.removeStateModBuff(this.protection);
		caster.computeUnitState(game, StateModBuffType.INVULNERABLE);
		this.departureArt.remove();
		this.arrivalArt.remove();
		this.departureArt = null;
		this.arrivalArt = null;
		this.townHall = null;
	}

	private CUnit nearbyTownHall(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		return game.getUnitsIncludingPending().stream().filter(hall -> eligibleTownHall(game, caster, hall))
				.filter(hall -> hall.distanceSquaredNoCollision(target) <= this.radius * this.radius)
				.min(Comparator.comparingDouble(hall -> hall.distanceSquaredNoCollision(target))).orElse(null);
	}

	private static CUnit defaultTownHall(final CSimulation game, final CUnit caster) {
		return game.getUnitsIncludingPending().stream().filter(hall -> hall.getPlayerIndex() == caster.getPlayerIndex()
				&& eligibleTownHall(game, caster, hall)).max(Comparator.comparingInt(CAbilityItemTownPortal::townHallTier)
						.thenComparingDouble(hall -> -caster.distanceSquaredNoCollision(hall))).orElse(null);
	}

	private static boolean eligibleTownHall(final CSimulation game, final CUnit caster, final CUnit hall) {
		if (!game.containsUnit(hall) || hall.isDead() || hall.isHidden() || hall.isConstructing()
				|| !hall.isUnitType(CUnitTypeJass.TOWNHALL)) return false;
		final int owner = hall.getPlayerIndex();
		final var player = game.getPlayer(caster.getPlayerIndex());
		return owner == player.getId() || player.hasAlliance(owner, CAllianceType.PASSIVE)
				&& player.hasAlliance(owner, CAllianceType.HELP_REQUEST);
	}

	private static int townHallTier(final CUnit hall) {
		return switch (hall.getTypeId().toString()) {
		case "hcas", "ofrt", "unp2", "etoe" -> 3;
		case "hkee", "ostr", "unp1", "etoa" -> 2;
		default -> 1;
		};
	}

	static boolean canTravel(final CUnit unit) {
		if (unit.isDead() || unit.isHidden() || unit.isBuilding() || unit.isMovementDisabled() || unit.isWebbed()
				|| unit.getCurrentBehavior() != null && !unit.getCurrentBehavior().acceptsOrders()) return false;
		final var behavior = unit.getCurrentBehavior();
		if (behavior == null) return true;
		final int order = behavior.getHighlightOrderId();
		return order != OrderIds.harvest && order != OrderIds.returnresources && order != OrderIds.wispharvest
				&& order != OrderIds.acolyteharvest && !(behavior instanceof CBehaviorHumanBuild
						|| behavior instanceof CBehaviorOrcBuild || behavior instanceof CBehaviorUndeadBuild
						|| behavior instanceof CBehaviorHumanRepair || behavior instanceof CBehaviorRepair);
	}
}

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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityItemRecall extends CAbilityUnitOrPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIrt");
	private final int baseOrderId;
	private int unitLimit;
	private boolean cluster;
	private float radius;
	private int delayTicks;
	private int teleportTick;

	public CAbilityItemRecall(final int handleId, final War3ID alias) {
		this(handleId, alias, OrderIds.itemrecall);
	}

	public CAbilityItemRecall(final int handleId, final War3ID alias, final int baseOrderId) {
		super(handleId, alias);
		this.baseOrderId = baseOrderId;
	}

	@Override
	public int getBaseOrderId() {
		return this.baseOrderId;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.unitLimit = Math.max(0, definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0));
		if (this.baseOrderId == OrderIds.darksummoning) {
			this.delayTicks = (int) Math.ceil(definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0)
					/ WarsmashConstants.SIMULATION_STEP_TIME);
			this.cluster = definition.getFieldAsBoolean(AbilityFields.DATA_C + level, 0);
		}
		else this.cluster = definition.getFieldAsBoolean(AbilityFields.DATA_B + level, 0);
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.delayTicks > 0) {
			this.teleportTick = game.getGameTurnTick() + this.delayTicks;
			return true;
		}
		teleportUnits(game, caster, target);
		return false;
	}

	@Override
	public boolean doChannelTick(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (game.getGameTurnTick() < this.teleportTick) return true;
		teleportUnits(game, caster, target);
		return false;
	}

	private void teleportUnits(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var candidates = new ArrayList<CUnit>();
		final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
		final float sourceX = target.getX();
		final float sourceY = target.getY();
		game.getWorldCollision().enumUnitsInRange(sourceX, sourceY, this.radius, unit -> {
			if (unit != caster && unit.getPlayerIndex() == caster.getPlayerIndex()
					&& CAbilityItemTownPortal.canTravel(unit)
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(), receiver.reset())) {
				candidates.add(unit);
			}
			return false;
		});
		candidates.sort(Comparator.comparingDouble((CUnit unit) -> unit.distanceSquaredNoCollision(sourceX, sourceY))
				.thenComparingInt(CUnit::getHandleId));
		final float destinationX = caster.getX();
		final float destinationY = caster.getY();
		for (final CUnit unit : candidates.subList(0, Math.min(this.unitLimit, candidates.size()))) {
			final float offsetX = this.cluster ? 0 : unit.getX() - sourceX;
			final float offsetY = this.cluster ? 0 : unit.getY() - sourceY;
			final boolean darkSummoning = this.baseOrderId == OrderIds.darksummoning;
			game.spawnSpellEffectOnPoint(unit.getX(), unit.getY(), 0, getAlias(),
					darkSummoning ? CEffectType.EFFECT : CEffectType.SPECIAL, 0).remove();
			unit.order(game, OrderIds.stop, null);
			unit.setPointAndCheckUnstuck(destinationX + offsetX, destinationY + offsetY, game);
			if (darkSummoning) game.createTemporarySpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
			else game.spawnSpellEffectOnPoint(unit.getX(), unit.getY(), 0, getAlias(), CEffectType.SPECIAL, 0).remove();
		}
	}
}

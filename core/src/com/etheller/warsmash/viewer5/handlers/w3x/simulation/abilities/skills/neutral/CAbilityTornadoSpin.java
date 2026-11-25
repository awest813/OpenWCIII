package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf.CAbilityCyclone;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityTornadoSpin extends CAbilityPassiveSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Btsp");
	private static final float SAME_TARGET_INTERVAL = 22f;
	private final Map<Integer, Integer> nextLiftByUnit = new HashMap<>();
	private War3ID buffId;
	private int pulseTicks;
	private int nextPulse;

	public CAbilityTornadoSpin(final int handleId, final War3ID alias) {
		super(handleId, War3ID.fromString("Atsp"), alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final String buff = definition.getField(AbilityFields.BUFF + level);
		this.buffId = buff.length() == 4 ? War3ID.fromString(buff) : DEFAULT_BUFF;
		final float interval = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.pulseTicks = interval > 0 ? ticks(interval) : ticks(3f);
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit tornado) {
		this.nextPulse = game.getGameTurnTick() + this.pulseTicks;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit tornado) {
		final int tick = game.getGameTurnTick();
		if (tick < this.nextPulse || getAreaOfEffect() <= 0) return;
		this.nextPulse = tick + this.pulseTicks;
		this.nextLiftByUnit.values().removeIf(nextLift -> tick >= nextLift);
		final var targets = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(tornado.getX(), tornado.getY(), getAreaOfEffect(), unit -> {
			if (unit != tornado && !unit.isBuilding() && !unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& getDurationForTarget(unit) > 0 && !this.nextLiftByUnit.containsKey(unit.getHandleId())
					&& unit.canBeTargetedBy(game, tornado, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				targets.add(unit);
			}
			return false;
		});
		if (targets.isEmpty()) return;
		targets.sort(Comparator.comparingInt(CUnit::getHandleId));
		final CUnit target = targets.get(game.getSeededRandom().nextInt(targets.size()));
		if (CAbilityCyclone.lift(game, target, this.buffId, getDurationForTarget(target), true)) {
			this.nextLiftByUnit.put(target.getHandleId(), tick + ticks(SAME_TARGET_INTERVAL));
		}
	}

	private static int ticks(final float seconds) {
		return (int) Math.ceil(seconds / WarsmashConstants.SIMULATION_STEP_TIME);
	}
}

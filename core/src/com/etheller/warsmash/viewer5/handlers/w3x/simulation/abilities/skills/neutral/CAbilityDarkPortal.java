package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.Arrays;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityDarkPortal extends CAbilityPointTargetSpellBase {
	private List<War3ID> demonTypes;
	private int minimumCount;
	private int maximumCount;

	public CAbilityDarkPortal(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.darkportal;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.demonTypes = Arrays.stream(definition.readSLKTag(AbilityFields.DATA_A + level).split(","))
				.map(String::strip).filter(id -> id.length() == 4).map(War3ID::fromString).toList();
		this.minimumCount = Math.max(0, definition.readSLKTagInt(AbilityFields.DATA_B + level));
		this.maximumCount = Math.max(this.minimumCount, definition.readSLKTagInt(AbilityFields.DATA_C + level));
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final List<War3ID> available = this.demonTypes.stream().filter(id -> game.getUnitData().getUnitType(id) != null).toList();
		if (available.isEmpty() || this.maximumCount <= 0) return false;
		final var random = game.getSeededRandom();
		final int count = this.minimumCount + random.nextInt(this.maximumCount - this.minimumCount + 1);
		final float x = target.getX();
		final float y = target.getY();
		final float facing = caster.getFacing();
		final int owner = caster.getPlayerIndex();
		final var portal = game.spawnSpellEffectOnPoint(x, y, facing, getAlias(), CEffectType.EFFECT, 0);
		final var open = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				for (int index = 0; index < count; index++) {
					final War3ID type = available.get(random.nextInt(available.size()));
					final CUnit demon = simulation.createUnitSimple(type, owner, x, y, facing);
					demon.addClassification(CUnitClassification.SUMMONED);
					caster.fireSummonEvents(simulation, demon);
				}
				portal.remove();
			}
		};
		open.setTimeoutTime(getDuration());
		open.start(game);
		return false;
	}
}

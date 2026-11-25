package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.nightelf;

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
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorCategory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityTaunt extends CAbilityNoTargetSpellBase {

	private static final int MAX_TARGETS = 10;
	private float radius;

	public CAbilityTaunt(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.taunt;
	}

	@Override
	public boolean isPhysical() {
		return true;
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.radius = ability.getFieldAsFloat(AbilityFields.AREA + level, 0);
		if (CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final List<CUnit> nearby = new ArrayList<>();
		final var owner = game.getPlayer(caster.getPlayerIndex());
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
			final var behavior = unit.getCurrentBehavior();
			if (unit.getPlayerIndex() != caster.getPlayerIndex() && !owner.hasAlliance(unit.getPlayerIndex(), CAllianceType.PASSIVE)
					&& !unit.isDead() && !unit.isPaused()
					&& (behavior == null || (behavior.getBehaviorCategory() != CBehaviorCategory.SPELL
							&& behavior.getHighlightOrderId() != OrderIds.repair))
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				nearby.add(unit);
			}
			return false;
		});
		nearby.sort(Comparator.comparingDouble((CUnit unit) -> caster.distanceSquaredNoCollision(unit))
				.thenComparingInt(CUnit::getHandleId));
		int affected = 0;
		for (final CUnit unit : nearby) {
			if (unit.orderAutomatically(game, OrderIds.attack, caster) && ++affected == MAX_TARGETS) {
				break;
			}
		}
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}
}

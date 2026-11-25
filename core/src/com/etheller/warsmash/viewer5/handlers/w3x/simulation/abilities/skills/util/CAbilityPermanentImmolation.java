package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util;

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

/** Damage emitted periodically by the Infernal and other units with permanent immolation. */
public final class CAbilityPermanentImmolation extends CAbilityPassiveSpellBase {
	private float damage;
	private int intervalTicks;
	private int nextPulseTick;

	public CAbilityPermanentImmolation(final int handleId, final War3ID alias) {
		super(handleId, alias, alias);
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.damage = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.intervalTicks = Math.max(1, (int) StrictMath.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME));
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		this.nextPulseTick = game.getGameTurnTick() + this.intervalTicks;
	}

	@Override
	public void onTick(final CSimulation game, final CUnit caster) {
		final int tick = game.getGameTurnTick();
		if (isDisabled() || caster.isHidden() || tick < this.nextPulseTick) return;
		this.nextPulseTick = tick + this.intervalTicks;
		final var affected = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), getAreaOfEffect(), unit -> {
			if (!unit.isDead() && !unit.isHidden() && !unit.isInvulnerable()
					&& !unit.isUnitAlly(game.getPlayer(caster.getPlayerIndex()))
					&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				affected.add(unit);
			}
			return false;
		});
		for (final CUnit unit : affected) {
			unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.FIRE, null, this.damage);
		}
	}
}

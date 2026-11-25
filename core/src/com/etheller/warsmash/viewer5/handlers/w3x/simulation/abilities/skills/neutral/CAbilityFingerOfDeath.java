package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityFingerOfDeath extends CAbilityTargetSpellBase {
	private float graphicDelay;
	private float graphicDuration;
	private float damage;

	public CAbilityFingerOfDeath(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.fingerofdeath;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.graphicDelay = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.graphicDuration = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damage = definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level).isEmpty()) {
			final var targets = CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED));
			targets.addAll(getTargetsAllowed());
			setTargetsAllowed(targets);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CWidget widget) || widget instanceof CUnit unit
				&& !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final CTimer graphicTimer = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				playImpactArt(simulation, widget);
			}
		};
		if (this.graphicDelay > 0) {
			graphicTimer.setTimeoutTime(this.graphicDelay);
			graphicTimer.start(game);
		}
		else {
			graphicTimer.onFire(game);
		}
		if (widget instanceof CUnit unit) {
			final boolean previouslyExploded = unit.isExplodesOnDeath();
			unit.setExplodesOnDeath(true);
			unit.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, this.damage);
			if (!unit.isDead()) unit.setExplodesOnDeath(previouslyExploded);
		}
		else {
			widget.damage(game, caster, false, true, CAttackType.SPELLS, CDamageType.MAGIC, null, this.damage);
		}
		return false;
	}

	private void playImpactArt(final CSimulation game, final CWidget target) {
		if (target instanceof CUnit unit && !unit.isDead() && !unit.isHidden() && this.graphicDuration > 0) {
			final var art = game.createPersistentSpellEffectOnUnit(unit, getAlias(), CEffectType.TARGET);
			final CTimer cleanup = new CTimer() {
				@Override
				public void onFire(final CSimulation simulation) {
					art.remove();
				}
			};
			cleanup.setTimeoutTime(this.graphicDuration);
			cleanup.start(game);
		}
		else {
			game.spawnTemporarySpellEffectOnPoint(target.getX(), target.getY(), 0, getAlias(), CEffectType.TARGET, 0);
		}
	}
}

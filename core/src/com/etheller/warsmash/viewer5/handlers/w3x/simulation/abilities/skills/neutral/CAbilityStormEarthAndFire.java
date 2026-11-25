package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityStormEarthAndFire extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("ANef");
	private static final War3ID TIMED_LIFE = War3ID.fromString("BNef");
	private List<War3ID> elementalTypes = List.of();
	private float spread;
	private Split split;

	public CAbilityStormEarthAndFire(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.elementalfury; }

	@Override
	public void populateData(final GameObject data, final int level) {
		this.elementalTypes = data.getFieldAsList(AbilityFields.DATA_A + level).stream()
				.filter(id -> id.length() == 4).map(War3ID::fromString).toList();
		this.spread = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.split != null || this.elementalTypes.isEmpty()
				|| this.elementalTypes.stream().anyMatch(id -> game.getUnitData().getUnitType(id) == null)) return false;
		final Split active = new Split(game, caster);
		this.split = active;
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.SPECIAL);
		caster.setInvulnerable(true);
		caster.setPaused(true);
		caster.setHidden(true);
		for (int index = 0; index < this.elementalTypes.size(); index++) {
			final double angle = Math.toRadians(caster.getFacing()) + (index * 2 * Math.PI / this.elementalTypes.size());
			final float x = caster.getX() + this.spread * (float) Math.cos(angle);
			final float y = caster.getY() + this.spread * (float) Math.sin(angle);
			final CUnit form = game.createUnitSimple(this.elementalTypes.get(index), caster.getPlayerIndex(), x, y, caster.getFacing());
			form.addClassification(CUnitClassification.SUMMONED);
			form.setPointAndCheckUnstuck(x, y, game);
			form.add(game, new ElementalDuration(game.getHandleIdAllocator().createId(), getDuration()));
			active.forms.add(form);
			caster.fireSummonEvents(game, form);
		}
		game.unitPreferredSelectionReplacement(caster, active.forms.get(0));
		active.setRepeats(true);
		active.setTimeoutTime(WarsmashConstants.SIMULATION_STEP_TIME);
		active.start(game);
		return false;
	}

	@Override public void onRemove(final CSimulation game, final CUnit unit) { if (this.split != null) this.split.finish(game); }
	@Override public void onDeath(final CSimulation game, final CUnit unit) { if (this.split != null) this.split.finish(game); }

	private final class Split extends CTimer {
		private final CUnit caster;
		private final List<CUnit> forms = new ArrayList<>();
		private final boolean invulnerable;
		private final boolean paused;
		private final boolean hidden;
		private final int endTick;

		private Split(final CSimulation game, final CUnit caster) {
			this.caster = caster;
			this.invulnerable = caster.isInvulnerable();
			this.paused = caster.isPaused();
			this.hidden = caster.isHidden();
			this.endTick = game.getGameTurnTick() + (int) Math.ceil(getDuration() / WarsmashConstants.SIMULATION_STEP_TIME);
		}

		private CUnit survivingForm(final CSimulation game) {
			return this.forms.stream().filter(form -> !form.isDead() && game.containsUnit(form)).findFirst().orElse(null);
		}

		@Override
		public void onFire(final CSimulation game) {
			if (CAbilityStormEarthAndFire.this.split != this) return;
			if (game.getGameTurnTick() >= this.endTick || !game.containsUnit(this.caster) || survivingForm(game) == null) finish(game);
		}

		private void finish(final CSimulation game) {
			setRepeats(false);
			pause(game);
			CAbilityStormEarthAndFire.this.split = null;
			final CUnit survivor = survivingForm(game);
			final boolean casterPresent = game.containsUnit(this.caster);
			if (casterPresent) {
				this.caster.setInvulnerable(this.invulnerable);
				this.caster.setPaused(this.paused);
				this.caster.setHidden(this.hidden);
				if (survivor != null && !this.caster.isDead()) game.unitPreferredSelectionReplacement(survivor, this.caster);
			}
			for (final CUnit form : this.forms) {
				if (game.containsUnit(form)) game.removeUnit(form);
			}
			if (casterPresent && !this.caster.isDead()) {
				if (survivor == null) this.caster.kill(game);
				else {
					this.caster.setPointAndCheckUnstuck(survivor.getX(), survivor.getY(), game);
					game.unitRepositioned(this.caster);
				}
			}
		}
	}

	private static final class ElementalDuration extends CBuffTimed {
		private ElementalDuration(final int handleId, final float duration) { super(handleId, TIMED_LIFE, TIMED_LIFE, duration); }
		@Override protected void onBuffAdd(final CSimulation game, final CUnit unit) { }
		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) { }
		@Override public boolean isTimedLifeBar() { return true; }
	}
}

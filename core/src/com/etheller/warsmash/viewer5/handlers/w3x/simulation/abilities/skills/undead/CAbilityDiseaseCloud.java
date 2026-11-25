package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

/** Disease Cloud from Abominations, plague wards and Meat Wagon impacts. */
public final class CAbilityDiseaseCloud extends CAbilityPassiveSpellBase {
	private static final War3ID AURA = War3ID.fromString("Aapl");
	private static final War3ID TOSS = War3ID.fromString("Apts");
	private static final int PULSE_TICKS = Math.round(1 / WarsmashConstants.SIMULATION_STEP_TIME);

	private final boolean projectile;
	private List<CUnitTypeRequirement> requirements = List.of();
	private War3ID cloudUnit;
	private float diseaseDuration;
	private float damagePerSecond;
	private float cloudDuration;
	private int nextPulse;

	public CAbilityDiseaseCloud(final int handleId, final War3ID alias, final boolean projectile) {
		super(handleId, projectile ? TOSS : AURA, alias);
		this.projectile = projectile;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.diseaseDuration = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.damagePerSecond = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.cloudDuration = this.projectile ? getDuration() : definition.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		final String unitId = definition.getField(AbilityFields.UNIT_ID + level);
		this.cloudUnit = unitId.length() == 4 ? War3ID.fromString(unitId) : null;
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		if (getTargetsAllowed().isEmpty()) {
			setTargetsAllowed(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final var player = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void onAddDisabled(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		setIconShowing(this.projectile || this.cloudDuration > 0);
		this.nextPulse = game.getGameTurnTick();
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		setIconShowing(false);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit source) {
		if (this.projectile || isDisabled() || source.isIllusion() || source.isHidden()
				|| this.diseaseDuration <= 0 || game.getGameTurnTick() < this.nextPulse) return;
		this.nextPulse = game.getGameTurnTick() + PULSE_TICKS;
		final var targets = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(source.getX(), source.getY(), getAreaOfEffect(), unit -> {
			if (!unit.isDead() && !unit.isHidden() && !unit.isInvulnerable() && !unit.isUnitType(CUnitTypeJass.UNDEAD)
					&& unit.canBeTargetedBy(game, source, false, getTargetsAllowed(),
							BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) targets.add(unit);
			return false;
		});
		for (final CUnit target : targets) {
			final Disease existing = target.getFirstAbilityOfType(Disease.class);
			if (existing == null) {
				target.add(game, new Disease(game.getHandleIdAllocator().createId(), getAlias(), this.diseaseDuration,
						this.damagePerSecond));
			}
			else existing.extend(game);
		}
	}

	@Override
	public void onDeath(final CSimulation game, final CUnit unit) {
		if (!this.projectile) spawnCloud(game, unit, unit.getX(), unit.getY());
	}

	public void onProjectileImpact(final CSimulation game, final CUnit source, final float x, final float y) {
		if (this.projectile) spawnCloud(game, source, x, y);
	}

	private void spawnCloud(final CSimulation game, final CUnit source, final float x, final float y) {
		if (this.cloudUnit == null || this.cloudDuration <= 0 || isDisabled() || source.isIllusion()
				|| !isRequirementsMet(game, source)) return;
		final CUnit cloud = game.createUnitSimple(this.cloudUnit, source.getPlayerIndex(), x, y, source.getFacing());
		if (cloud == null) return;
		cloud.addClassification(CUnitClassification.SUMMONED);
		cloud.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), War3ID.fromString("BTLF"),
				this.cloudDuration, false));
	}

	private static final class Disease extends CBuffTimed {
		private final float damage;
		private int nextPulse;

		Disease(final int handleId, final War3ID alias, final float duration, final float damage) {
			super(handleId, AURA, alias, duration);
			this.damage = damage;
		}

		void extend(final CSimulation game) {
			refreshDuration(game);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.nextPulse = game.getGameTurnTick() + PULSE_TICKS;
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		}

		@Override
		public void onTick(final CSimulation game, final CUnit unit) {
			if (game.getGameTurnTick() >= this.nextPulse) {
				this.nextPulse += PULSE_TICKS;
				if (!unit.isInvulnerable() && unit.getLife() > 2 && !unit.isUnitType(CUnitTypeJass.UNDEAD)) {
					unit.setLife(game, Math.max(2, unit.getLife() - this.damage));
				}
			}
			super.onTick(game, unit);
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}

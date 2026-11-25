package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.blademaster;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityMirrorImage extends CAbilityNoTargetSpellBase {
	private static final War3ID DEFAULT_IMAGE_BUFF = War3ID.fromString("BOmi");
	private final List<CUnit> images = new ArrayList<>();
	private int imageCount;
	private float damageDealt;
	private float damageTaken;
	private float splitDelay;
	private War3ID imageBuff;

	public CAbilityMirrorImage(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.mirrorimage;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.imageCount = data.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.damageDealt = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.damageTaken = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.splitDelay = data.getFieldAsFloat(AbilityFields.DATA_D + level, 0);
		final War3ID authoredBuff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.imageBuff = War3ID.NONE.equals(authoredBuff) ? DEFAULT_IMAGE_BUFF : authoredBuff;
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		for (final CUnit image : this.images) {
			if (!image.isDead() && !image.isHidden()) image.kill(game);
		}
		this.images.clear();
		for (final var ability : List.copyOf(caster.getAbilities())) {
			if (ability instanceof CBuff buff && !buff.isTimedLifeBar()) caster.remove(game, buff);
		}
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.SPECIAL);
		final boolean wasHidden = caster.isHidden();
		final boolean wasInvulnerable = caster.isInvulnerable();
		final float originX = caster.getX();
		final float originY = caster.getY();
		final int count = this.imageCount;
		final float dealt = this.damageDealt;
		final float taken = this.damageTaken;
		final float duration = getDuration();
		final float spread = getCastRange();
		final War3ID buff = this.imageBuff;
		caster.setHidden(true);
		caster.setInvulnerable(true);
		final var split = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				if (simulation.getUnit(caster.getHandleId()) != caster) return;
				caster.setInvulnerable(wasInvulnerable);
				caster.setHidden(wasHidden);
				if (!caster.isDead()) placeImages(simulation, caster, originX, originY, count, spread, dealt, taken, duration, buff);
			}
		};
		split.setTimeoutTime(this.splitDelay);
		split.start(game);
		return false;
	}

	private void placeImages(final CSimulation game, final CUnit caster, final float originX, final float originY,
			final int count, final float spread, final float dealt, final float taken, final float duration,
			final War3ID buff) {
		if (count < 1) return;
		final int slots = count + 1;
		final int originalSlot = game.getSeededRandom().nextInt(slots);
		final double start = game.getSeededRandom().nextDouble() * StrictMath.PI * 2;
		final var grid = game.getPathingGrid();
		for (int slot = 0; slot < slots; slot++) {
			final double angle = start + slot * StrictMath.PI * 2 / slots;
			float x = originX + (float) (spread * StrictMath.cos(angle));
			float y = originY + (float) (spread * StrictMath.sin(angle));
			if (!grid.contains(x, y) || !grid.isPathable(x, y, caster.getMovementType(), caster.getUnitType().getCollisionSize())) {
				x = originX;
				y = originY;
			}
			if (slot == originalSlot) {
				caster.setPointAndCheckUnstuck(x, y, game);
			}
			else {
				final CUnit image = game.createIllusion(caster, x, y, dealt, taken, duration, buff);
				if (image != null) this.images.add(image);
			}
		}
	}
}

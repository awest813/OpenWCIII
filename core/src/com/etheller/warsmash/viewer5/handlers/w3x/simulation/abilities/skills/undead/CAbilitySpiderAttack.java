package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilitySpiderAttack extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Aspa");
	private static final War3ID DEFAULT_SPIDERLING = War3ID.fromString("Bspa");
	private War3ID spiderling;
	private String missileArt;

	public CAbilitySpiderAttack(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.spiderling = War3ID.NONE.equals(buff) ? DEFAULT_SPIDERLING : buff;
		this.missileArt = definition.getField("Missileart");
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	public String getMissileArt() {
		return isDisabled() || this.missileArt.isBlank() ? null : this.missileArt;
	}

	public void onImpact(final CSimulation game, final CUnit attacker, final CUnit target, final float speed) {
		if (isDisabled() || target.isDead() || target.isHidden() || speed <= 0
				|| !target.canBeTargetedBy(game, attacker, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) return;
		final var attachment = game.createPersistentSpellEffectOnUnit(target, this.spiderling, CEffectType.TARGET);
		final CTimer returnToAttacker = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				attachment.remove();
				if (!attacker.isDead() && !attacker.isHidden() && !target.isDead() && !target.isHidden()) {
					simulation.createProjectile(target, CODE, target.getX(), target.getY(), (float) target.angleTo(attacker),
							speed, true, attacker, CAbilityProjectileListener.DO_NOTHING);
				}
			}
		};
		returnToAttacker.setTimeoutTime(getDurationForTarget(target));
		returnToAttacker.start(game);
	}
}

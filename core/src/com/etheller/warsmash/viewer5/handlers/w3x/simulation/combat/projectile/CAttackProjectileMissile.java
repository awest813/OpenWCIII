package com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.CAbilityDiseaseCloud;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttack;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;

public class CAttackProjectileMissile extends CAttackProjectile {
	protected final CUnitAttackMissile unitAttack;
	protected final int bounceIndex;
	private final float launchX;
	private final float launchY;

	public CAttackProjectileMissile(final float x, final float y, final float speed, final AbilityTarget target,
			final CUnit source, final float damage, final CUnitAttackMissile unitAttack, final int bounceIndex,
			final CUnitAttackListener attackListener) {
		super(x, y, speed, target, source, damage, unitAttack.isProjectileHomingEnabled(), attackListener);
		this.unitAttack = unitAttack;
		this.bounceIndex = bounceIndex;
		this.launchX = x;
		this.launchY = y;
	}

	@Override
	protected void onHitTarget(CSimulation game) {
		CUnit tarU = getTarget().visit(AbilityTargetVisitor.UNIT);
		if (tarU == null || tarU.checkForAttackProjReaction(game, getSource(), this)) {
			this.unitAttack.doProjectileDamage(game, getSource(), getTarget(), this.damage, getX(), getY(),
					this.launchX, this.launchY, this.bounceIndex, this.attackListener);
			final CAbilityDiseaseCloud diseaseCloud = getSource().getFirstAbilityOfType(CAbilityDiseaseCloud.class);
			if (diseaseCloud != null) diseaseCloud.onProjectileImpact(game, getSource(), getX(), getY());
		}
		
	}

	public CUnitAttack getUnitAttack() {
		return this.unitAttack;
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.lich;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameplayConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;

public final class CBuffFrost extends CBuffTimed {
	static final War3ID DEFAULT_BUFF = War3ID.fromString("Bfro");
	private final NonStackingStatBuff movement;
	private final NonStackingStatBuff attack;

	private CBuffFrost(final int handleId, final War3ID alias, final float duration, final CGameplayConstants constants) {
		super(handleId, DEFAULT_BUFF, alias, duration);
		this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "frost", -constants.getFrostMoveSpeedDecrease());
		this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "frost", -constants.getFrostAttackSpeedDecrease());
	}

	public static void apply(final CSimulation game, final CUnit target, final float duration) {
		apply(game, target, DEFAULT_BUFF, duration);
	}

	static void apply(final CSimulation game, final CUnit target, final War3ID alias, final float duration) {
		if (target.isDead() || duration <= 0) return;
		final CBuffFrost previous = target.getFirstAbilityOfType(CBuffFrost.class);
		if (previous != null) {
			if (previous.getDurationRemaining(game, target) >= duration) return;
			target.remove(game, previous);
		}
		target.add(game, new CBuffFrost(game.getHandleIdAllocator().createId(), alias, duration, game.getGameplayConstants()));
	}

	@Override
	protected void onBuffAdd(final CSimulation game, final CUnit unit) {
		unit.addNonStackingStatBuff(this.movement);
		unit.addNonStackingStatBuff(this.attack);
	}

	@Override
	protected void onBuffRemove(final CSimulation game, final CUnit unit) {
		unit.removeNonStackingStatBuff(this.movement);
		unit.removeNonStackingStatBuff(this.attack);
	}

	@Override
	public boolean isTimedLifeBar() {
		return false;
	}
}

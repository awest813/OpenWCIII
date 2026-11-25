package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;

public interface CBuff extends CAliasedLevelingAbility {
	enum DispelPolarity {
		UNCLASSIFIED, BENEFICIAL, HARMFUL
	}

	float getDurationRemaining(CSimulation game, CUnit unit);

	float getDurationMax();

	boolean isTimedLifeBar();

	default boolean isDispellable() {
		return false;
	}

	default DispelPolarity getDispelPolarity() {
		return DispelPolarity.UNCLASSIFIED;
	}

	default boolean isHeroSpellBuff() {
		return false;
	}

	default boolean ticksWhilePaused() {
		return false;
	}
}

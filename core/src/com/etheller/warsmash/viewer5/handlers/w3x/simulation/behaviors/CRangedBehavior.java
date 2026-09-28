package com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;

public interface CRangedBehavior extends CBehavior {
	boolean isWithinRange(final CSimulation simulation);

	void endMove(CSimulation game, boolean interrupted);

	/** Called when movement exhausts its route or gives up, not for combat interruptions. */
	default void onMoveGiveUp(final CSimulation game) {
		endMove(game, true);
	}
	
	AbilityTarget getTarget();
}

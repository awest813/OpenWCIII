package com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;

public interface COrder {
	int getAbilityHandleId();

	int getOrderId();

	CBehavior begin(final CSimulation game, CUnit caster);

	AbilityTarget getTarget(CSimulation game);

	boolean isQueued();

	void fireEvents(CSimulation game, CUnit unit);
}

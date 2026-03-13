package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.condition;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABCondition;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.iterstructs.UnitAndRange;

public class ABConditionMatchingUnitExistsInRangeOfUnit implements ABCondition {
	private static final Rectangle recycleRect = new Rectangle();

	private ABUnitCallback originUnit;
	private ABFloatCallback range;
	private List<ABCondition> conditions;

	// ⚡ Bolt Optimization: Pool the enum function to avoid allocation while supporting re-entrancy
	private final Pool<MatchingUnitExistsEnum> enumFunctionPool = new Pool<MatchingUnitExistsEnum>() {
		@Override
		protected MatchingUnitExistsEnum newObject() {
			return new MatchingUnitExistsEnum();
		}
	};

	@Override
	public boolean evaluate(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = originUnit.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		MatchingUnitExistsEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect, enumFunction.reset(game, caster, localStore, castId, originUnitTarget, rangeVal, conditions));
			return enumFunction.getUnitAndRange().getUnit() != null;
		} finally {
			enumFunction.clear();
			this.enumFunctionPool.free(enumFunction);
		}
	}

	private static final class MatchingUnitExistsEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit caster;
		private Map<String, Object> localStore;
		private int castId;
		private CUnit originUnitTarget;
		private float rangeVal;
		private List<ABCondition> conditions;

		private final UnitAndRange ur = new UnitAndRange();

		public MatchingUnitExistsEnum reset(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
				final int castId, final CUnit originUnitTarget, final float rangeVal, final List<ABCondition> conditions) {
			this.game = game;
			this.caster = caster;
			this.localStore = localStore;
			this.castId = castId;
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.conditions = conditions;
			this.ur.setUnit(null);
			this.ur.setRange(0);
			return this;
		}

		public void clear() {
			this.game = null;
			this.caster = null;
			this.localStore = null;
			this.originUnitTarget = null;
			this.conditions = null;
			this.ur.setUnit(null);
		}

		public UnitAndRange getUnitAndRange() {
			return this.ur;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				if (this.ur.getUnit() == null) {
					if (this.conditions != null) {
						boolean result = true;
						this.localStore.put(ABLocalStoreKeys.MATCHINGUNIT + this.castId, enumUnit);
						for (ABCondition condition : this.conditions) {
							result = result && condition.evaluate(this.game, this.caster, this.localStore, this.castId);
						}
						this.localStore.remove(ABLocalStoreKeys.MATCHINGUNIT + this.castId);
						if (result) {
							this.ur.setUnit(enumUnit);
						}
					} else {
						this.ur.setUnit(enumUnit);
					}
				}
			}
			return false;
		}
	}

}

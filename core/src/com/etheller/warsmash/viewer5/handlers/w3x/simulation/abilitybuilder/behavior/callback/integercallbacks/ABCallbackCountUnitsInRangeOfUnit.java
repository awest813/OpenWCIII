package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.integercallbacks;

import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;

public class ABCallbackCountUnitsInRangeOfUnit extends ABIntegerCallback {

	private static final Rectangle recycleRect = new Rectangle();
	private ABUnitCallback unit;
	private ABFloatCallback range;
	
	// ⚡ Bolt Optimization: Pool the enum function to avoid allocation while supporting re-entrancy
	private final Pool<CountUnitsEnum> enumFunctionPool = new Pool<CountUnitsEnum>() {
		@Override
		protected CountUnitsEnum newObject() {
			return new CountUnitsEnum();
		}
	};
	
	@Override
	public Integer callback(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit originUnitTarget = unit.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		CountUnitsEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect, enumFunction.reset(originUnitTarget, rangeVal));
			return enumFunction.getCount();
		} finally {
			enumFunction.clear();
			this.enumFunctionPool.free(enumFunction);
		}
	}

	private static final class CountUnitsEnum implements CUnitEnumFunction {
		private CUnit originUnitTarget;
		private float rangeVal;
		private int count = 0;

		public CountUnitsEnum reset(final CUnit originUnitTarget, final float rangeVal) {
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		public void clear() {
			this.originUnitTarget = null;
		}

		public int getCount() {
			return this.count;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				this.count++;
			}
			return false;
		}
	}

}

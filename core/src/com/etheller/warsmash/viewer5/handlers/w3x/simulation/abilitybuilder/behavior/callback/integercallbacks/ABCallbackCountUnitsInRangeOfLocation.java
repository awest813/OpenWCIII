package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.integercallbacks;

import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.locationcallbacks.ABLocationCallback;

public class ABCallbackCountUnitsInRangeOfLocation extends ABIntegerCallback {

	private static final Rectangle recycleRect = new Rectangle();
	private ABLocationCallback location;
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
		AbilityPointTarget origin = location.callback(game, caster, localStore, castId);
		Float rangeVal = range.callback(game, caster, localStore, castId);
		
		recycleRect.set(origin.getX() - rangeVal, origin.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);

		CountUnitsEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect, enumFunction.reset(origin, rangeVal));
			return enumFunction.getCount();
		} finally {
			enumFunction.clear();
			this.enumFunctionPool.free(enumFunction);
		}
	}

	private static final class CountUnitsEnum implements CUnitEnumFunction {
		private AbilityPointTarget origin;
		private float rangeVal;
		private int count = 0;

		public CountUnitsEnum reset(final AbilityPointTarget origin, final float rangeVal) {
			this.origin = origin;
			this.rangeVal = rangeVal;
			this.count = 0;
			return this;
		}

		public void clear() {
			this.origin = null;
		}

		public int getCount() {
			return this.count;
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (enumUnit.canReach(this.origin, this.rangeVal)) {
				this.count++;
			}
			return false;
		}
	}

}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.action.structural;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;
import com.etheller.warsmash.parsers.jass.JassTextGenerator;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorCallStmt;
import com.etheller.warsmash.parsers.jass.JassTextGeneratorStmt;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.floatcallbacks.ABFloatCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;
import com.badlogic.gdx.utils.Pool;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABLocalStoreKeys;

public class ABActionIterateUnitsInRangeOfUnit implements ABAction {

	private static final Rectangle recycleRect = new Rectangle();
	private List<ABAction> iterationActions;

	private ABUnitCallback originUnit;
	private ABFloatCallback range;

	// ⚡ Bolt Optimization: Use an object pool for the CUnitEnumFunction to prevent allocating an anonymous class
	// on every tick during spatial queries, while avoiding re-entrancy bugs. This reduces GC pressure safely.
	private final Pool<IterateUnitsInRangeEnum> enumFunctionPool = new Pool<IterateUnitsInRangeEnum>() {
		@Override
		protected IterateUnitsInRangeEnum newObject() {
			return new IterateUnitsInRangeEnum();
		}
	};

	@Override
	public void runAction(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
			final int castId) {
		final CUnit originUnitTarget = this.originUnit.callback(game, caster, localStore, castId);
		final Float rangeValWrapper = this.range.callback(game, caster, localStore, castId);
		final float rangeVal = rangeValWrapper == null ? 0 : rangeValWrapper;

		recycleRect.set(originUnitTarget.getX() - rangeVal, originUnitTarget.getY() - rangeVal, rangeVal * 2,
				rangeVal * 2);
		IterateUnitsInRangeEnum enumFunction = this.enumFunctionPool.obtain();
		try {
			game.getWorldCollision().enumUnitsInRect(recycleRect,
					enumFunction.reset(game, caster, localStore, castId, originUnitTarget, rangeVal));
		} finally {
			enumFunction.clear(); // Clear references to prevent memory leaks even if exception occurs
			this.enumFunctionPool.free(enumFunction);
		}

		localStore.remove(ABLocalStoreKeys.ENUMUNIT + castId);
	}

	private final class IterateUnitsInRangeEnum implements CUnitEnumFunction {
		private CSimulation game;
		private CUnit caster;
		private Map<String, Object> localStore;
		private int castId;
		private CUnit originUnitTarget;
		private float rangeVal;

		public IterateUnitsInRangeEnum reset(final CSimulation game, final CUnit caster, final Map<String, Object> localStore,
				final int castId, final CUnit originUnitTarget, final float rangeVal) {
			this.game = game;
			this.caster = caster;
			this.localStore = localStore;
			this.castId = castId;
			this.originUnitTarget = originUnitTarget;
			this.rangeVal = rangeVal;
			return this;
		}

		public void clear() {
			this.game = null;
			this.caster = null;
			this.localStore = null;
			this.originUnitTarget = null;
			// Primitive fields like rangeVal and castId do not need to be cleared as they don't hold references to objects
		}

		@Override
		public boolean call(final CUnit enumUnit) {
			if (this.originUnitTarget.canReach(enumUnit, this.rangeVal)) {
				this.localStore.put(ABLocalStoreKeys.ENUMUNIT + this.castId, enumUnit);
				for (final ABAction iterationAction : ABActionIterateUnitsInRangeOfUnit.this.iterationActions) {
					iterationAction.runAction(this.game, this.caster, this.localStore, this.castId);
				}
			}
			return false;
		}
	}

	@Override
	public void generateJassEquivalent(int indent, JassTextGenerator jassTextGenerator) {
		final List<JassTextGeneratorStmt> modifiedActionList = new ArrayList<>(this.iterationActions);
		modifiedActionList.add(0, new JassTextGeneratorCallStmt() {
			@Override
			public String generateJassEquivalent(final JassTextGenerator jassTextGenerator) {
				return "SetLocalStoreUnitHandle(" + jassTextGenerator.getTriggerLocalStore()
						+ ", AB_LOCAL_STORE_KEY_ENUMUNIT + I2S(" + jassTextGenerator.getCastId() + "), "
						+ "GetFilterUnit()" + ") // filter unit used intentionally as enum";
			}
		});
		modifiedActionList.add(new JassTextGeneratorStmt() {
			@Override
			public void generateJassEquivalent(int indent, JassTextGenerator jassTextGenerator) {
				final StringBuilder sb = new StringBuilder();
				JassTextGenerator.Util.indent(indent, sb);
				sb.append("return false");
				jassTextGenerator.println(sb.toString());
			}
		});
		final String iterationActionsName = jassTextGenerator.createAnonymousFunction(modifiedActionList,
				"UnitsInRangeOfUnitEnumActions");

		final StringBuilder sb = new StringBuilder();
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("// TODO: use a global filter");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call GroupEnumUnitsInRangeOfUnit(au_tempGroup, "
				+ this.originUnit.generateJassEquivalent(jassTextGenerator) + ", "
				+ this.range.generateJassEquivalent(jassTextGenerator) + ", Filter("
				+ jassTextGenerator.functionPointerByName(iterationActionsName) + "))");
		jassTextGenerator.println(sb.toString());

		sb.setLength(0);
		JassTextGenerator.Util.indent(indent, sb);
		sb.append("call FlushChildLocalStore(" + jassTextGenerator.getTriggerLocalStore()
				+ ", AB_LOCAL_STORE_KEY_ENUMUNIT + I2S(" + jassTextGenerator.getCastId() + "))");
		jassTextGenerator.println(sb.toString());
	}
}

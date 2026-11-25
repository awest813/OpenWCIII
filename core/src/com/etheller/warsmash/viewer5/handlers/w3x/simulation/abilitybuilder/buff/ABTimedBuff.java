package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.buff;

import java.util.List;
import java.util.Map;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingFx;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

public class ABTimedBuff extends ABGenericTimedBuff {
	private static final War3ID SLOW_BUFF = War3ID.fromString("Bslo");

	protected Map<String, Object> localStore;
	private List<ABAction> onAddActions;
	private List<ABAction> onRemoveActions;
	private List<ABAction> onExpireActions;
	
	private CEffectType artType = CEffectType.TARGET;
	private NonStackingFx fx;
	private SimulationRenderComponent sfx;
	private SimulationRenderComponent lsfx;
	
	protected int castId = 0;

	public ABTimedBuff(int handleId, War3ID alias, float duration, boolean showTimedLifeBar, Map<String, Object> localStore,
			List<ABAction> onAddActions, List<ABAction> onRemoveActions, List<ABAction> onExpireActions, boolean showIcon, final int castId) {
		this(handleId, alias, duration, showTimedLifeBar, localStore, onAddActions, onRemoveActions, onExpireActions, castId);
		this.setIconShowing(showIcon);
	}

	public ABTimedBuff(int handleId, War3ID alias, float duration, boolean showTimedLifeBar, Map<String, Object> localStore,
			List<ABAction> onAddActions, List<ABAction> onRemoveActions, List<ABAction> onExpireActions, final int castId) {
		super(handleId, alias, duration, showTimedLifeBar);
		this.localStore = localStore;
		this.onAddActions = onAddActions;
		this.onRemoveActions = onRemoveActions;
		this.onExpireActions = onExpireActions;
		this.castId = castId;
	}
	
	public void setArtType(CEffectType artType) {
		this.artType = artType;
	}

	@Override
	public boolean isDispellable() {
		return SLOW_BUFF.equals(getAlias());
	}

	@Override
	public DispelPolarity getDispelPolarity() {
		return SLOW_BUFF.equals(getAlias()) ? DispelPolarity.HARMFUL : DispelPolarity.UNCLASSIFIED;
	}

	@Override
	protected void onBuffAdd(CSimulation game, CUnit unit) {
		if(this.fx != null) {
			unit.removeNonStackingFx(game, this.fx);
		}
		if (this.getAlias() != null) {
			if (artType != null) {
				this.fx = unit.addNonStackingFx(game, getAlias().asStringValue(), getAlias(), artType);
			}
			this.sfx = game.unitSoundEffectEvent(unit, getAlias());
			this.lsfx = game.unitLoopSoundEffectEvent(unit, getAlias());
		}
		if (onAddActions != null) {
			for (ABAction action : onAddActions) {
				action.runAction(game, unit, localStore, castId);
			}
		}
	}

	@Override
	protected void onBuffRemove(CSimulation game, CUnit unit) {
		if (this.fx != null) {
			unit.removeNonStackingFx(game, this.fx);
			this.fx = null;
		}
		if (this.sfx != null) {
			this.sfx.remove();
		}
		if (this.lsfx != null) {
			this.lsfx.remove();
		}
		if (onRemoveActions != null) {
			for (ABAction action : onRemoveActions) {
				action.runAction(game, unit, localStore, castId);
			}
		}
	}

	@Override
	protected void onBuffExpire(CSimulation game, CUnit unit) {
		if (onExpireActions != null) {
			for (ABAction action : onExpireActions) {
				action.runAction(game, unit, localStore, castId);
			}
		}
	}

	
	
	
}

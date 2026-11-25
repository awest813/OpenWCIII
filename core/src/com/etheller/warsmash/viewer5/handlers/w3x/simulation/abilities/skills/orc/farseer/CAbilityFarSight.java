package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CTimedCircleFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;

public final class CAbilityFarSight extends CAbilityPointTargetSpellBase {
	public static final War3ID ITEM_AREA_DETECTION = War3ID.fromString("AIta");
	private float radius;
	private byte detectionTypes;

	public CAbilityFarSight(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return ITEM_AREA_DETECTION.equals(getCode()) ? OrderIds.itemareadetection : OrderIds.farsight;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.detectionTypes = (byte) data.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final float x = target.getX();
		final float y = target.getY();
		final var owner = game.getPlayer(caster.getPlayerIndex());
		owner.addFogModifer(game, new CTimedCircleFogModifier(CFogState.VISIBLE, this.radius, x, y, getDuration(),
				this.detectionTypes), false);
		game.spawnSpellEffectOnPoint(x, y, 0, getAlias(), CEffectType.AREA_EFFECT, 0).remove();
		return false;
	}
}

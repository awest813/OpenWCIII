package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;

public final class CAbilityItemBundleOfLumber extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIlu");
	private int lumber;

	public CAbilityItemBundleOfLumber(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemgivelumber;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.lumber = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		if (orderId == getBaseOrderId()) {
			// The inventory removes the pickup ability before a cast animation can finish.
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var owner = game.getPlayer(caster.getPlayerIndex());
		owner.addLumber(this.lumber);
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		game.unitSoundEffectEvent(caster, getAlias());
		game.unitGainResourceEvent(caster, owner.getId(), ResourceType.LUMBER, this.lumber);
		return false;
	}
}

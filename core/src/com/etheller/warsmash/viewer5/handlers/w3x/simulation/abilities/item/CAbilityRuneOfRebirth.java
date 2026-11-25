package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public final class CAbilityRuneOfRebirth extends CAbilityNoTargetSpellBase {
	private CItem rune;

	public CAbilityRuneOfRebirth(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemruneofrebirth;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.rune = item;
	}

	@Override
	public CItem getItem() {
		return this.rune;
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId, final AbilityTarget target) {
		if (orderId == getBaseOrderId()) {
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.rune != null && this.rune.getDropSourceUnitType() != null) {
			final CUnit reborn = game.createUnitSimple(this.rune.getDropSourceUnitType(), caster.getPlayerIndex(),
					this.rune.getX(), this.rune.getY(), caster.getFacing());
			game.createTemporarySpellEffectOnUnit(reborn, getAlias(), CEffectType.TARGET);
			game.unitSoundEffectEvent(caster, getAlias());
		}
		return false;
	}
}

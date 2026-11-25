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

/** Item powerup that adds levels to the research specified by the glyph. */
public final class CAbilityItemGlyph extends CAbilityNoTargetSpellBase {
	private War3ID research;
	private int levels;

	public CAbilityItemGlyph(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemultravisionglyph;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		final String researchId = definition.getFieldAsString(AbilityFields.UNIT_ID + level, 0).trim();
		this.research = researchId.length() == 4 ? War3ID.fromString(researchId) : null;
		this.levels = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		if (orderId == getBaseOrderId()) {
			// Powerup pickup removes the temporary ability immediately after this order.
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var upgrade = this.research == null ? null : game.getUpgradeData().getType(this.research);
		if (upgrade != null && this.levels > 0) {
			final var owner = game.getPlayer(caster.getPlayerIndex());
			final int previous = owner.getTechtreeUnlocked(this.research);
			final int granted = Math.min(this.levels, upgrade.getLevelCount() - previous);
			if (granted > 0) {
				owner.addTechResearched(game, this.research, granted);
			}
			game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
			game.unitSoundEffectEvent(caster, getAlias());
		}
		return false;
	}
}

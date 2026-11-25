package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import java.util.EnumSet;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;

public final class CAbilityMeatWagonCargo extends CAbilityCargoHold {
	public static final War3ID CODE = War3ID.fromString("Amtc");

	public CAbilityMeatWagonCargo(final int handleId, final War3ID alias, final int capacity, final float duration,
			final float castRange, final EnumSet<CTargetType> targetsAllowed) {
		super(handleId, CODE, alias, capacity, duration, castRange, targetsAllowed);
	}

	@Override
	public boolean hasCapacity(final int count) {
		return count > 0 && count <= getCargoCapacity() - getCargoCount();
	}

	public boolean canStoreCorpse(final CUnit corpse) {
		return hasCapacity(1) && corpse.isDead() && corpse.isRaisable() && corpse.isDecays()
				&& !corpse.isHero() && !corpse.isBuilding() && !corpse.isHidden() && corpse.getTransport() == null;
	}

	public void storeCorpse(final CSimulation game, final CUnit wagon, final CUnit corpse) {
		if (!canStoreCorpse(corpse)) return;
		game.getWorldCollision().removeUnit(corpse);
		corpse.prepareCorpseForDecay(game);
		corpse.setHidden(true);
		corpse.moveStoredCorpseTo(wagon);
		addUnit(wagon, corpse);
	}

	@Override
	public void onTick(final CSimulation game, final CUnit wagon) {
		for (int index = 0; index < getCargoCount(); index++) getUnit(index).moveStoredCorpseTo(wagon);
	}

	public static boolean canUseStoredCorpse(final CSimulation game, final CUnit caster, final CUnit corpse) {
		final CUnit wagon = corpse.getTransport();
		return corpse.isDead() && wagon != null && !wagon.isDead() && !wagon.isHidden()
				&& wagon.getCargoData() instanceof CAbilityMeatWagonCargo
				&& game.getPlayer(caster.getPlayerIndex()).hasAlliance(wagon.getPlayerIndex(), CAllianceType.PASSIVE);
	}

	@Override
	public boolean dropUnitByIndex(final CSimulation game, final CUnit wagon, final int index) {
		final CUnit corpse = getUnit(index);
		if (!super.dropUnitByIndex(game, wagon, index)) {
			removeUnitAtIndex(wagon, index);
			corpse.relocate(wagon.getX(), wagon.getY(), game);
			corpse.setHidden(false);
		}
		corpse.prepareCorpseForDecay(game);
		game.getWorldCollision().addUnit(corpse);
		return true;
	}
}

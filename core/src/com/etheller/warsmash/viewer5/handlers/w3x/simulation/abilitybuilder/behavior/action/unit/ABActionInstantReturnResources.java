package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.action.unit;

import java.util.Map;

import com.etheller.warsmash.parsers.jass.JassTextGenerator;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.harvest.CAbilityHarvest;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.behavior.callback.unitcallbacks.ABUnitCallback;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilitybuilder.core.ABSingleAction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;

public class ABActionInstantReturnResources implements ABSingleAction {

	private ABUnitCallback unit;

	@Override
	public void runAction(CSimulation game, CUnit caster, Map<String, Object> localStore, final int castId) {
		CUnit targetUnit = caster;
		if (this.unit != null) {
			targetUnit = this.unit.callback(game, caster, localStore, castId);
		}

		final CAbilityHarvest harv = targetUnit.getFirstAbilityOfType(CAbilityHarvest.class);
		if ((harv != null) && (harv.getCarriedResourceType() != null) && (harv.getCarriedResourceAmount() > 0)) {
			final CPlayer pl = game.getPlayer(targetUnit.getPlayerIndex());
			switch (harv.getCarriedResourceType()) {
			case FOOD:
				// This might be a bad idea? Not sure it will ever matter
				pl.setFoodCap(Math.min(pl.getFoodCap() + harv.getCarriedResourceAmount(), pl.getFoodCapCeiling()));
				game.unitGainResourceEvent(targetUnit, pl.getId(), harv.getCarriedResourceType(),
						harv.getCarriedResourceAmount());
				harv.setCarriedResources(ResourceType.FOOD, 0);
				break;
			case GOLD: {
				final int goldMined = harv.getCarriedResourceAmount();
				final int goldTax = Math.round(goldMined * (pl.getGoldUpkeepRate() / 100.0f));
				final int goldGained = goldMined - goldTax;
				pl.addGold(goldGained);
				game.unitGainResourceEvent(targetUnit, pl.getId(), harv.getCarriedResourceType(),
						goldGained);
				harv.setCarriedResources(ResourceType.GOLD, 0);
				break;
			}
			case LUMBER: {
				final int lumberHarvested = harv.getCarriedResourceAmount();
				final int lumberTax = Math.round(lumberHarvested * (pl.getLumberUpkeepRate() / 100.0f));
				final int lumberGained = lumberHarvested - lumberTax;
				pl.addLumber(lumberGained);
				game.unitGainResourceEvent(targetUnit, pl.getId(), harv.getCarriedResourceType(),
						lumberGained);
				harv.setCarriedResources(ResourceType.LUMBER, 0);
				break;
			}
			case MANA:
				// ??
				break;
			default:
				break;

			}
		}

	}

	@Override
	public String generateJassEquivalent(JassTextGenerator jassTextGenerator) {
		String targetExpression;
		if (this.unit != null) {
			targetExpression = this.unit.generateJassEquivalent(jassTextGenerator);
		}
		else {
			targetExpression = jassTextGenerator.getCaster();
		}
		return "UnitInstantReturnResources(" + targetExpression + ")";
	}

}

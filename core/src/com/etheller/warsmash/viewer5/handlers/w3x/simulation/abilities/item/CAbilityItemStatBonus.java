package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbilityGenericSingleIconPassiveAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;

public class CAbilityItemStatBonus extends AbilityGenericSingleIconPassiveAbility {
	private static final War3ID ATTRIBUTE_BONUS = War3ID.fromString("Aamk");
	private int strength;
	private int agility;
	private int intelligence;
	private boolean applied;

	public CAbilityItemStatBonus(final int handleId, final War3ID code, final War3ID alias, final int strength, final int agility,
			final int intelligence) {
		super(code, alias, handleId);
		setIconShowing(ATTRIBUTE_BONUS.equals(code));
		this.strength = strength;
		this.agility = agility;
		this.intelligence = intelligence;
	}

	@Override
	public void onAdd(final CSimulation game, final CUnit unit) {
		final CAbilityHero heroData = unit.getHeroData();
		if (heroData == null || this.applied) return;
		applyBonuses(game, unit, heroData, this.strength, this.agility, this.intelligence);
		this.applied = true;
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit unit) {
		final CAbilityHero heroData = unit.getHeroData();
		if (heroData != null && this.applied) {
			applyBonuses(game, unit, heroData, -this.strength, -this.agility, -this.intelligence);
		}
		this.applied = false;
	}

	public void setBonuses(final CSimulation game, final CUnit unit, final int strength, final int agility, final int intelligence) {
		final CAbilityHero heroData = unit.getHeroData();
		if (heroData != null && this.applied) {
			applyBonuses(game, unit, heroData, strength - this.strength, agility - this.agility, intelligence - this.intelligence);
		}
		this.strength = strength;
		this.agility = agility;
		this.intelligence = intelligence;
	}

	private static void applyBonuses(final CSimulation game, final CUnit unit, final CAbilityHero heroData,
			final int strength, final int agility, final int intelligence) {
		heroData.addStrengthBonus(game, unit, strength);
		heroData.addAgilityBonus(game, unit, agility);
		heroData.addIntelligenceBonus(game, unit, intelligence);
	}
}

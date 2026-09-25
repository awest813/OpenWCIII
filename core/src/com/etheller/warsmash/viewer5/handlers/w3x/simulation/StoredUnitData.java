package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;

/**
 * Immutable snapshot of a unit's serializable state captured by
 * {@code StoreUnit} and consumed by {@code RestoreUnit}.
 *
 * <p>For hero units the snapshot records XP, stat bases/bonuses, skill points,
 * proper name, and learned hero abilities. For every unit type the snapshot
 * records inventory items (type-id + charges) so items survive the carry-over
 * between maps.</p>
 */
public final class StoredUnitData {

	/** Rawcode of the unit type (four-character War3ID integer). */
	public final War3ID unitTypeId;

	// ---- hero stats (0 for non-hero units) ----

	public final int xp;
	public final int skillPoints;
	public final int strengthBase;
	public final int agilityBase;
	public final int intelligenceBase;
	public final int strengthBonus;
	public final int agilityBonus;
	public final int intelligenceBonus;

	/** Proper-name string (empty for non-hero units). */
	public final String properName;

	// ---- inventory ----

	/** Per-slot item data; length = inventory capacity, null entry = empty slot. */
	public final StoredItemData[] items;

	/**
	 * Learned hero abilities (ability id + level). Empty/null for non-heroes or
	 * heroes with no skills learned yet.
	 */
	public final StoredAbilityData[] abilities;

	public StoredUnitData(final War3ID unitTypeId, final int xp, final int skillPoints, final int strengthBase,
			final int agilityBase, final int intelligenceBase, final int strengthBonus, final int agilityBonus,
			final int intelligenceBonus, final String properName, final StoredItemData[] items) {
		this(unitTypeId, xp, skillPoints, strengthBase, agilityBase, intelligenceBase, strengthBonus, agilityBonus,
				intelligenceBonus, properName, items, null);
	}

	public StoredUnitData(final War3ID unitTypeId, final int xp, final int skillPoints, final int strengthBase,
			final int agilityBase, final int intelligenceBase, final int strengthBonus, final int agilityBonus,
			final int intelligenceBonus, final String properName, final StoredItemData[] items,
			final StoredAbilityData[] abilities) {
		this.unitTypeId = unitTypeId;
		this.xp = xp;
		this.skillPoints = skillPoints;
		this.strengthBase = strengthBase;
		this.agilityBase = agilityBase;
		this.intelligenceBase = intelligenceBase;
		this.strengthBonus = strengthBonus;
		this.agilityBonus = agilityBonus;
		this.intelligenceBonus = intelligenceBonus;
		this.properName = properName != null ? properName : "";
		this.items = items;
		this.abilities = abilities;
	}

	/** Creates a campaign carryover unit, restoring item effects before total bonuses. */
	public CUnit createUnit(final CSimulation simulation, final int playerIndex, final float x, final float y,
			final float facing) {
		final CUnit unit = simulation.createUnitSimple(this.unitTypeId, playerIndex, x, y, facing);
		if (unit == null) {
			return null;
		}
		final CAbilityHero hero = unit.getHeroData();
		if (hero != null) {
			hero.setXp(simulation, unit, this.xp, false);
			hero.setStrengthBase(simulation, unit, this.strengthBase);
			hero.setAgilityBase(simulation, unit, this.agilityBase);
			hero.setIntelligenceBase(simulation, unit, this.intelligenceBase);
			if (this.abilities != null) {
				for (final StoredAbilityData ability : this.abilities) {
					if ((ability == null) || (ability.abilityId == null) || (ability.level <= 0)) {
						continue;
					}
					// Give each skill its own budget rather than a fixed 64-point allowance.
					hero.setSkillPoints(ability.level);
					for (int level = 0; level < ability.level; level++) {
						final int before = hero.getSkillPoints();
						hero.selectHeroSkill(simulation, unit, ability.abilityId);
						if (hero.getSkillPoints() == before) {
							break; // Unsupported/max-level skill: do not spin on corrupt data.
						}
					}
				}
			}
			hero.setSkillPoints(this.skillPoints);
			if (!this.properName.isEmpty()) {
				hero.setProperName(this.properName);
			}
		}
		final CAbilityInventory inventory = unit.getInventoryData();
		if ((inventory != null) && (this.items != null)) {
			for (int slot = 0; slot < Math.min(this.items.length, inventory.getItemCapacity()); slot++) {
				final StoredItemData itemData = this.items[slot];
				if (itemData != null) {
					final CItem item = simulation.createItem(itemData.typeId, x, y);
					if (item != null) {
						item.setCharges(itemData.charges);
						inventory.giveItem(simulation, unit, item, slot, false);
					}
				}
			}
		}
		if (hero != null) {
			// Stored bonuses already include inventory effects. Apply only the difference
			// after equipping items, so carrying gear across chapters cannot stack stats.
			hero.addStrengthBonus(simulation, unit, this.strengthBonus - hero.getStrength().getBonus());
			hero.addAgilityBonus(simulation, unit, this.agilityBonus - hero.getAgility().getBonus());
			hero.addIntelligenceBonus(simulation, unit, this.intelligenceBonus - hero.getIntelligence().getBonus());
		}
		return unit;
	}

	/** Data for a single inventory slot. */
	public static final class StoredItemData {
		public final War3ID typeId;
		public final int charges;

		public StoredItemData(final War3ID typeId, final int charges) {
			this.typeId = typeId;
			this.charges = charges;
		}
	}

	/** Data for a learned hero ability. */
	public static final class StoredAbilityData {
		public final War3ID abilityId;
		public final int level;

		public StoredAbilityData(final War3ID abilityId, final int level) {
			this.abilityId = abilityId;
			this.level = level;
		}
	}
}

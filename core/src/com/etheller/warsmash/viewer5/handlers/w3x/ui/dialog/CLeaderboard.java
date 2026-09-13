package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboard state used by campaign/script natives. Rendering is handled by
 * {@code MeleeUI} from the item list when displayed.
 */
public class CLeaderboard {
	private String label = "";
	private boolean displayed;
	private final List<Item> items = new ArrayList<>();

	public static final class Item {
		public String label;
		public int value;
		public int playerIndex;

		public Item(final String label, final int value, final int playerIndex) {
			this.label = label != null ? label : "";
			this.value = value;
			this.playerIndex = playerIndex;
		}
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(final String label) {
		this.label = label != null ? label : "";
	}

	public boolean isDisplayed() {
		return this.displayed;
	}

	public void setDisplayed(final boolean displayed) {
		this.displayed = displayed;
	}

	public List<Item> getItems() {
		return this.items;
	}

	public int getItemCount() {
		return this.items.size();
	}

	public void addItem(final String itemLabel, final int value, final int playerIndex) {
		this.items.add(new Item(itemLabel, value, playerIndex));
	}

	public void removeItem(final int index) {
		if ((index >= 0) && (index < this.items.size())) {
			this.items.remove(index);
		}
	}

	public void clear() {
		this.items.clear();
	}

	public boolean hasPlayerItem(final int playerIndex) {
		for (final Item item : this.items) {
			if (item.playerIndex == playerIndex) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes the first row belonging to the given player, matching the retail
	 * behavior of LeaderboardRemovePlayerItem.
	 *
	 * @return true if a row was removed
	 */
	public boolean removePlayerItem(final int playerIndex) {
		for (int i = 0; i < this.items.size(); i++) {
			if (this.items.get(i).playerIndex == playerIndex) {
				this.items.remove(i);
				return true;
			}
		}
		return false;
	}

	public int getPlayerIndex(final int itemIndex) {
		if ((itemIndex < 0) || (itemIndex >= this.items.size())) {
			return -1;
		}
		return this.items.get(itemIndex).playerIndex;
	}

	public void setItemValue(final int index, final int value) {
		if ((index >= 0) && (index < this.items.size())) {
			this.items.get(index).value = value;
		}
	}

	public void setItemLabel(final int index, final String itemLabel) {
		if ((index >= 0) && (index < this.items.size())) {
			this.items.get(index).label = itemLabel != null ? itemLabel : "";
		}
	}
}

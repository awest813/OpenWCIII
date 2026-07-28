package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Multiboard state used by campaign/script natives. Rendering is handled by
 * {@code MeleeUI} when {@link #isDisplayed()} is true.
 */
public class CMultiboard {
	private String title = "";
	private int columns = 0;
	private int rows = 0;
	private boolean displayed = false;
	private boolean minimized = false;
	/** Persistent item cells indexed by row * 1000 + col. */
	private final Map<Integer, CMultiboardItem> items = new HashMap<>();

	public String getTitle() {
		return this.title;
	}

	public void setTitle(final String title) {
		this.title = title != null ? title : "";
	}

	public int getColumns() {
		return this.columns;
	}

	public void setColumns(final int columns) {
		this.columns = Math.max(0, columns);
	}

	public int getRows() {
		return this.rows;
	}

	public void setRows(final int rows) {
		this.rows = Math.max(0, rows);
	}

	public boolean isDisplayed() {
		return this.displayed;
	}

	public void setDisplayed(final boolean displayed) {
		this.displayed = displayed;
	}

	public boolean isMinimized() {
		return this.minimized;
	}

	public void setMinimized(final boolean minimized) {
		this.minimized = minimized;
	}

	public void clear() {
		this.items.clear();
		this.rows = 0;
		this.columns = 0;
		this.title = "";
	}

	public void setAllItemsValue(final String value) {
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				getItem(row, col).setValue(value);
			}
		}
	}

	public void setAllItemsWidth(final float width) {
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				getItem(row, col).setWidth(width);
			}
		}
	}

	public void setAllItemsIcon(final String icon) {
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				getItem(row, col).setIcon(icon);
			}
		}
	}

	public void setAllItemsStyle(final int style) {
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				getItem(row, col).setStyle(style);
			}
		}
	}

	public void setAllItemsValueColor(final float r, final float g, final float b, final float a) {
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				getItem(row, col).setValueColor(r, g, b, a);
			}
		}
	}

	/**
	 * Returns (or lazily creates) the persistent item cell for the given row and
	 * column (0-based).
	 */
	public CMultiboardItem getItem(final int row, final int column) {
		final int key = row * 1000 + column;
		CMultiboardItem item = this.items.get(key);
		if (item == null) {
			item = new CMultiboardItem();
			this.items.put(key, item);
		}
		return item;
	}

	/** Builds a plain-text snapshot suitable for a simple overlay StringFrame. */
	public String toDisplayText() {
		final StringBuilder sb = new StringBuilder();
		if ((this.title != null) && !this.title.isEmpty()) {
			sb.append(this.title).append('\n');
		}
		if (this.minimized) {
			return sb.toString();
		}
		for (int row = 0; row < this.rows; row++) {
			for (int col = 0; col < this.columns; col++) {
				if (col > 0) {
					sb.append(" | ");
				}
				sb.append(getItem(row, col).getValue());
			}
			if (row + 1 < this.rows) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}
}

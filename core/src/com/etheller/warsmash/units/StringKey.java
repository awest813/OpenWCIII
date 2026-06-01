package com.etheller.warsmash.units;

/**
 * A hashable wrapper object for a String that can be used as the key in a
 * hashtable, but which disregards case as a key -- except that it will remember
 * case if directly asked for its value. The game needs this to be able to show
 * the original case of a string to the user in the editor, while still doing
 * map lookups in a case insensitive way.
 *
 * @author Eric
 *
 */
public final class StringKey {
	private final String string;
	private final int hashVal;

	public StringKey(final String string) {
		this.string = string;
		int h = 0;
		if (string != null) {
			for (int i = 0; i < string.length(); i++) {
				h = 31 * h + Character.toLowerCase(string.charAt(i));
			}
		}
		this.hashVal = 31 + h;
	}

	public String getString() {
		return this.string;
	}

	@Override
	public int hashCode() {
		return this.hashVal;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final StringKey other = (StringKey) obj;
		if (this.string == null) {
			if (other.string != null) {
				return false;
			}
		}
		else if (!this.string.equalsIgnoreCase(other.string)) {
			return false;
		}
		return true;
	}
}
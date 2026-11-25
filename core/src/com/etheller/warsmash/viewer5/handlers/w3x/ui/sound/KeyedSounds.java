package com.etheller.warsmash.viewer5.handlers.w3x.ui.sound;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;

public class KeyedSounds {
	private final Function<String, Element> lookup;
	private final DataSource dataSource;
	private final Map<String, UnitSound> keyToSound;

	public KeyedSounds(final DataTable uiSoundsTable, final DataSource dataSource) {
		this(uiSoundsTable::get, dataSource);
	}

	public KeyedSounds(final Function<String, Element> lookup, final DataSource dataSource) {
		this.lookup = lookup;
		this.dataSource = dataSource;
		this.keyToSound = new HashMap<>();
	}

	public UnitSound getSound(final String key) {
		UnitSound sound = this.keyToSound.get(key);
		if (sound == null) {
			sound = UnitSound.create(this.dataSource, this.lookup.apply(key));
			this.keyToSound.put(key, sound);
		}
		return sound;
	}
}

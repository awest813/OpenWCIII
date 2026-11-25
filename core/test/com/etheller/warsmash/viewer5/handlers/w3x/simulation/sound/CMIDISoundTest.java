package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSourceDescriptor;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.WorldEditStrings;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

class CMIDISoundTest {
	@Test
	void ambientGroupDucksMidiAtTheCurrentMasterVolume() {
		final OptionsSettingsStore settings = OptionsSettingsStore.get();
		final OptionsSettingsStore previous = new OptionsSettingsStore();
		previous.copyFrom(settings);
		try {
			settings.reset();
			settings.setSoundVolume(50);
			final double[] ambientScale = { 1 };
			final CMIDISound sound = new CMIDISound(null, "test", null, () -> false, () -> ambientScale[0]);
			assertEquals(0.5f, sound.effectiveGain(), 0.00001f);
			ambientScale[0] = 0.25;
			assertEquals(0.125f, sound.effectiveGain(), 0.00001f);
			ambientScale[0] = 0;
			assertEquals(0f, sound.effectiveGain());
			ambientScale[0] = 1;
			assertEquals(0.5f, sound.effectiveGain(), 0.00001f);
			settings.setAmbientSounds(false);
			assertEquals(0f, sound.effectiveGain());
		}
		finally {
			settings.copyFrom(previous);
		}
	}

	@Test
	void classicLabelsLoadSequencesAndTheirAuthoredInstrumentBank() throws Exception {
		final List<DataSource> archives = new ArrayList<>();
		try {
			for (final String path : RetailTestData.requireAll()) {
				archives.add(new MpqDataSourceDescriptor(path).createDataSource());
			}
			final CompoundDataSource assets = new CompoundDataSource(archives);
			final DataTable labels = new DataTable(new WorldEditStrings(assets));
			try (InputStream stream = assets.getResourceAsStream("UI\\SoundInfo\\MIDISounds.slk")) {
				assertNotNull(stream);
				labels.readSLK(stream);
			}
			for (final String label : List.of("DalaranRuinsDay", "DalaranRuinsNight")) {
				assertNotNull(labels.get(label));
				final CMIDISound sound = new CMIDISound(assets, label, labels.get(label), () -> false);
				assertTrue(sound.getPredictedDuration() > 0, label);
				sound.start();
				assertTrue(sound.updatePlayback(), "Retain the request while checkpoint replay suppresses output");
				assertFalse(sound.isPlaying());
				sound.stop();
				assertFalse(sound.updatePlayback());
			}
			try (InputStream stream = assets.getResourceAsStream("Sound\\Ambient\\DalaranRuins\\DalaranRuins.dls")) {
				assertNotNull(stream);
				assertTrue(MidiSoundbankReader.read(stream).getInstruments().length > 0);
			}
		}
		finally {
			for (final DataSource source : archives) {
				source.close();
			}
		}
	}
}

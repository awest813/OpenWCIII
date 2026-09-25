package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionsSettingsStoreTest {

	@TempDir
	File tmpDir;

	@Test
	void defaults() {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		assertEquals(100, store.getMusicVolume());
		assertTrue(store.isMusicEnabled());
		assertEquals(100, store.getEffectiveMusicVolume());
		assertEquals(100, store.getSoundVolume());
		assertTrue(store.isSubtitles());
		assertTrue(store.isUnitSounds());
		assertTrue(store.isAmbientSounds());
		assertTrue(store.isMovementSounds());
		assertEquals(50, store.getMouseScrollSpeed());
		assertEquals(50, store.getKeyScrollSpeed());
		assertFalse(store.isMouseScrollDisabled());
		assertEquals(50, store.getGamma());
	}

	@Test
	void percentagesClamp() {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.setMusicVolume(150);
		assertEquals(100, store.getMusicVolume());
		store.setMusicVolume(-5);
		assertEquals(0, store.getMusicVolume());
		store.setGamma(1000);
		assertEquals(100, store.getGamma());
	}

	@Test
	void effectiveMusicVolumeHonorsEnabled() {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.setMusicVolume(70);
		assertEquals(70, store.getEffectiveMusicVolume());
		store.setMusicEnabled(false);
		assertEquals(0, store.getEffectiveMusicVolume());
		store.setMusicEnabled(true);
		assertEquals(70, store.getEffectiveMusicVolume());
	}

	@Test
	void roundtrip() throws Exception {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.setMusicVolume(70);
		store.setMusicEnabled(false);
		store.setSoundVolume(30);
		store.setSoundEnabled(false);
		store.setSubtitles(false);
		store.setUnitSounds(false);
		store.setTooltips(false);
		store.setPositionalAudio(false);
		store.setMouseScrollSpeed(80);
		store.setMouseScrollDisabled(true);
		store.setGamma(60);
		final File f = new File(this.tmpDir, "options.properties");
		store.save(f);

		final OptionsSettingsStore loaded = new OptionsSettingsStore();
		loaded.load(f);
		assertEquals(70, loaded.getMusicVolume());
		assertFalse(loaded.isMusicEnabled());
		assertEquals(0, loaded.getEffectiveMusicVolume());
		assertEquals(30, loaded.getSoundVolume());
		assertFalse(loaded.isSoundEnabled());
		assertEquals(0, loaded.getEffectiveSoundVolume());
		assertFalse(loaded.isTooltips());
		assertFalse(loaded.isPositionalAudio());
		assertTrue(loaded.isMultibuttonMouse());
		assertFalse(loaded.isSubtitles());
		assertFalse(loaded.isUnitSounds());
		assertTrue(loaded.isAmbientSounds());
		assertEquals(80, loaded.getMouseScrollSpeed());
		assertTrue(loaded.isMouseScrollDisabled());
		assertEquals(60, loaded.getGamma());
	}

	@Test
	void missingFileKeepsValues() {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.setMusicVolume(11);
		store.load(new File(this.tmpDir, "nope.properties"));
		assertEquals(11, store.getMusicVolume());
	}

	@Test
	void malformedValuesIgnored() throws Exception {
		final File f = new File(this.tmpDir, "bad.properties");
		final Properties props = new Properties();
		props.setProperty("musicVolume", "loud");
		props.setProperty("subtitles", "maybe");
		try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
			props.store(out, "");
		}
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.load(f);
		assertEquals(100, store.getMusicVolume());
		assertTrue(store.isSubtitles());
	}

	@Test
	void draftCopy() {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		store.setMusicVolume(42);
		final OptionsSettingsStore draft = new OptionsSettingsStore();
		draft.copyFrom(store);
		assertEquals(42, draft.getMusicVolume());
		draft.setMusicVolume(7);
		assertEquals(42, store.getMusicVolume());
		assertEquals(7, draft.getMusicVolume());
	}

	@Test
	void parentDirsCreated() throws Exception {
		final OptionsSettingsStore store = new OptionsSettingsStore();
		final File f = new File(this.tmpDir, "nested" + File.separator + "options.properties");
		store.save(f);
		assertTrue(f.exists());
		assertTrue(Files.size(f.toPath()) > 0);
	}
}

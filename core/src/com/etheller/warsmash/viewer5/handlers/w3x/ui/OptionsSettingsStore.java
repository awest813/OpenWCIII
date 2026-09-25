package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Persisted player options (main-menu Options screen).
 *
 * <p>Values are 0-100 percentages unless noted. Settings take live effect
 * where the engine has a backend today (music volume: menu music and
 * in-mission music players) and are otherwise stored so later passes can honor
 * them (sound volume, scroll speeds, subtitles, gamma, toggles). Files that
 * predate a key load with that key's default.</p>
 *
 * <p>Pure Java with no LibGDX dependency so it can be unit tested headlessly.
 * The menu edits a draft copy and commits it on OK; Cancel discards the draft.</p>
 */
public final class OptionsSettingsStore {
	private static final OptionsSettingsStore INSTANCE = new OptionsSettingsStore();

	public static final int DEFAULT_MUSIC_VOLUME = 100;
	public static final int DEFAULT_SOUND_VOLUME = 100;
	public static final int DEFAULT_SCROLL_SPEED = 50;
	public static final int DEFAULT_GAMMA = 50;

	private int musicVolume = DEFAULT_MUSIC_VOLUME;
	private boolean musicEnabled = true;
	private int soundVolume = DEFAULT_SOUND_VOLUME;
	private boolean soundEnabled = true;
	private boolean subtitles = true;
	private boolean unitSounds = true;
	private boolean ambientSounds = true;
	private boolean movementSounds = true;
	private int mouseScrollSpeed = DEFAULT_SCROLL_SPEED;
	private int keyScrollSpeed = DEFAULT_SCROLL_SPEED;
	private boolean mouseScrollDisabled;
	private boolean tooltips = true;
	private boolean multibuttonMouse = true;
	private boolean environmentalAudio = true;
	private boolean positionalAudio = true;
	private int gamma = DEFAULT_GAMMA;

	public static OptionsSettingsStore get() {
		return INSTANCE;
	}

	/** File used for persistence ({@code ~/.warsmash/options.properties}). */
	public static File optionsFile() {
		return new File(System.getProperty("user.home") + File.separator + ".warsmash" + File.separator
				+ "options.properties");
	}

	public void copyFrom(final OptionsSettingsStore other) {
		this.musicVolume = other.musicVolume;
		this.musicEnabled = other.musicEnabled;
		this.soundVolume = other.soundVolume;
		this.soundEnabled = other.soundEnabled;
		this.subtitles = other.subtitles;
		this.unitSounds = other.unitSounds;
		this.ambientSounds = other.ambientSounds;
		this.movementSounds = other.movementSounds;
		this.mouseScrollSpeed = other.mouseScrollSpeed;
		this.keyScrollSpeed = other.keyScrollSpeed;
		this.mouseScrollDisabled = other.mouseScrollDisabled;
		this.tooltips = other.tooltips;
		this.multibuttonMouse = other.multibuttonMouse;
		this.environmentalAudio = other.environmentalAudio;
		this.positionalAudio = other.positionalAudio;
		this.gamma = other.gamma;
	}

	public int getMusicVolume() {
		return this.musicVolume;
	}

	public void setMusicVolume(final int volume) {
		this.musicVolume = clamp(volume);
	}

	public boolean isMusicEnabled() {
		return this.musicEnabled;
	}

	public void setMusicEnabled(final boolean enabled) {
		this.musicEnabled = enabled;
	}

	/**
	 * Volume actually sent to music players: 0 when music is disabled,
	 * otherwise {@link #getMusicVolume()}.
	 */
	public int getEffectiveMusicVolume() {
		return this.musicEnabled ? this.musicVolume : 0;
	}

	public int getSoundVolume() {
		return this.soundVolume;
	}

	public void setSoundVolume(final int volume) {
		this.soundVolume = clamp(volume);
	}

	public boolean isSoundEnabled() {
		return this.soundEnabled;
	}

	public void setSoundEnabled(final boolean enabled) {
		this.soundEnabled = enabled;
	}

	/**
	 * Volume actually sent to sound effects: 0 when sound is disabled,
	 * otherwise {@link #getSoundVolume()}. No SFX backend consumes this yet; it
	 * is stored so later passes can honor it.
	 */
	public int getEffectiveSoundVolume() {
		return this.soundEnabled ? this.soundVolume : 0;
	}

	public boolean isSubtitles() {
		return this.subtitles;
	}

	public void setSubtitles(final boolean subtitles) {
		this.subtitles = subtitles;
	}

	public boolean isUnitSounds() {
		return this.unitSounds;
	}

	public void setUnitSounds(final boolean unitSounds) {
		this.unitSounds = unitSounds;
	}

	public boolean isAmbientSounds() {
		return this.ambientSounds;
	}

	public void setAmbientSounds(final boolean ambientSounds) {
		this.ambientSounds = ambientSounds;
	}

	public boolean isMovementSounds() {
		return this.movementSounds;
	}

	public void setMovementSounds(final boolean movementSounds) {
		this.movementSounds = movementSounds;
	}

	public int getMouseScrollSpeed() {
		return this.mouseScrollSpeed;
	}

	public void setMouseScrollSpeed(final int speed) {
		this.mouseScrollSpeed = clamp(speed);
	}

	public int getKeyScrollSpeed() {
		return this.keyScrollSpeed;
	}

	public void setKeyScrollSpeed(final int speed) {
		this.keyScrollSpeed = clamp(speed);
	}

	public boolean isMouseScrollDisabled() {
		return this.mouseScrollDisabled;
	}

	public void setMouseScrollDisabled(final boolean disabled) {
		this.mouseScrollDisabled = disabled;
	}

	public boolean isTooltips() {
		return this.tooltips;
	}

	public void setTooltips(final boolean tooltips) {
		this.tooltips = tooltips;
	}

	public boolean isMultibuttonMouse() {
		return this.multibuttonMouse;
	}

	public void setMultibuttonMouse(final boolean multibuttonMouse) {
		this.multibuttonMouse = multibuttonMouse;
	}

	public boolean isEnvironmentalAudio() {
		return this.environmentalAudio;
	}

	public void setEnvironmentalAudio(final boolean environmentalAudio) {
		this.environmentalAudio = environmentalAudio;
	}

	public boolean isPositionalAudio() {
		return this.positionalAudio;
	}

	public void setPositionalAudio(final boolean positionalAudio) {
		this.positionalAudio = positionalAudio;
	}

	public int getGamma() {
		return this.gamma;
	}

	public void setGamma(final int gamma) {
		this.gamma = clamp(gamma);
	}

	/** Restores defaults (useful for tests). */
	public void reset() {
		this.musicVolume = DEFAULT_MUSIC_VOLUME;
		this.musicEnabled = true;
		this.soundVolume = DEFAULT_SOUND_VOLUME;
		this.soundEnabled = true;
		this.subtitles = true;
		this.unitSounds = true;
		this.ambientSounds = true;
		this.movementSounds = true;
		this.mouseScrollSpeed = DEFAULT_SCROLL_SPEED;
		this.keyScrollSpeed = DEFAULT_SCROLL_SPEED;
		this.mouseScrollDisabled = false;
		this.tooltips = true;
		this.multibuttonMouse = true;
		this.environmentalAudio = true;
		this.positionalAudio = true;
		this.gamma = DEFAULT_GAMMA;
	}

	/** Persists this store to {@code file}, creating parent directories as needed. */
	public void save(final File file) throws IOException {
		final File dir = file.getParentFile();
		if ((dir != null) && !dir.exists()) {
			dir.mkdirs();
		}
		final Properties props = new Properties();
		props.setProperty("musicVolume", Integer.toString(this.musicVolume));
		props.setProperty("musicEnabled", Boolean.toString(this.musicEnabled));
		props.setProperty("soundVolume", Integer.toString(this.soundVolume));
		props.setProperty("soundEnabled", Boolean.toString(this.soundEnabled));
		props.setProperty("subtitles", Boolean.toString(this.subtitles));
		props.setProperty("unitSounds", Boolean.toString(this.unitSounds));
		props.setProperty("ambientSounds", Boolean.toString(this.ambientSounds));
		props.setProperty("movementSounds", Boolean.toString(this.movementSounds));
		props.setProperty("mouseScrollSpeed", Integer.toString(this.mouseScrollSpeed));
		props.setProperty("keyScrollSpeed", Integer.toString(this.keyScrollSpeed));
		props.setProperty("mouseScrollDisabled", Boolean.toString(this.mouseScrollDisabled));
		props.setProperty("tooltips", Boolean.toString(this.tooltips));
		props.setProperty("multibuttonMouse", Boolean.toString(this.multibuttonMouse));
		props.setProperty("environmentalAudio", Boolean.toString(this.environmentalAudio));
		props.setProperty("positionalAudio", Boolean.toString(this.positionalAudio));
		props.setProperty("gamma", Integer.toString(this.gamma));
		try (FileOutputStream out = new FileOutputStream(file)) {
			props.store(out, "OpenWCIII options");
		}
	}

	/**
	 * Loads settings from {@code file}. A missing file keeps current values;
	 * missing keys keep their defaults; malformed values are ignored.
	 */
	public void load(final File file) {
		if ((file == null) || !file.exists()) {
			return;
		}
		final Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(file)) {
			props.load(in);
		}
		catch (final IOException e) {
			System.err.println("OptionsSettingsStore: failed to load " + file + ": " + e.getMessage());
			return;
		}
		this.musicVolume = getInt(props, "musicVolume", this.musicVolume);
		this.musicEnabled = getBool(props, "musicEnabled", this.musicEnabled);
		this.soundVolume = getInt(props, "soundVolume", this.soundVolume);
		this.soundEnabled = getBool(props, "soundEnabled", this.soundEnabled);
		this.subtitles = getBool(props, "subtitles", this.subtitles);
		this.unitSounds = getBool(props, "unitSounds", this.unitSounds);
		this.ambientSounds = getBool(props, "ambientSounds", this.ambientSounds);
		this.movementSounds = getBool(props, "movementSounds", this.movementSounds);
		this.mouseScrollSpeed = getInt(props, "mouseScrollSpeed", this.mouseScrollSpeed);
		this.keyScrollSpeed = getInt(props, "keyScrollSpeed", this.keyScrollSpeed);
		this.mouseScrollDisabled = getBool(props, "mouseScrollDisabled", this.mouseScrollDisabled);
		this.tooltips = getBool(props, "tooltips", this.tooltips);
		this.multibuttonMouse = getBool(props, "multibuttonMouse", this.multibuttonMouse);
		this.environmentalAudio = getBool(props, "environmentalAudio", this.environmentalAudio);
		this.positionalAudio = getBool(props, "positionalAudio", this.positionalAudio);
		this.gamma = getInt(props, "gamma", this.gamma);
	}

	private static int getInt(final Properties props, final String key, final int current) {
		final String raw = props.getProperty(key);
		if (raw == null) {
			return current;
		}
		try {
			return clamp(Integer.parseInt(raw.trim()));
		}
		catch (final NumberFormatException e) {
			return current;
		}
	}

	private static boolean getBool(final Properties props, final String key, final boolean current) {
		final String raw = props.getProperty(key);
		if (raw == null) {
			return current;
		}
		final String trimmed = raw.trim();
		if ("true".equalsIgnoreCase(trimmed)) {
			return true;
		}
		if ("false".equalsIgnoreCase(trimmed)) {
			return false;
		}
		return current;
	}

	private static int clamp(final int value) {
		return Math.max(0, Math.min(100, value));
	}
}

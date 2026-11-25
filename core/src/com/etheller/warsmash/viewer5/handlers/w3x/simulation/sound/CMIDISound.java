package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;

import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.viewer5.AudioBufferSource;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.OptionsSettingsStore;

public final class CMIDISound implements CSound {
	private final DataSource assets;
	private final String label;
	private final String sequencePath;
	private final String soundbankPath;
	private final boolean looping;
	private final BooleanSupplier playbackAllowed;
	private final DoubleSupplier ambientVolumeScale;
	private int volume;
	private float pitch;
	private transient boolean loaded;
	private transient Sequence sequence;
	private transient Soundbank soundbank;
	private transient boolean requested;
	private transient int lastGain = -1;
	private transient Sequencer sequencer;
	private transient Synthesizer synthesizer;
	private transient Receiver receiver;

	public CMIDISound(final DataSource assets, final String label, final Element row,
			final BooleanSupplier playbackAllowed) {
		this(assets, label, row, playbackAllowed, () -> 1);
	}

	public CMIDISound(final DataSource assets, final String label, final Element row,
			final BooleanSupplier playbackAllowed, final DoubleSupplier ambientVolumeScale) {
		this.assets = assets;
		this.label = label;
		this.playbackAllowed = playbackAllowed;
		this.ambientVolumeScale = ambientVolumeScale;
		final String directory = row == null ? "" : row.getField("DirectoryBase");
		this.sequencePath = assetPath(directory, row == null ? "" : row.getField("MIDIFileName"));
		this.soundbankPath = assetPath(directory, row == null ? "" : row.getField("DLSFileName"));
		this.looping = row != null && Arrays.asList(row.getField("Flags").split(",")).contains("LOOPING");
		setVolume(row == null ? 127 : row.getFieldValue("Volume"));
		setPitch(row == null ? 1 : row.getFieldFloatValue("Pitch"));
	}

	private static String assetPath(final String directory, final String file) {
		if (file.isEmpty() || "_".equals(file)) {
			return "";
		}
		if (directory.isEmpty() || directory.endsWith("\\") || directory.endsWith("/")) {
			return directory + file;
		}
		return directory + "\\" + file;
	}

	@Override
	public void start() {
		stop();
		this.requested = true;
		updatePlayback();
	}

	public boolean updatePlayback() {
		if (!this.requested || !this.playbackAllowed.getAsBoolean()) {
			return this.requested;
		}
		final float gain = effectiveGain();
		if (this.sequencer != null) {
			if (!this.sequencer.isRunning()) {
				stop();
			}
			else {
				applyGain(gain);
			}
		}
		else if (gain > 0) {
			openPlayback(gain);
		}
		return this.requested;
	}

	float effectiveGain() {
		return OptionsSettingsStore.get().isAmbientSounds()
				? AudioBufferSource.effectiveVolume(this.volume / 127f) * (float) this.ambientVolumeScale.getAsDouble()
				: 0;
	}

	private void openPlayback(final float gain) {
		loadAssets();
		if (this.sequence == null) {
			this.requested = false;
			return;
		}
		try {
			this.sequencer = MidiSystem.getSequencer(false);
			this.synthesizer = MidiSystem.getSynthesizer();
			this.synthesizer.open();
			if (this.soundbank != null && !this.synthesizer.loadAllInstruments(this.soundbank)) {
				throw new MidiUnavailableException("Cannot load the authored soundbank");
			}
			this.receiver = this.synthesizer.getReceiver();
			this.sequencer.open();
			this.sequencer.setSequence(this.sequence);
			this.sequencer.getTransmitter().setReceiver(this.receiver);
			this.sequencer.setTempoFactor(this.pitch);
			this.sequencer.setLoopCount(this.looping ? Sequencer.LOOP_CONTINUOUSLY : 0);
			applyGain(gain);
			this.sequencer.start();
		}
		catch (final MidiUnavailableException | InvalidMidiDataException | RuntimeException error) {
			stop();
			System.err.println("Unable to play MIDI sound '" + this.label + "': " + error);
		}
	}

	private void loadAssets() {
		if (this.loaded) {
			return;
		}
		this.loaded = true;
		try {
			try (InputStream stream = openAsset(this.sequencePath)) {
				this.sequence = MidiSystem.getSequence(stream);
			}
			if (!this.soundbankPath.isEmpty()) {
				try (InputStream stream = openAsset(this.soundbankPath)) {
					this.soundbank = MidiSoundbankReader.read(stream);
				}
			}
		}
		catch (final IOException | InvalidMidiDataException | RuntimeException error) {
			this.sequence = null;
			System.err.println("Unable to load MIDI sound '" + this.label + "': " + error);
		}
	}

	private InputStream openAsset(final String path) throws IOException {
		final InputStream stream = path.isEmpty() ? null : this.assets.getResourceAsStream(path);
		if (stream == null) {
			throw new IOException(path.isEmpty() ? "MIDI label has no sequence" : "Missing asset " + path);
		}
		return new BufferedInputStream(stream);
	}

	private void applyGain(final float gain) {
		final int level = Math.round(gain * 16383);
		if (level == this.lastGain) {
			return;
		}
		try {
			final byte[] message = { (byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, (byte) (level & 127),
					(byte) (level >> 7), (byte) 0xf7 };
			this.receiver.send(new SysexMessage(message, message.length), -1);
			this.lastGain = level;
		}
		catch (final InvalidMidiDataException error) {
			throw new IllegalStateException("Invalid MIDI master volume", error);
		}
	}

	@Override
	public void stop() {
		this.requested = false;
		this.lastGain = -1;
		if (this.sequencer != null) {
			this.sequencer.close();
			this.sequencer = null;
		}
		if (this.receiver != null) {
			this.receiver.close();
			this.receiver = null;
		}
		if (this.synthesizer != null) {
			this.synthesizer.close();
			this.synthesizer = null;
		}
	}

	@Override
	public void setVolume(final int volume) {
		this.volume = Math.max(0, Math.min(127, volume));
	}

	@Override
	public void setPitch(final float pitch) {
		this.pitch = Float.isFinite(pitch) && pitch > 0 ? Math.max(0.01f, Math.min(4, pitch)) : 1;
		if (this.sequencer != null && this.sequencer.isOpen()) {
			this.sequencer.setTempoFactor(this.pitch);
		}
	}

	@Override
	public void setPosition(final float x, final float y, final float z) {
		// MIDI ambience is a 2D mix.
	}

	@Override
	public void setPlayPosition(final int milliseconds) {
		final long microseconds = milliseconds * 1000L;
		if (microseconds >= 0 && isPlaying() && microseconds < this.sequencer.getMicrosecondLength()) {
			this.sequencer.setMicrosecondPosition(microseconds);
		}
	}

	@Override
	public boolean isPlaying() {
		return this.sequencer != null && this.sequencer.isRunning();
	}

	@Override
	public float getPredictedDuration() {
		loadAssets();
		return this.sequence == null ? 0 : this.sequence.getMicrosecondLength() / 1_000_000f;
	}

	@Override
	public float getRemainingTimeToPlayOnTheDesyncLocalComputer() {
		if (this.sequencer == null) {
			return 0;
		}
		return (this.sequencer.getMicrosecondLength() - this.sequencer.getMicrosecondPosition()) / 1_000_000f;
	}
}

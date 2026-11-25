package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

final class MovieAudioPlayback {
	private static final int SAMPLE_RATE = 44_100;
	private static final int CHANNELS = 2;
	private final String decoder;
	private final File file;
	private volatile boolean stopped;
	private volatile Process process;
	private volatile long submittedSamples;
	private Thread thread;

	MovieAudioPlayback(final String decoder, final File file) {
		this.decoder = decoder;
		this.file = file;
	}

	synchronized void start() {
		if (this.stopped || this.thread != null) return;
		this.thread = new Thread(this::play, "movie-audio");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	private void play() {
		Process decoderProcess = null;
		AudioDevice device = null;
		try {
			decoderProcess = new ProcessBuilder(this.decoder, "-v", "error", "-i", this.file.getAbsolutePath(),
					"-map", "0:a:0", "-vn", "-f", "s16le", "-acodec", "pcm_s16le", "-ar", Integer.toString(SAMPLE_RATE),
					"-ac", Integer.toString(CHANNELS), "-").redirectError(ProcessBuilder.Redirect.DISCARD).start();
			this.process = decoderProcess;
			if (this.stopped) return;
			device = Gdx.audio.newAudioDevice(SAMPLE_RATE, false);
			device.setVolume(OptionsSettingsStore.get().getEffectiveSoundVolume() / 100f);
			final byte[] bytes = new byte[4096];
			final short[] samples = new short[bytes.length / 2];
			final ByteBuffer pcm = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			try (var input = decoderProcess.getInputStream()) {
				while (!this.stopped) {
					final int read = input.readNBytes(bytes, 0, bytes.length);
					if (read < CHANNELS * 2 || this.stopped) break;
					final int sampleCount = (read / (CHANNELS * 2)) * CHANNELS;
					pcm.clear();
					pcm.asShortBuffer().get(samples, 0, sampleCount);
					device.writeSamples(samples, 0, sampleCount);
					this.submittedSamples += sampleCount;
				}
			}
		}
		catch (final IOException | RuntimeException error) {
			if (!this.stopped) System.err.println("Cinematic audio: " + error.getMessage());
		}
		finally {
			if (decoderProcess != null) decoderProcess.destroy();
			this.process = null;
			if (device != null) device.dispose();
		}
	}

	void stop() {
		final Thread worker;
		final Process active;
		synchronized (this) {
			this.stopped = true;
			active = this.process;
			worker = this.thread;
		}
		if (active != null) active.destroy();
		if (worker != null) {
			worker.interrupt();
			try {
				worker.join(350);
			}
			catch (final InterruptedException error) {
				Thread.currentThread().interrupt();
			}
		}
	}

	float getSubmittedSeconds() {
		return this.submittedSamples / (float) (SAMPLE_RATE * CHANNELS);
	}
}

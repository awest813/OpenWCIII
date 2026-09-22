package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retail campaign movies without any bundled codec.
 *
 * <p>
 * Classic Warcraft III cinematics are AVI files (stored with an {@code .mpq}
 * extension) using DivX video. There is no MIT-licensed pure-Java MPEG-4 Part 2
 * decoder to bundle, so this player shells out to a user-provided
 * {@code ffmpeg} binary — the same "bring your own" model the engine already
 * uses for Blizzard game assets — and decodes to raw frames through a pipe.
 * When no movie or no ffmpeg is available, callers keep the timed-overlay
 * fallback.
 *
 * <p>
 * This class is intentionally free of LibGDX types so the probing, candidate
 * resolution and queue logic stay unit-testable; texture upload and audio
 * output live with the caller ({@link MeleeUI}).
 */
public final class MoviePlayer {
	private static final Pattern DURATION_PATTERN = Pattern
			.compile("Duration:\\s*(\\d+):(\\d+):([\\d.]+)");
	private static final Pattern VIDEO_LINE_PATTERN = Pattern.compile("Stream.*Video:.*");
	private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d{2,5})x(\\d{2,5})");
	private static final Pattern FPS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*fps");
	private static final Pattern AUDIO_LINE_PATTERN = Pattern.compile("Stream.*Audio:.*");

	private MoviePlayer() {
	}

	/**
	 * Candidate data-source paths for a PlayCinematic argument, in lookup order.
	 * Campaign scripts pass a bare name, a Movies-relative path, or a full path,
	 * with or without an extension.
	 */
	public static List<String> candidatePaths(final String moviePath) {
		final List<String> candidates = new ArrayList<>();
		if ((moviePath == null) || moviePath.isEmpty()) {
			return candidates;
		}
		final String normalized = moviePath.replace('/', '\\');
		addIfMissing(candidates, normalized);

		String pathWithoutExt = normalized;
		final int dot = normalized.lastIndexOf('.');
		if (dot >= 0) {
			pathWithoutExt = normalized.substring(0, dot);
		}

		String base = normalized;
		final int slash = Math.max(base.lastIndexOf('\\'), base.lastIndexOf('/'));
		if (slash >= 0) {
			base = base.substring(slash + 1);
		}
		String baseWithoutExt = base;
		final int baseDot = base.lastIndexOf('.');
		if (baseDot >= 0) {
			baseWithoutExt = base.substring(0, baseDot);
		}

		// Try standard movie extensions for the path as given
		addIfMissing(candidates, pathWithoutExt + ".mpq");
		addIfMissing(candidates, pathWithoutExt + ".avi");
		addIfMissing(candidates, pathWithoutExt + ".mp4");

		// Try under Movies\ directory
		addIfMissing(candidates, "Movies\\" + base);
		addIfMissing(candidates, "Movies\\" + baseWithoutExt + ".mpq");
		addIfMissing(candidates, "Movies\\" + baseWithoutExt + ".avi");
		addIfMissing(candidates, "Movies\\" + baseWithoutExt + ".mp4");

		// Try bare base name
		addIfMissing(candidates, base);
		addIfMissing(candidates, baseWithoutExt + ".mpq");
		addIfMissing(candidates, baseWithoutExt + ".avi");
		addIfMissing(candidates, baseWithoutExt + ".mp4");

		return candidates;
	}

	private static void addIfMissing(final List<String> candidates, final String candidate) {
		if (!candidates.contains(candidate)) {
			candidates.add(candidate);
		}
	}

	/** Parses {@code Duration: HH:MM:SS.xx} from {@code ffmpeg -i} output. */
	public static double parseDurationSeconds(final String ffmpegOutput) {
		if (ffmpegOutput == null) {
			return -1;
		}
		final Matcher matcher = DURATION_PATTERN.matcher(ffmpegOutput);
		if (!matcher.find()) {
			return -1;
		}
		try {
			final double hours = Double.parseDouble(matcher.group(1));
			final double minutes = Double.parseDouble(matcher.group(2));
			final double seconds = Double.parseDouble(matcher.group(3));
			return (hours * 3600) + (minutes * 60) + seconds;
		}
		catch (final NumberFormatException e) {
			return -1;
		}
	}

	/** Parses the first video stream's frame rate from {@code ffmpeg -i} output. */
	public static double parseFps(final String ffmpegOutput) {
		if (ffmpegOutput == null) {
			return -1;
		}
		for (final String line : ffmpegOutput.split("\n")) {
			if (VIDEO_LINE_PATTERN.matcher(line).find()) {
				final Matcher matcher = FPS_PATTERN.matcher(line);
				if (matcher.find()) {
					try {
						return Double.parseDouble(matcher.group(1));
					}
					catch (final NumberFormatException e) {
						return -1;
					}
				}
				return -1;
			}
		}
		return -1;
	}

	/** Parses the first video stream's {@code WxH} from {@code ffmpeg -i} output. */
	public static int[] parseVideoSize(final String ffmpegOutput) {
		if (ffmpegOutput == null) {
			return null;
		}
		for (final String line : ffmpegOutput.split("\n")) {
			if (VIDEO_LINE_PATTERN.matcher(line).find()) {
				final Matcher matcher = SIZE_PATTERN.matcher(line);
				if (matcher.find()) {
					try {
						return new int[] { Integer.parseInt(matcher.group(1)),
								Integer.parseInt(matcher.group(2)) };
					}
					catch (final NumberFormatException e) {
						return null;
					}
				}
				return null;
			}
		}
		return null;
	}

	/** Reports whether {@code ffmpeg -i} output lists an audio stream. */
	public static boolean hasAudioStream(final String ffmpegOutput) {
		if (ffmpegOutput == null) {
			return false;
		}
		for (final String line : ffmpegOutput.split("\n")) {
			if (AUDIO_LINE_PATTERN.matcher(line).find()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Locates an ffmpeg binary: {@code -Dwarsmash.ffmpeg=} first, then the
	 * {@code WARSMASH_FFMPEG} environment variable, then {@code ffmpeg} on the
	 * PATH. Returns null when none is usable.
	 */
	public static String findFfmpeg() {
		final String override = System.getProperty("warsmash.ffmpeg");
		if ((override != null) && !override.isEmpty() && canRun(override)) {
			return override;
		}
		final String env = System.getenv("WARSMASH_FFMPEG");
		if ((env != null) && !env.isEmpty() && canRun(env)) {
			return env;
		}
		if (canRun("ffmpeg")) {
			return "ffmpeg";
		}
		return null;
	}

	private static boolean canRun(final String binary) {
		try {
			final Process process = new ProcessBuilder(binary, "-version").redirectErrorStream(true).start();
			final byte[] buffer = new byte[4096];
			while (process.getInputStream().read(buffer) != -1) {
				// drain
			}
			return process.waitFor() == 0;
		}
		catch (final IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/** Runs {@code ffmpeg -i} and returns its combined output for probing. */
	public static String probeOutput(final String ffmpeg, final File movieFile) {
		try {
			final Process process = new ProcessBuilder(ffmpeg, "-hide_banner", "-i", movieFile.getAbsolutePath())
					.redirectErrorStream(true).start();
			final StringBuilder output = new StringBuilder();
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = process.getInputStream().read(buffer)) != -1) {
				output.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
			}
			process.waitFor();
			return output.toString();
		}
		catch (final IOException | InterruptedException e) {
			Thread.currentThread().interrupt();
			return "";
		}
	}

	/**
	 * A running rawvideo decode. Frames are RGB888 top-row-first byte arrays;
	 * the caller uploads whichever frame is due and drops the rest.
	 */
	public static final class Session {
		private static final int QUEUE_CAPACITY = 16;

		private final Process process;
		private final Thread readerThread;
		private final BlockingQueue<byte[]> frames = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
		private final int width;
		private final int height;
		private final double fps;
		private final double durationSeconds;
		private volatile boolean finished;

		private Session(final Process process, final int width, final int height, final double fps,
				final double durationSeconds) {
			this.process = process;
			this.width = width;
			this.height = height;
			this.fps = fps;
			this.durationSeconds = durationSeconds;
			final int frameBytes = width * height * 3;
			this.readerThread = new Thread(() -> readLoop(frameBytes), "movie-decode");
			this.readerThread.setDaemon(true);
			this.readerThread.start();
		}

		/**
		 * Starts decoding {@code movieFile} to raw RGB frames, or returns null
		 * when ffmpeg cannot be launched.
		 */
		public static Session start(final String ffmpeg, final File movieFile, final int width,
				final int height, final double fps, final double durationSeconds) {
			try {
				final ProcessBuilder builder = new ProcessBuilder(ffmpeg, "-v", "error", "-i",
						movieFile.getAbsolutePath(), "-map", "0:v:0", "-f", "rawvideo", "-pix_fmt", "rgb24",
						"-s", width + "x" + height, "-r", Double.toString(fps), "-");
				builder.redirectError(ProcessBuilder.Redirect.DISCARD);
				final Process process = builder.start();
				return new Session(process, width, height, fps, durationSeconds);
			}
			catch (final IOException e) {
				System.err.println("MoviePlayer: could not start ffmpeg: " + e.getMessage());
				return null;
			}
		}

		private void readLoop(final int frameBytes) {
			final InputStream stdout = this.process.getInputStream();
			try {
				while (!Thread.currentThread().isInterrupted()) {
					final byte[] frame = new byte[frameBytes];
					if (!readFully(stdout, frame)) {
						break;
					}
					try {
						this.frames.put(frame);
					}
					catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
			catch (final IOException e) {
				// process died or was stopped; fall through to finished
			}
			finally {
				this.finished = true;
				try {
					stdout.close();
				}
				catch (final IOException ignored) {
				}
			}
		}

		private static boolean readFully(final InputStream in, final byte[] buffer) throws IOException {
			int offset = 0;
			while (offset < buffer.length) {
				final int read = in.read(buffer, offset, buffer.length - offset);
				if (read == -1) {
					return false;
				}
				offset += read;
			}
			return true;
		}

		/** Returns the oldest queued frame, or null when none is available. */
		public byte[] pollFrame() {
			return this.frames.poll();
		}

		public boolean isFinished() {
			return this.finished && this.frames.isEmpty();
		}

		public int getWidth() {
			return this.width;
		}

		public int getHeight() {
			return this.height;
		}

		public double getFps() {
			return this.fps;
		}

		public double getDurationSeconds() {
			return this.durationSeconds;
		}

		public void stop() {
			this.readerThread.interrupt();
			this.process.destroy();
			this.frames.clear();
		}
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MoviePlayerTest {
	private static final String FFMPEG_PROBE = "ffmpeg version 6.1.1\n"
			+ "Input #0, avi, from 'OrcIntro.mpq':\n" + "  Duration: 00:01:47.20, start: 0.000000, bitrate: 1500 kb/s\n"
			+ "  Stream #0:0: Video: mpeg4 (DIVX / 0x58564944), yuv420p, 640x480 [SAR 1:1 DAR 4:3], 1350 kb/s, 29.97 fps, 29.97 tbr, 29.97 tbn\n"
			+ "  Stream #0:1: Audio: mp3 (U[0][0][0] / 0x0055), 44100 Hz, stereo, fltp, 128 kb/s\n";

	@Test
	void parsesDuration() {
		assertEquals(107.2, MoviePlayer.parseDurationSeconds(FFMPEG_PROBE), 0.001);
		assertEquals(-1, MoviePlayer.parseDurationSeconds("no duration here"));
		assertEquals(-1, MoviePlayer.parseDurationSeconds(null));
	}

	@Test
	void parsesFpsAndSizeFromFirstVideoStream() {
		assertEquals(29.97, MoviePlayer.parseFps(FFMPEG_PROBE), 0.001);
		final int[] size = MoviePlayer.parseVideoSize(FFMPEG_PROBE);
		assertEquals(640, size[0]);
		assertEquals(480, size[1]);
		assertNull(MoviePlayer.parseVideoSize("no streams here"));
	}

	@Test
	void detectsAudioStream() {
		assertTrue(MoviePlayer.hasAudioStream(FFMPEG_PROBE));
		assertFalse(MoviePlayer.hasAudioStream("Stream #0:0: Video: mpeg4"));
		assertFalse(MoviePlayer.hasAudioStream(null));
	}

	@Test
	void buildsLookupCandidatesInOrder() {
		final List<String> candidates = MoviePlayer.candidatePaths("OrcIntro");
		assertEquals("OrcIntro", candidates.get(0));
		assertTrue(candidates.contains("OrcIntro.mpq"));
		assertTrue(candidates.contains("OrcIntro.avi"));
		assertTrue(candidates.contains("OrcIntro.mp4"));
		assertTrue(candidates.contains("Movies\\OrcIntro.mpq"));
		assertTrue(candidates.contains("Movies\\OrcIntro.avi"));
		assertTrue(candidates.contains("Movies\\OrcIntro.mp4"));
	}

	@Test
	void fullPathsAreNotPrefixedTwice() {
		final List<String> candidates = MoviePlayer.candidatePaths("Movies\\HumanIntro.mpq");
		assertEquals("Movies\\HumanIntro.mpq", candidates.get(0));
		assertEquals(1, candidates.stream().filter("Movies\\HumanIntro.mpq"::equals).count());
		assertTrue(candidates.contains("Movies\\HumanIntro.avi"));
		assertTrue(candidates.contains("Movies\\HumanIntro.mp4"));
		assertTrue(candidates.contains("HumanIntro.mpq"));
	}

	@Test
	void aviInputResolvesMpqAndMp4() {
		final List<String> candidates = MoviePlayer.candidatePaths("Movies\\NightElfIntro.avi");
		assertEquals("Movies\\NightElfIntro.avi", candidates.get(0));
		assertTrue(candidates.contains("Movies\\NightElfIntro.mpq"));
		assertTrue(candidates.contains("Movies\\NightElfIntro.mp4"));
		assertTrue(candidates.contains("NightElfIntro.avi"));
	}

	@Test
	void forwardSlashesAreNormalized() {
		final List<String> candidates = MoviePlayer.candidatePaths("Movies/UndeadIntro.mpq");
		assertEquals("Movies\\UndeadIntro.mpq", candidates.get(0));
		assertTrue(candidates.contains("Movies\\UndeadIntro.avi"));
	}

	@Test
	void emptyPathYieldsNoCandidates() {
		assertTrue(MoviePlayer.candidatePaths("").isEmpty());
		assertTrue(MoviePlayer.candidatePaths(null).isEmpty());
	}
}

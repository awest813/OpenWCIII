package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hiveworkshop.rms.parsers.mdlx.MdlxModel;

import mpq.ArchivedFile;
import mpq.ArchivedFileExtractor;
import mpq.ArchivedFileStream;
import mpq.HashLookup;
import mpq.MPQArchive;

/**
 * Validates the cinematic assets retail campaign scripts reference against real
 * disc data when it is present (skipped otherwise). Every {@code SetSkyModel}
 * path and the single {@code PlayModelCinematic} path from the 85 retail
 * scripts must exist and parse with the structure the engine relies on (sky:
 * looping sequence + geosets; model cinematic: cameras + sequences + geosets).
 */
class CinematicRetailDataTest {
	private static final String[] ARCHIVES = { "F:\\WC3Data\\war3.mpq", "F:\\WC3Data\\War3x.mpq",
			"F:\\WC3Data\\War3xlocal.mpq", };

	private static final String[] SKY_MODELS = { "Environment\\Sky\\BlizzardSky\\BlizzardSky.mdl",
			"Environment\\Sky\\DalaranSky\\DalaranSky.mdl", "Environment\\Sky\\FelwoodSky\\FelwoodSky.mdl",
			"Environment\\Sky\\FoggedSky\\FoggedSky.mdl", "Environment\\Sky\\LordaeronFallSky\\LordaeronFallSky.mdl",
			"Environment\\Sky\\LordaeronSummerSky\\LordaeronSummerSky.mdl",
			"Environment\\Sky\\LordaeronWinterSkyBrightGreen\\LordaeronWinterSkyBrightGreen.mdl",
			"Environment\\Sky\\LordaeronWinterSkyPink\\LordaeronWinterSkyPink.mdl",
			"Environment\\Sky\\LordaeronWinterSkyRed\\LordaeronWinterSkyRed.mdl",
			"Environment\\Sky\\LordaeronWinterSkyYellow\\LordaeronWinterSkyYellow.mdl",
			"Environment\\Sky\\LordaeronWinterSky\\LordaeronWinterSky.mdl",
			"Environment\\Sky\\Outland_Sky\\Outland_Sky.mdl", "Environment\\Sky\\Sky\\SkyLight.mdl", };

	private static final String FIGHT_MODEL = "Doodads\\Cinematic\\ArthasIllidanFight\\ArthasIllidanFight.mdl";

	@Test
	void skyModelsParseIfPresent() throws Exception {
		final List<OpenArchive> open = openArchives();
		if (open.isEmpty()) {
			return;
		}
		try {
			for (final String path : SKY_MODELS) {
				final byte[] data = readAny(open, path);
				assertNotNull(data, "Sky model missing from retail data: " + path);
				final MdlxModel model = parse(data, path);
				assertTrue(model.sequences.size() >= 1, path + " has no sequences");
				assertTrue(model.geosets.size() >= 1, path + " has no geosets");
			}
		}
		finally {
			close(open);
		}
	}

	@Test
	void modelCinematicParsesIfPresent() throws Exception {
		final List<OpenArchive> open = openArchives();
		if (open.isEmpty()) {
			return;
		}
		try {
			final byte[] data = readAny(open, FIGHT_MODEL);
			assertNotNull(data, "Model cinematic missing from retail data: " + FIGHT_MODEL);
			final MdlxModel model = parse(data, FIGHT_MODEL);
			assertTrue(model.sequences.size() >= 1, FIGHT_MODEL + " has no sequences");
			assertTrue(model.cameras.size() >= 1, FIGHT_MODEL + " has no cameras");
			assertTrue(model.geosets.size() >= 1, FIGHT_MODEL + " has no geosets");
		}
		finally {
			close(open);
		}
	}

	private static MdlxModel parse(final byte[] data, final String path) {
		try {
			return new MdlxModel(ByteBuffer.wrap(data));
		}
		catch (final Exception e) {
			fail("Failed to parse retail model " + path + ": " + e.getMessage());
			return null;
		}
	}

	private static List<OpenArchive> openArchives() throws Exception {
		final List<OpenArchive> open = new ArrayList<>();
		for (final String archive : ARCHIVES) {
			if (Files.exists(Paths.get(archive))) {
				final SeekableByteChannel channel = Files.newByteChannel(Paths.get(archive),
						StandardOpenOption.READ);
				open.add(new OpenArchive(new MPQArchive(channel), channel));
			}
		}
		return open;
	}

	private static void close(final List<OpenArchive> open) throws Exception {
		for (final OpenArchive o : open) {
			o.channel.close();
		}
	}

	private static byte[] readAny(final List<OpenArchive> open, final String path) {
		byte[] found = null;
		final ArchivedFileExtractor extractor = new ArchivedFileExtractor();
		for (final OpenArchive o : open) {
			final byte[] data = read(o.mpq, o.channel, extractor, path);
			if (data != null) {
				found = data;
			}
			else {
				// Retail scripts reference .mdl source names; archives carry compiled .mdx.
				final String swapped = swapMdxMdl(path);
				if (swapped != null) {
					final byte[] alt = read(o.mpq, o.channel, extractor, swapped);
					if (alt != null) {
						found = alt;
					}
				}
			}
		}
		return found;
	}

	private static String swapMdxMdl(final String path) {
		final String lower = path.toLowerCase();
		if (lower.endsWith(".mdl")) {
			return path.substring(0, path.length() - 4) + ".mdx";
		}
		if (lower.endsWith(".mdx")) {
			return path.substring(0, path.length() - 4) + ".mdl";
		}
		return null;
	}

	private static byte[] read(final MPQArchive mpq, final SeekableByteChannel channel,
			final ArchivedFileExtractor extractor, final String path) {
		try {
			final ArchivedFile file = mpq.lookupHash2(new HashLookup(path));
			try (ArchivedFileStream stream = new ArchivedFileStream(channel, extractor, file)) {
				final ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) stream.size());
				final ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
				int n;
				while ((n = stream.read(buffer)) > 0) {
					bytes.write(buffer.array(), 0, n);
					buffer.clear();
				}
				return bytes.toByteArray();
			}
		}
		catch (final Exception e) {
			return null;
		}
	}

	private static final class OpenArchive {
		final MPQArchive mpq;
		final SeekableByteChannel channel;

		OpenArchive(final MPQArchive mpq, final SeekableByteChannel channel) {
			this.mpq = mpq;
			this.channel = channel;
		}
	}
}

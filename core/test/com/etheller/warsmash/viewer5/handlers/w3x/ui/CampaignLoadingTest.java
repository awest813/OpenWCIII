package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.jass.JassAIEnvironment;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.units.custom.WTS;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;

import mpq.MPQArchive;

class CampaignLoadingTest {

	@Test
	void testStringWithWtsSafety() throws Exception {
		final Method method = MenuUI.class.getDeclaredMethod("getStringWithWTS", WTS.class, String.class);
		method.setAccessible(true);

		// Null string returns empty string safely
		assertEquals("", method.invoke(null, null, null));

		// Plain text without TRIGSTR prefix returned unchanged
		assertEquals("Hello Campaign", method.invoke(null, null, "Hello Campaign"));

		// Null WTS with TRIGSTR returns trigstr string as-is without NPE
		assertEquals("TRIGSTR_001", method.invoke(null, null, "TRIGSTR_001"));

		// Malformed TRIGSTR returns trigstr string as-is without NumberFormatException
		assertEquals("TRIGSTR_abc", method.invoke(null, null, "TRIGSTR_abc"));

		// Valid WTS resolves string
		final WTS wts = key -> (key == 1 ? "The Defense of Strahnbrad" : null);
		assertEquals("The Defense of Strahnbrad", method.invoke(null, wts, "TRIGSTR_001"));
	}

	@Test
	void testForceCampaignSelectScreenLifecycle() {
		final CampaignProgressStore store = CampaignProgressStore.get();
		store.reset();
		assertFalse(store.consumeForceCampaignSelectScreen());

		store.forceCampaignSelectScreen();
		assertTrue(store.consumeForceCampaignSelectScreen());
		assertFalse(store.consumeForceCampaignSelectScreen());
	}

	@Test
	void testBeginLoadingMapNonExistentThrowsIllegalArgument() {
		final DataSource emptySource = new CompoundDataSource(Collections.emptyList());
		assertThrows(IllegalArgumentException.class, () -> {
			War3MapViewer.beginLoadingMap(emptySource, "NonExistentMap.w3x");
		});
	}

	@Test
	void testMapPathNormalizationOnDiscDataIfPresent() throws Exception {
		final String war3MpqPath = "F:\\WC3Data\\war3.mpq";
		final File mpqFile = new File(war3MpqPath);
		if (!mpqFile.exists()) {
			return; // Skip if retail archive not present in test environment
		}

		try (SeekableByteChannel channel = Files.newByteChannel(Paths.get(war3MpqPath), StandardOpenOption.READ)) {
			final MPQArchive mpqArchive = new MPQArchive(channel);
			final MpqDataSource dataSource = new MpqDataSource(mpqArchive, channel);
			final CompoundDataSource compound = new CompoundDataSource(Collections.singletonList(dataSource));

			// Verify backslash lookup
			final War3Map mapBackslash = War3MapViewer.beginLoadingMap(compound, "Maps\\Campaign\\Human01.w3x");
			assertNotNull(mapBackslash);
			final War3MapW3i infoBackslash = mapBackslash.readMapInformation();
			assertNotNull(infoBackslash);

			// Verify forward slash normalization resolves the same map
			final War3Map mapForwardSlash = War3MapViewer.beginLoadingMap(compound, "Maps/Campaign/Human01.w3x");
			assertNotNull(mapForwardSlash);
			final War3MapW3i infoForwardSlash = mapForwardSlash.readMapInformation();
			assertNotNull(infoForwardSlash);
			assertEquals(infoBackslash.getLoadingScreenTitle(), infoForwardSlash.getLoadingScreenTitle());

			// Verify relative Maps\Campaign prefix fallback
			final War3Map mapBare = War3MapViewer.beginLoadingMap(compound, "Human01.w3x");
			assertNotNull(mapBare);
			final War3MapW3i infoBare = mapBare.readMapInformation();
			assertNotNull(infoBare);
			assertEquals(infoBackslash.getLoadingScreenTitle(), infoBare.getLoadingScreenTitle());
		}
	}

	@Test
	void testCampaignAILoadingOnDiscDataIfPresent() throws Exception {
		final String war3MpqPath = "F:\\WC3Data\\war3.mpq";
		final File mpqFile = new File(war3MpqPath);
		if (!mpqFile.exists()) {
			return;
		}

		try (SeekableByteChannel channel = Files.newByteChannel(Paths.get(war3MpqPath), StandardOpenOption.READ)) {
			final MPQArchive mpqArchive = new MPQArchive(channel);
			final MpqDataSource dataSource = new MpqDataSource(mpqArchive, channel);
			final CompoundDataSource compound = new CompoundDataSource(Collections.singletonList(dataSource));

			// Check if common.ai exists
			assertTrue(compound.has("Scripts\\common.ai"));

			// Audit natives in common.ai
			final java.util.Set<String> aiNatives = new java.util.TreeSet<>();
			try (java.io.InputStream in = compound.getResourceAsStream("Scripts\\common.ai");
					java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
				String line;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("native ") || line.startsWith("constant native ")) {
						final String[] parts = line.split("\\s+");
						for (int i = 0; i < parts.length; i++) {
							if (parts[i].equals("native") && i + 1 < parts.length) {
								aiNatives.add(parts[i + 1]);
								break;
							}
						}
					}
				}
			}
			System.out.println("Natives in common.ai count: " + aiNatives.size());
			System.out.println("Natives in common.ai: " + aiNatives);

			// Open NightElf01 map
			final War3Map map = War3MapViewer.beginLoadingMap(compound, "Maps\\Campaign\\NightElf01.w3m");
			assertNotNull(map);

			// Check map's CompoundDataSource
			final CompoundDataSource mapCompound = new CompoundDataSource(
					java.util.Arrays.asList(map, compound));

			// Read war3map.j and find StartCampaignAI calls
			try (java.io.InputStream in = map.getResourceAsStream("war3map.j");
					java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains("StartCampaignAI") || line.contains("StartMeleeAI")) {
						System.out.println("AI CALL: " + line.trim());
						// Extract script string between quotes
						int firstQuote = line.indexOf('"');
						int secondQuote = line.indexOf('"', firstQuote + 1);
						if (firstQuote >= 0 && secondQuote > firstQuote) {
							String script = line.substring(firstQuote + 1, secondQuote);
							System.out.println("Has script in mapCompound: " + script + " -> " + mapCompound.has(script));
							final JassAIEnvironment ai = JassAIEnvironment.loadAI(mapCompound, null, null, null, null, null, 1, script);
							System.out.println("AI loaded for " + script + ": " + (ai != null));
							assertNotNull(ai, "AI should load for " + script);
						}
					}
				}
			}
		}
	}

	@Test
	void testConfigPlayerStateRetention() {
		final com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfigPlayer configPlayer =
				new com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfigPlayer(1);

		// Default state is 0 / false
		assertEquals(0, configPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.ALLIED_VICTORY));
		assertEquals(0, configPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.GIVES_BOUNTY));

		// Set player state (e.g. during map config / InitCustomTeams)
		configPlayer.setPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.ALLIED_VICTORY, 1);
		configPlayer.setPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.GIVES_BOUNTY, 1);
		assertEquals(1, configPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.ALLIED_VICTORY));
		assertEquals(1, configPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.GIVES_BOUNTY));

		// Construct CPlayer using configPlayer and verify retention
		final com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer simPlayer =
				new com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer(
						new com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRace(1),
						new float[] { 100f, 200f }, configPlayer, null);

		assertEquals(1, simPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.ALLIED_VICTORY));
		assertEquals(1, simPlayer.getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.GIVES_BOUNTY));
		assertTrue(simPlayer.isAlliedVictory());
		assertTrue(simPlayer.isGivesBounty());
	}

	@Test
	void testAllCampaignMapsLoadingAndAIScripts() throws Exception {
		final java.util.List<java.nio.file.Path> mpqPaths = new java.util.ArrayList<>();
		for (final String pathStr : new String[] { "F:\\WC3Data\\war3.mpq", "F:\\WC3Data\\War3x.mpq", "F:\\WC3Data\\War3xlocal.mpq" }) {
			final java.nio.file.Path p = Paths.get(pathStr);
			if (Files.exists(p)) {
				mpqPaths.add(p);
			}
		}
		if (mpqPaths.isEmpty()) {
			return; // Skip if disc data not available
		}

		final java.util.List<MpqDataSource> mpqSources = new java.util.ArrayList<>();
		try {
			for (final java.nio.file.Path p : mpqPaths) {
				final SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ);
				final MPQArchive mpq = new MPQArchive(ch);
				mpqSources.add(new MpqDataSource(mpq, ch));
			}
			final CompoundDataSource compound = new CompoundDataSource(new java.util.ArrayList<>(mpqSources));

			// Collect all campaign map paths from archives
			final java.util.Set<String> campaignMaps = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			for (final MpqDataSource source : mpqSources) {
				final java.util.Collection<String> listfile = source.getListfile();
				if (listfile != null) {
					for (final String entry : listfile) {
						final String trimmed = entry.trim();
						final String lower = trimmed.toLowerCase();
						if ((lower.endsWith(".w3m") || lower.endsWith(".w3x")) && lower.contains("campaign")) {
							campaignMaps.add(trimmed);
						}
					}
				}
			}

			System.out.println("Discovered campaign maps: " + campaignMaps.size());
			int mapsTested = 0;
			int aiScriptsTested = 0;

			for (final String mapPath : campaignMaps) {
				War3Map map = null;
				try {
					map = War3MapViewer.beginLoadingMap(compound, mapPath);
				}
				catch (final Exception e) {
					System.err.println("Failed to load map: " + mapPath + " -> " + e.getMessage());
					throw e;
				}
				assertNotNull(map, "Map should load: " + mapPath);
				final War3MapW3i w3i = map.readMapInformation();
				assertNotNull(w3i, "W3I should load for: " + mapPath);
				mapsTested++;

				if (map.has("war3map.j")) {
					final CompoundDataSource mapCompound = new CompoundDataSource(
							java.util.Arrays.asList(map, compound));
					try (java.io.InputStream in = map.getResourceAsStream("war3map.j");
							java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in))) {
						String line;
						while ((line = reader.readLine()) != null) {
							if (line.contains("StartCampaignAI") || line.contains("StartMeleeAI")) {
								int firstQuote = line.indexOf('"');
								int secondQuote = line.indexOf('"', firstQuote + 1);
								if (firstQuote >= 0 && secondQuote > firstQuote) {
									String script = line.substring(firstQuote + 1, secondQuote);
									final JassAIEnvironment ai = JassAIEnvironment.loadAI(
											mapCompound, null, null, null, null, null, 1, script);
									assertNotNull(ai, "AI script should load: " + script + " in map " + mapPath);
									aiScriptsTested++;
								}
							}
						}
					}
				}
			}
			System.out.println("Total campaign maps tested: " + mapsTested);
			System.out.println("Total campaign AI scripts verified: " + aiScriptsTested);
			assertTrue(mapsTested > 0, "Should have tested campaign maps");
		}
		finally {
			for (final MpqDataSource source : mpqSources) {
				try {
					source.close();
				}
				catch (final Exception ignored) {}
			}
		}
	}
}



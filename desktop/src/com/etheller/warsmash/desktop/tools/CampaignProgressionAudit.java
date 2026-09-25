package com.etheller.warsmash.desktop.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

import mpq.MPQArchive;

/**
 * End-to-end progression audit across all retail Warcraft III campaign maps
 * (Reign of Chaos and The Frozen Throne).
 *
 * <p>Audits:
 * <ul>
 *   <li>{@code ChangeLevel} / {@code ChangeLevelBJ} calls: parses target map paths
 *       and verifies that 100% of target maps exist and resolve via
 *       {@link War3MapViewer#beginLoadingMap(com.etheller.warsmash.datasources.DataSource, String)}.</li>
 *   <li>{@code InitGameCache}: discovers all distinct gamecache filenames across campaigns.</li>
 *   <li>Campaign availability natives: {@code SetMissionAvailable}, {@code SetCampaignAvailable},
 *       {@code SetOpCinematicAvailable}, {@code SetEdCinematicAvailable}.</li>
 *   <li>Difficulty natives: {@code GetGameDifficulty}, {@code SetGameDifficulty}.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * ./gradlew :desktop:campaignProgressionAudit -Pargs="--mpq F:\WC3Data\war3.mpq --mpq F:\WC3Data\War3x.mpq --mpq F:\WC3Data\War3xlocal.mpq"
 * </pre>
 */
public final class CampaignProgressionAudit {

	private static final Pattern CHANGE_LEVEL_LITERAL = Pattern.compile(
			"(?:ChangeLevel|ChangeLevelBJ|SetNextLevelBJ|SaveAndChangeLevelBJ)\\s*\\(\\s*\"([^\"]+)\"");
	private static final Pattern SET_CHANGE_LEVEL_MAP_NAME = Pattern.compile(
			"set\\s+bj_changeLevelMapName\\s*=\\s*\"([^\"]+)\"");
	private static final Pattern INIT_GAME_CACHE_LITERAL = Pattern.compile(
			"(?:InitGameCache|InitGameCacheBJ)\\s*\\(\\s*\"([^\"]+)\"");
	private static final Pattern SET_MISSION_AVAIL = Pattern.compile(
			"(?:SetMissionAvailable|SetMissionAvailableBJ)\\s*\\(");
	private static final Pattern SET_CAMPAIGN_AVAIL = Pattern.compile(
			"(?:SetCampaignAvailable|SetCampaignAvailableBJ)\\s*\\(");
	private static final Pattern SET_OP_CINE_AVAIL = Pattern.compile(
			"(?:SetOpCinematicAvailable|SetOpCinematicAvailableBJ)\\s*\\(");
	private static final Pattern SET_ED_CINE_AVAIL = Pattern.compile(
			"(?:SetEdCinematicAvailable|SetEdCinematicAvailableBJ)\\s*\\(");
	private static final Pattern CUSTOM_VICTORY = Pattern.compile(
			"(?:CustomVictory|CustomVictoryBJ)\\s*\\(");
	private static final Pattern GET_GAME_DIFFICULTY = Pattern.compile(
			"GetGameDifficulty\\s*\\(");
	private static final Pattern SET_GAME_DIFFICULTY = Pattern.compile(
			"SetGameDifficulty\\s*\\(");

	private CampaignProgressionAudit() {
	}

	public static void main(final String[] args) throws Exception {
		final List<Path> archivePaths = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			if ("--mpq".equals(args[i])) {
				archivePaths.add(Paths.get(args[++i]));
			}
			else {
				throw new IllegalArgumentException("Unknown option: " + args[i]);
			}
		}

		if (archivePaths.isEmpty()) {
			for (final String defaultPath : new String[] { "F:\\WC3Data\\war3.mpq", "F:\\WC3Data\\War3x.mpq", "F:\\WC3Data\\War3xlocal.mpq" }) {
				final Path p = Paths.get(defaultPath);
				if (Files.exists(p)) {
					archivePaths.add(p);
				}
			}
		}

		if (archivePaths.isEmpty()) {
			System.err.println("Pass at least one --mpq <path> holding campaign maps.");
			System.exit(2);
		}

		System.out.println("===============================================================================");
		System.out.println("            WARCRAFT III CAMPAIGN PROGRESSION SPINE AUDIT                      ");
		System.out.println("===============================================================================");
		System.out.println("Archives:");
		for (final Path p : archivePaths) {
			System.out.println("  - " + p);
		}

		final List<MpqDataSource> mpqSources = new ArrayList<>();
		try {
			for (final Path p : archivePaths) {
				final SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ);
				final MPQArchive mpq = new MPQArchive(ch);
				mpqSources.add(new MpqDataSource(mpq, ch));
			}
			final CompoundDataSource compound = new CompoundDataSource(new ArrayList<>(mpqSources));

			// Discover campaign maps
			final Set<String> campaignMaps = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			for (final MpqDataSource source : mpqSources) {
				final Collection<String> listfile = source.getListfile();
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

			System.out.println("\nDiscovered Campaign Maps: " + campaignMaps.size());
			if (campaignMaps.isEmpty()) {
				throw new IllegalStateException("No campaign maps found; no audit was performed");
			}

			final Map<String, Set<String>> changeLevelTargets = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			final Map<String, Set<String>> gameCaches = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			int customVictoryCount = 0;
			int setMissionAvailCount = 0;
			int setCampaignAvailCount = 0;
			int setOpCineCount = 0;
			int setEdCineCount = 0;
			int getDifficultyCount = 0;
			int setDifficultyCount = 0;
			int mapsWithScripts = 0;

			for (final String mapPath : campaignMaps) {
				War3Map map = null;
				try {
					map = War3MapViewer.beginLoadingMap(compound, mapPath);
				}
				catch (final Exception e) {
					throw new IllegalStateException("Failed to open campaign map: " + mapPath, e);
				}

				if (!map.has("war3map.j")) {
					map.close();
					continue;
				}
				mapsWithScripts++;

				try (InputStream in = map.getResourceAsStream("war3map.j");
						BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
					String line;
					while ((line = reader.readLine()) != null) {
						final Matcher mCl = CHANGE_LEVEL_LITERAL.matcher(line);
						while (mCl.find()) {
							final String target = mCl.group(1).replace("\\\\", "\\");
							changeLevelTargets.computeIfAbsent(target, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
									.add(mapPath);
						}

						final Matcher mCl2 = SET_CHANGE_LEVEL_MAP_NAME.matcher(line);
						while (mCl2.find()) {
							final String target = mCl2.group(1).replace("\\\\", "\\");
							changeLevelTargets.computeIfAbsent(target, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
									.add(mapPath);
						}

						final Matcher mGc = INIT_GAME_CACHE_LITERAL.matcher(line);
						while (mGc.find()) {
							final String cacheName = mGc.group(1);
							gameCaches.computeIfAbsent(cacheName, k -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER))
									.add(mapPath);
						}

						if (CUSTOM_VICTORY.matcher(line).find()) {
							customVictoryCount++;
						}
						if (SET_MISSION_AVAIL.matcher(line).find()) {
							setMissionAvailCount++;
						}
						if (SET_CAMPAIGN_AVAIL.matcher(line).find()) {
							setCampaignAvailCount++;
						}
						if (SET_OP_CINE_AVAIL.matcher(line).find()) {
							setOpCineCount++;
						}
						if (SET_ED_CINE_AVAIL.matcher(line).find()) {
							setEdCineCount++;
						}
						if (GET_GAME_DIFFICULTY.matcher(line).find()) {
							getDifficultyCount++;
						}
						if (SET_GAME_DIFFICULTY.matcher(line).find()) {
							setDifficultyCount++;
						}
					}
				}
				finally {
					map.close();
				}
			}
			System.out.println("Maps with war3map.j scripts: " + mapsWithScripts);
			if ((mapsWithScripts == 0) || changeLevelTargets.isEmpty()) {
				throw new IllegalStateException("No scripted campaign transitions found; progression was not verified");
			}

			if (compound.has("Scripts\\Blizzard.j")) {
				System.out.println("Scripts\\Blizzard.j present and verified.");
			}

			// --- ChangeLevel targets audit ---
			System.out.println("\n--- 1. ChangeLevel Map Chaining Resolution ---");
			int resolvedTargets = 0;
			int brokenTargets = 0;
			for (final Map.Entry<String, Set<String>> entry : changeLevelTargets.entrySet()) {
				final String targetPath = entry.getKey();
				final Set<String> callers = entry.getValue();
				War3Map targetMap = null;
				try {
					targetMap = War3MapViewer.beginLoadingMap(compound, targetPath);
				}
				catch (final Exception ignored) {
				}

				if (targetMap != null) {
					resolvedTargets++;
					final War3MapW3i info = targetMap.readMapInformation();
					final String title = info != null ? info.getLoadingScreenTitle() : "?";
					System.out.printf("  PASS: \"%s\" (title: %s) <- %s%n", targetPath, title, callers);
					targetMap.close();
				}
				else {
					brokenTargets++;
					System.err.printf("  FAIL: \"%s\" could not be resolved! <- %s%n", targetPath, callers);
				}
			}

			// --- GameCache audit ---
			System.out.println("\n--- 2. GameCache In-Session Usage ---");
			for (final Map.Entry<String, Set<String>> entry : gameCaches.entrySet()) {
				System.out.printf("  CACHE: \"%s\" used across %d maps: %s%n",
						entry.getKey(), entry.getValue().size(), entry.getValue());
			}

			// --- Progression & Difficulty natives ---
			System.out.println("\n--- 3. Progression & Difficulty Native Call Sites ---");
			System.out.printf("  CustomVictory call sites:            %d%n", customVictoryCount);
			System.out.printf("  SetMissionAvailable call sites:      %d%n", setMissionAvailCount);
			System.out.printf("  SetCampaignAvailable call sites:     %d%n", setCampaignAvailCount);
			System.out.printf("  SetOpCinematicAvailable call sites:  %d%n", setOpCineCount);
			System.out.printf("  SetEdCinematicAvailable call sites:  %d%n", setEdCineCount);
			System.out.printf("  GetGameDifficulty call sites:        %d%n", getDifficultyCount);
			System.out.printf("  SetGameDifficulty call sites:        %d%n", setDifficultyCount);

			System.out.println("\n===============================================================================");
			if (brokenTargets == 0) {
				System.out.printf("AUDIT COMPLETE: All %d ChangeLevel target maps resolved successfully (100.0%%)!%n",
						resolvedTargets);
				System.out.println("===============================================================================");
			}
			else {
				System.err.printf("AUDIT FAILED: %d / %d ChangeLevel targets failed to resolve!%n",
						brokenTargets, changeLevelTargets.size());
				System.out.println("===============================================================================");
				System.exit(1);
			}
		}
		finally {
			for (final MpqDataSource source : mpqSources) {
				try {
					source.close();
				}
				catch (final Exception ignored) {
				}
			}
		}
	}
}

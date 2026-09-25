package com.etheller.warsmash.desktop.tools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.jass.JassAIEnvironment;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.parsers.w3x.doo.Doodad;
import com.etheller.warsmash.parsers.w3x.doo.War3MapDoo;
import com.etheller.warsmash.parsers.w3x.objectdata.Warcraft3MapRuntimeObjectData;
import com.etheller.warsmash.parsers.w3x.unitsdoo.Unit;
import com.etheller.warsmash.parsers.w3x.unitsdoo.War3MapUnitsDoo;
import com.etheller.warsmash.parsers.w3x.w3e.War3MapW3e;
import com.etheller.warsmash.parsers.w3x.w3i.Player;
import com.etheller.warsmash.parsers.w3x.w3i.War3MapW3i;
import com.etheller.warsmash.parsers.w3x.w3r.War3MapW3r;
import com.etheller.warsmash.parsers.w3x.wpm.War3MapWpm;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.util.WorldEditStrings;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.TextTag;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackInstant;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.attacks.CUnitAttackMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityCollisionProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAttackProjectileMissile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CCollisionProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CJassProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CPsuedoProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.CBasePlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRaceManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentLightning;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderController;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.TextTagConfigType;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.CommandErrorListener;

import mpq.MPQArchive;

/**
 * Headless Campaign Soak Test Runner.
 *
 * <p>Discovers all 85 single-player campaign maps across RoC and TFT retail MPQs,
 * boots headless simulation with map data (W3I, W3E, WPM, DOO, Units.doo, modifications),
 * parses and verifies AI routines, and steps the simulation engine for 300 ticks per map.
 * Detects data-loading and simulation-step failures. Does not execute mission
 * JASS, objectives, combat scenarios, or rendered presentation; this is not a
 * start-to-finish campaign playthrough or a parity certification.
 */
public final class CampaignSoakTestRunner {

	private CampaignSoakTestRunner() {
	}

	public static void main(final String[] args) throws Exception {
		final List<Path> mpqPaths = new ArrayList<>();
		int maxTicks = 300;
		int mapLimit = Integer.MAX_VALUE;
		String mapFilter = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--mpq":
				mpqPaths.add(Paths.get(args[++i]));
				break;
			case "--ticks":
				maxTicks = Integer.parseInt(args[++i]);
				break;
			case "--limit":
				mapLimit = Integer.parseInt(args[++i]);
				break;
			case "--filter":
				mapFilter = args[++i];
				break;
			default:
				throw new IllegalArgumentException("Unknown option: " + args[i]);
			}
		}

		if ((maxTicks <= 0) || (mapLimit <= 0)) {
			throw new IllegalArgumentException("--ticks and --limit must be positive");
		}

		if (mpqPaths.isEmpty()) {
			for (final String defaultPath : new String[] {
					"F:\\WC3Data\\war3.mpq",
					"F:\\WC3Data\\War3x.mpq",
					"F:\\WC3Data\\War3xlocal.mpq" }) {
				final Path p = Paths.get(defaultPath);
				if (Files.exists(p)) {
					mpqPaths.add(p);
				}
			}
		}

		if (mpqPaths.isEmpty()) {
			System.err.println("No retail archives found. Pass at least one --mpq <path> holding campaign maps.");
			System.exit(2);
		}

		System.out.println("===============================================================================");
		System.out.println("         WARCRAFT III 85-MAP HEADLESS CAMPAIGN SOAK TEST RUNNER                ");
		System.out.println("===============================================================================");
		System.out.println("Archives:");
		for (final Path p : mpqPaths) {
			System.out.println("  - " + p);
		}
		System.out.println("Target ticks per map: " + maxTicks);

		// Ensure Race Manager is initialized
		if (WarsmashConstants.RACE_MANAGER == null) {
			WarsmashConstants.RACE_MANAGER = new CRaceManager();
			WarsmashConstants.RACE_MANAGER.addRace("Human", 1, 1);
			WarsmashConstants.RACE_MANAGER.addRace("Orc", 2, 2);
			WarsmashConstants.RACE_MANAGER.addRace("Undead", 3, 4);
			WarsmashConstants.RACE_MANAGER.addRace("NightElf", 4, 3);
			WarsmashConstants.RACE_MANAGER.build();
		}

		final List<MpqDataSource> mpqSources = new ArrayList<>();
		try {
			for (final Path p : mpqPaths) {
				final SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ);
				final MPQArchive mpq = new MPQArchive(ch);
				mpqSources.add(new MpqDataSource(mpq, ch));
			}
			final CompoundDataSource compound = new CompoundDataSource(new ArrayList<>(mpqSources));

			// Load global misc data tables
			final DataTable miscData = new DataTable(new WorldEditStrings(compound));
			for (final String txt : new String[] { "UI\\MiscData.txt", "Units\\MiscData.txt", "Units\\MiscGame.txt", "UI\\MiscUI.txt" }) {
				if (compound.has(txt)) {
					try (InputStream is = compound.getResourceAsStream(txt)) {
						miscData.readTXT(is, true);
					}
				}
			}

			// Discover campaign maps
			final Set<String> campaignMaps = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
			for (final MpqDataSource source : mpqSources) {
				for (final String entry : source.getListfile()) {
					final String trimmed = entry.trim();
					final String lower = trimmed.toLowerCase();
					if ((lower.endsWith(".w3m") || lower.endsWith(".w3x")) && lower.contains("campaign")) {
						if ((mapFilter == null) || lower.contains(mapFilter.toLowerCase())) {
							campaignMaps.add(trimmed);
						}
					}
				}
			}

			System.out.println("Total Campaign Maps Discovered: " + campaignMaps.size());
			if (campaignMaps.isEmpty()) {
				throw new IllegalStateException("No campaign maps matched; no audit was performed");
			}
			System.out.println("-------------------------------------------------------------------------------");

			int index = 0;
			int passed = 0;
			int failed = 0;
			long totalSimTicks = 0;
			final long soakStartMillis = System.currentTimeMillis();

			for (final String mapPath : campaignMaps) {
				index++;
				if (index > mapLimit) {
					break;
				}

				final long mapStart = System.currentTimeMillis();
				try (War3Map map = War3MapViewer.beginLoadingMap(compound, mapPath)) {
					final War3MapW3i w3i = map.readMapInformation();
					final War3MapW3e env = map.readEnvironment();
					final War3MapWpm pathing = map.readPathing();
					final War3MapDoo doodads = map.readDoodads(w3i);
					final War3MapUnitsDoo unitsDoo = map.readUnits(w3i);
					final War3MapW3r regions = map.readRegions();
					final Warcraft3MapRuntimeObjectData objectData = map.readModifications();

					// Check AI script validity
					int aiScriptsVerified = 0;
					if (map.has("war3map.j")) {
						final CompoundDataSource mapCompound = new CompoundDataSource(Arrays.asList(compound, map));
						try (InputStream in = map.getResourceAsStream("war3map.j");
								BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
							String line;
							while ((line = reader.readLine()) != null) {
								if (line.contains("StartCampaignAI") || line.contains("StartMeleeAI")) {
									final int firstQuote = line.indexOf('"');
									final int secondQuote = line.indexOf('"', firstQuote + 1);
									if (firstQuote >= 0 && secondQuote > firstQuote) {
										final String script = line.substring(firstQuote + 1, secondQuote).replace("\\\\", "\\");
										try {
											final JassAIEnvironment ai = JassAIEnvironment.loadAI(
													mapCompound, null, null, null, null, null, 1, script);
											if (ai != null) {
												aiScriptsVerified++;
											}
											else {
												throw new IllegalStateException("AI did not load: " + script);
											}
										}
										catch (final Exception e) {
											throw new IllegalStateException("AI load failed: " + script, e);
										}
									}
								}
							}
						}
					}

					// Setup headless simulation
					final War3MapConfig mapConfig = new War3MapConfig(WarsmashConstants.MAX_PLAYERS);
					mapConfig.setMapName(w3i.getName());
					for (int pIdx = 0; pIdx < w3i.getPlayers().size(); pIdx++) {
						final Player p = w3i.getPlayers().get(pIdx);
						final CBasePlayer cp = mapConfig.getPlayer(p.getId());
						if (cp != null && p.getName() != null) {
							cp.setName(p.getName());
						}
					}

					final float[] centerOffset = env.getCenterOffset();
					final int[] mapSize = env.getMapSize();
					final Rectangle entireMap = new Rectangle(centerOffset[0], centerOffset[1],
							(mapSize[0] * 128f) - 128, (mapSize[1] * 128f) - 128);
					final PathingGrid pathingGrid = new PathingGrid(pathing, centerOffset);

					final CommandErrorListener noopErrorListener = new CommandErrorListener() {
						@Override
						public void showInterfaceError(final int playerIndex, final String message) {
						}

						@Override
						public void showCommandErrorWithoutSound(final int playerIndex, final String message) {
						}

						@Override
						public void showUpgradeCompleteAlert(final int playerIndex, final War3ID queuedRawcode, final int level) {
						}
					};

					final HeadlessSimulationRenderController renderController = new HeadlessSimulationRenderController();
					final CSimulation simulation = new CSimulation(mapConfig, w3i.getVersion(), miscData,
							objectData.getUnits(), objectData.getItems(), objectData.getDestructibles(),
							objectData.getAbilities(), objectData.getUpgrades(), objectData.getStandardUpgradeEffectMeta(),
							renderController, pathingGrid, entireMap, new Random(1337), noopErrorListener);
					renderController.setSimulation(simulation);

					// Pre-place units
					int unitsSpawned = 0;
					if (unitsDoo != null) {
						for (final Unit u : unitsDoo.getUnits()) {
							if (War3ID.fromString("sloc").equals(u.getId())) {
								continue;
							}
							if (objectData.getItems().get(u.getId().asStringValue()) != null) {
								simulation.createItem(u.getId(), u.getLocation()[0], u.getLocation()[1]);
								continue;
							}
							try {
								int pIdx = u.getPlayer();
								if ((pIdx < 0) || (pIdx >= WarsmashConstants.MAX_PLAYERS)) {
									pIdx = pIdx & 0xFF;
									if ((pIdx < 0) || (pIdx >= WarsmashConstants.MAX_PLAYERS)) {
										pIdx = WarsmashConstants.MAX_PLAYERS - 1;
									}
								}
								final CUnit created = simulation.createUnit(u.getId(), pIdx,
										u.getLocation()[0], u.getLocation()[1], (float) Math.toDegrees(u.getAngle()));
								if (created != null) {
									unitsSpawned++;
								}
							}
							catch (final Exception e) {
								throw new IllegalStateException("Unit load failed: " + u.getId(), e);
							}
						}
					}

					// Pre-place doodads
					int doodadsSpawned = 0;
					if (doodads != null) {
						for (final Doodad d : doodads.getDoodads()) {
							if (objectData.getDestructibles().get(d.getId().asStringValue()) == null) {
								continue; // Decorative doodads have no simulation entity.
							}
							try {
								simulation.internalCreateDestructable(d.getId(), d.getLocation()[0], d.getLocation()[1], null, null);
								doodadsSpawned++;
							}
							catch (final Exception e) {
								throw new IllegalStateException("Destructable load failed: " + d.getId(), e);
							}
						}
					}

					// Run soak simulation ticks
					for (int t = 0; t < maxTicks; t++) {
						simulation.update();
					}
					totalSimTicks += maxTicks;

					final long elapsed = System.currentTimeMillis() - mapStart;
					System.out.printf("[%02d/%02d] PASS: %-36s | Units: %3d | Doodads: %4d | AI: %d | %d ticks in %d ms%n",
							index, campaignMaps.size(), mapPath, unitsSpawned, doodadsSpawned, aiScriptsVerified, maxTicks, elapsed);
					passed++;
				}
				catch (final Exception t) {
					final long elapsed = System.currentTimeMillis() - mapStart;
					System.err.printf("[%02d/%02d] FAIL: %-36s in %d ms -> %s%n",
							index, campaignMaps.size(), mapPath, elapsed, t.getMessage());
					t.printStackTrace();
					failed++;
				}
				finally {
					System.gc();
				}
			}

			final long totalDuration = System.currentTimeMillis() - soakStartMillis;
			System.out.println("===============================================================================");
			System.out.println("                         CAMPAIGN SOAK AUDIT SUMMARY                           ");
			System.out.println("===============================================================================");
			System.out.printf("Campaign Maps Discovered: %d%n", campaignMaps.size());
			System.out.printf("Maps Tested:             %d%n", passed + failed);
			System.out.printf("Maps Passed:             %d (%.1f%%)%n", passed, (passed * 100.0) / (passed + failed));
			System.out.println("Scope: data loading and idle simulation only; mission objectives and rendering are not tested.");
			System.out.printf("Maps Failed:             %d%n", failed);
			System.out.printf("Total Simulation Ticks:  %,d ticks%n", totalSimTicks);
			System.out.printf("Total Execution Time:    %.2f seconds%n", totalDuration / 1000.0);
			System.out.println("===============================================================================");

			if (failed > 0) {
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

	private static final class HeadlessSimulationRenderController implements SimulationRenderController {
		private CSimulation simulation;

		public void setSimulation(final CSimulation simulation) {
			this.simulation = simulation;
		}

		@Override
		public CAttackProjectile createAttackProjectile(final CSimulation simulation, final float launchX,
				final float launchY, final float launchFacing, final CUnit source, final CUnitAttackMissile attack,
				final AbilityTarget target, final float damage, final int bounceIndex,
				final CUnitAttackListener attackListener) {
			return new CAttackProjectileMissile(launchX, launchY, attack.getProjectileSpeed(), target, source, damage,
					attack, bounceIndex, attackListener);
		}

		@Override
		public CAbilityProjectile createProjectile(final CSimulation cSimulation, final float launchX,
				final float launchY, final float launchFacing, final float speed, final boolean homing, final CUnit source,
				final War3ID spellAlias, final AbilityTarget target, final CAbilityProjectileListener projectileListener) {
			return new CAbilityProjectile(launchX, launchY, speed, target, homing, source, projectileListener);
		}

		@Override
		public CJassProjectile createJassProjectile(final CSimulation cSimulation, final float launchX,
				final float launchY, final float launchFacing, final float speed, final boolean homing, final CUnit source,
				final War3ID spellAlias, final AbilityTarget target) {
			return new CJassProjectile(launchX, launchY, speed, target, homing, source);
		}

		@Override
		public CCollisionProjectile createCollisionProjectile(final CSimulation cSimulation, final float launchX,
				final float launchY, final float launchFacing, final float projectileSpeed, final boolean homing,
				final CUnit source, final War3ID spellAlias, final AbilityTarget target, final int maxHits,
				final int hitsPerTarget, final float startingRadius, final float finalRadius,
				final float collisionInterval, final CAbilityCollisionProjectileListener projectileListener,
				final boolean provideCounts) {
			return new CCollisionProjectile(launchX, launchY, projectileSpeed, target, homing, source,
					maxHits, hitsPerTarget, startingRadius, finalRadius, collisionInterval, projectileListener,
					provideCounts);
		}

		@Override
		public CPsuedoProjectile createPseudoProjectile(final CSimulation cSimulation, final float launchX,
				final float launchY, final float launchFacing, final float projectileSpeed,
				final float projectileStepInterval, final int projectileArtSkip, final boolean homing, final CUnit source,
				final War3ID spellAlias, final CEffectType effectType, final int effectArtIndex,
				final AbilityTarget target, final int maxHits, final int hitsPerTarget, final float startingRadius,
				final float finalRadius, final CAbilityCollisionProjectileListener projectileListener,
				final boolean provideCounts) {
			return new CPsuedoProjectile(launchX, launchY, projectileSpeed, projectileStepInterval, projectileArtSkip,
					target, homing, source, spellAlias, effectType, effectArtIndex, maxHits, hitsPerTarget,
					startingRadius, finalRadius, projectileListener, provideCounts);
		}

		@Override
		public SimulationRenderComponentLightning createLightning(final CSimulation simulation,
				final War3ID lightningId, final CUnit source, final CUnit target) {
			return SimulationRenderComponentLightning.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponentLightning createLightning(final CSimulation simulation,
				final War3ID lightningId, final CUnit source, final CUnit target, final Float duration) {
			return SimulationRenderComponentLightning.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponentLightning createAbilityLightning(final CSimulation simulation,
				final War3ID lightningId, final CUnit source, final CUnit target, final int index) {
			return SimulationRenderComponentLightning.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponentLightning createAbilityLightning(final CSimulation simulation,
				final War3ID lightningId, final CUnit source, final CUnit target, final int index, final Float duration) {
			return SimulationRenderComponentLightning.DO_NOTHING;
		}

		@Override
		public CUnit createUnit(final CSimulation simulation, final War3ID typeId, final int playerIndex,
				final float x, final float y, final float facing) {
			return this.simulation.internalCreateUnit(typeId, playerIndex, x, y, (float) Math.toDegrees(facing),
					PathingGrid.BLANK_PATHING);
		}

		@Override
		public CItem createItem(final CSimulation simulation, final War3ID typeId, final float x, final float y) {
			return this.simulation.internalCreateItem(typeId, x, y);
		}

		@Override
		public CDestructable createDestructable(final War3ID typeId, final float x, final float y, final float facing,
				final float scale, final int variation) {
			return this.simulation.internalCreateDestructable(typeId, x, y, null, null);
		}

		@Override
		public CDestructable createDestructableZ(final War3ID typeId, final float x, final float y, final float z,
				final float facing, final float scale, final int variation) {
			return this.simulation.internalCreateDestructable(typeId, x, y, null, null);
		}

		@Override
		public void createInstantAttackEffect(final CSimulation cSimulation, final CUnit source,
				final CUnitAttackInstant attack, final CWidget target) {
		}

		@Override
		public void spawnDamageSound(final CWidget damagedDestructable, final String weaponSound,
				final String armorType) {
		}

		@Override
		public void spawnUnitConstructionSound(final CUnit constructingUnit, final CUnit constructedStructure) {
		}

		@Override
		public void removeUnit(final CUnit unit) {
		}

		@Override
		public void removeDestructable(final CDestructable dest) {
		}

		@Override
		public BufferedImage getBuildingPathingPixelMap(final War3ID rawcode) {
			return PathingGrid.BLANK_PATHING;
		}

		@Override
		public BufferedImage getDestructablePathingPixelMap(final War3ID rawcode) {
			return PathingGrid.BLANK_PATHING;
		}

		@Override
		public BufferedImage getDestructablePathingDeathPixelMap(final War3ID rawcode) {
			return PathingGrid.BLANK_PATHING;
		}

		@Override
		public void spawnUnitConstructionFinishSound(final CUnit constructedStructure) {
		}

		@Override
		public void spawnUnitUpgradeFinishSound(final CUnit constructedStructure) {
		}

		@Override
		public void spawnDeathExplodeEffect(final CUnit cUnit, final War3ID explodesOnDeathBuffId) {
		}

		@Override
		public void spawnGainLevelEffect(final CUnit cUnit) {
		}

		@Override
		public void spawnUnitReadySound(final CUnit trainedUnit) {
		}

		@Override
		public void unitRepositioned(final CUnit cUnit) {
		}

		@Override
		public TextTag spawnTextTag(final CUnit unit, final TextTagConfigType configType, final int displayAmount) {
			return spawnTextTag(unit, configType, String.valueOf(displayAmount));
		}

		@Override
		public TextTag spawnTextTag(final CUnit unit, final TextTagConfigType configType, final String message) {
			final Vector3 pos = unit != null ? new Vector3(unit.getX(), unit.getY(), 0) : new Vector3(0, 0, 0);
			return new TextTag(pos, new Vector2(0, 60f), message != null ? message : "", Color.WHITE, 2f, 1f, 10f, -1);
		}

		@Override
		public TextTag createTextTag() {
			return new TextTag(new Vector3(0, 0, 0), new Vector2(0, 0), "", Color.WHITE, 100f, 0f, 10f, 0);
		}

		@Override
		public void destroyTextTag(final TextTag textTag) {
		}

		@Override
		public void spawnEffectOnUnit(final CUnit unit, final String effectPath) {
		}

		@Override
		public void spawnTemporarySpellEffectOnUnit(final CUnit unit, final War3ID alias,
				final CEffectType effectType) {
		}

		@Override
		public SimulationRenderComponentModel spawnPersistentSpellEffectOnUnit(final CUnit unit, final War3ID alias,
				final CEffectType effectType) {
			return SimulationRenderComponentModel.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponentModel spawnPersistentSpellEffectOnUnit(final CUnit unit, final War3ID alias,
				final CEffectType effectType, final int index) {
			return SimulationRenderComponentModel.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponentModel spawnSpellEffectOnPoint(final float x, final float y, final float facing,
				final War3ID alias, final CEffectType effectType, final int index) {
			return SimulationRenderComponentModel.DO_NOTHING;
		}

		@Override
		public void spawnTemporarySpellEffectOnPoint(final float x, final float y, final float facing,
				final War3ID alias, final CEffectType effectType, final int index) {
		}

		@Override
		public void spawnUIUnitGetItemSound(final CUnit cUnit, final CItem item) {
		}

		@Override
		public void spawnUIUnitDropItemSound(final CUnit cUnit, final CItem item) {
		}

		@Override
		public SimulationRenderComponent spawnAbilitySoundEffect(final CUnit caster, final War3ID alias) {
			return SimulationRenderComponent.DO_NOTHING;
		}

		@Override
		public SimulationRenderComponent loopAbilitySoundEffect(final CUnit caster, final War3ID alias) {
			return SimulationRenderComponent.DO_NOTHING;
		}

		@Override
		public void stopAbilitySoundEffect(final CUnit caster, final War3ID alias) {
		}

		@Override
		public void unitPreferredSelectionReplacement(final CUnit unit, final CUnit newUnit) {
		}

		@Override
		public void heroRevived(final CUnit trainedUnit) {
		}

		@Override
		public void heroDeathEvent(final CUnit cUnit) {
		}

		@Override
		public SimulationRenderComponentModel createSpellEffectOverDestructable(final CUnit source,
				final CDestructable target, final War3ID alias, final float artAttachmentHeight) {
			return SimulationRenderComponentModel.DO_NOTHING;
		}

		@Override
		public void unitUpgradingEvent(final CUnit unit, final War3ID upgradeIdType) {
		}

		@Override
		public void unitCancelUpgradingEvent(final CUnit unit, final War3ID upgradeIdType) {
		}

		@Override
		public void setBlight(final float x, final float y, final float radius, final boolean blighted) {
		}

		@Override
		public void unitUpdatedType(final CUnit unit, final War3ID typeId) {
		}

		@Override
		public void changeUnitColor(final CUnit unit, final int playerIndex) {
		}

		@Override
		public void changeUnitPlayerColor(final CUnit unit, final int previousColor, final int newColor) {
		}

		@Override
		public void changeUnitVertexColor(final CUnit unit, final Color color) {
		}

		@Override
		public void changeUnitVertexColor(final CUnit unit, final float r, final float g, final float b) {
		}

		@Override
		public void changeUnitVertexColor(final CUnit unit, final float r, final float g, final float b,
				final float a) {
		}

		@Override
		public float[] getUnitVertexColor(final CUnit unit) {
			return new float[] { 1f, 1f, 1f, 1f };
		}

		@Override
		public int getTerrainHeight(final float x, final float y) {
			return 0;
		}

		@Override
		public boolean isTerrainRomp(final float x, final float y) {
			return false;
		}

		@Override
		public boolean isTerrainWater(final float x, final float y) {
			return false;
		}
	}
}

package com.etheller.warsmash.desktop.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.StringBundle;
import com.google.common.io.ByteStreams;
import com.hiveworkshop.rms.parsers.mdlx.MdlxModel;
import com.hiveworkshop.rms.parsers.mdlx.MdlxSequence;

import mpq.MPQArchive;

/**
 * CLI tool auditing 3D and 2D backing screens across Warcraft III: Reign of Chaos
 * and The Frozen Throne data archives.
 *
 * <p>Audits:
 * <ul>
 *   <li>Main menu 3D scenes ({@code MainMenu3D}, {@code MainMenu3D_exp})</li>
 *   <li>Campaign backdrops (RoC &amp; TFT) and skin resolution ({@code _V0} / {@code _V1})</li>
 *   <li>Camera definitions, FOV, and sequence sets</li>
 *   <li>Score screen models and loading screen backdrops</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * ./gradlew :desktop:campaignBackingAudit
 * ./gradlew :desktop:campaignBackingAudit -Pargs="--mpq F:\WC3Data\war3.mpq --mpq F:\WC3Data\War3x.mpq --mpq F:\WC3Data\War3xlocal.mpq"
 * </pre>
 */
public final class CampaignBackingScreensAudit {

	private static final String[] DEFAULT_ARCHIVES = {
			"F:\\WC3Data\\war3.mpq",
			"F:\\WC3Data\\War3x.mpq",
			"F:\\WC3Data\\War3xlocal.mpq",
	};

	private static final String[] SCORE_SCREEN_MODELS = {
			"UI\\Glues\\ScoreScreen\\ScoreScreen-Background\\ScoreScreen-Background.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-HumanVictory\\ScoreScreen-HumanVictory.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-HumanVictoryExpansion\\ScoreScreen-HumanVictoryExpansion.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-OrcVictory\\ScoreScreen-OrcVictory.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-OrcVictoryExpansion\\ScoreScreen-OrcVictoryExpansion.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-NightElfVictory\\ScoreScreen-NightElfVictory.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-NightElfVictoryExpansion\\ScoreScreen-NightElfVictoryExpansion.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-UndeadVictory\\ScoreScreen-UndeadVictory.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-UndeadVictoryExpansion\\ScoreScreen-UndeadVictoryExpansion.mdx",
			"UI\\Glues\\ScoreScreen\\ScoreScreen-Defeat\\ScoreScreen-Defeat.mdx"
	};

	private static final String[] LOADING_SCREEN_MODELS = {
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\AshenvaleBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\AshenvaleExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\BarrensBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\BarrensExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\DalaranBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\LordaeronBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\LordaeronExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\NorthrendBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\IcecrownExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\QuelthalasBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\DrownedRuinsExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\OutlandExpansionBackground.mdx",
			"UI\\Glues\\Loading\\Backgrounds\\Campaigns\\TutorialBackground.mdx",
			"UI\\Glues\\Loading\\Load-Generic\\Load-Generic.mdx",
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-Human.mdx",
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-Orc.mdx",
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-NightElf.mdx",
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-Undead.mdx",
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-Random.mdx"
	};

	private CampaignBackingScreensAudit() {
	}

	public static void main(final String[] args) throws Exception {
		final List<Path> archivePaths = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			if ("--mpq".equals(args[i]) && ((i + 1) < args.length)) {
				archivePaths.add(Paths.get(args[++i]));
			}
		}
		if (archivePaths.isEmpty()) {
			for (final String def : DEFAULT_ARCHIVES) {
				final Path p = Paths.get(def);
				if (Files.exists(p)) {
					archivePaths.add(p);
				}
			}
		}

		if (archivePaths.isEmpty()) {
			System.err.println("No MPQ archives found. Pass --mpq <path> to specify archive locations.");
			return;
		}

		System.out.println("===============================================================================");
		System.out.println("              WARCRAFT III BACKING SCREENS PARITY AUDIT                        ");
		System.out.println("===============================================================================");
		System.out.println("Archives:");
		for (final Path p : archivePaths) {
			System.out.println("  - " + p);
		}
		System.out.println();

		final List<CloseableSource> closeables = new ArrayList<>();
		final List<DataSource> sources = new ArrayList<>();
		for (final Path p : archivePaths) {
			final SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ);
			final MPQArchive mpq = new MPQArchive(ch);
			sources.add(new MpqDataSource(mpq, ch));
			closeables.add(new CloseableSource(ch));
		}
		final CompoundDataSource compound = new CompoundDataSource(sources);

		int auditedCount = 0;
		int passedCount = 0;

		try {
			// 1. Audit UI skins (war3skins.txt)
			System.out.println("--- 1. UI Skin Resolution Keys (war3skins.txt) ---");
			final byte[] skinBytes = readAny(compound, "UI\\war3skins.txt");
			final DataTable skinsTable = new DataTable(StringBundle.EMPTY);
			if (skinBytes != null) {
				try (InputStream in = new ByteArrayInputStream(skinBytes)) {
					skinsTable.readTXT(in, true);
				}
			}
			final Element defaultSkin = skinsTable.get("Default");
			if (defaultSkin == null) {
				System.err.println("FAIL: 'Default' section missing from war3skins.txt!");
			}
			else {
				final String[] skinKeys = {
						"GlueSpriteLayerBackground", "HumanBackdrop", "TutorialBackdrop",
						"OrcBackdrop", "UndeadBackdrop", "NightElfBackdrop", "CampaignFile"
				};
				for (final String key : skinKeys) {
					auditedCount++;
					final String defVal = defaultSkin.getField(key);
					final String v0Val = defaultSkin.getField(key + "_V0");
					final String v1Val = defaultSkin.getField(key + "_V1");
					System.out.printf("  %-28s default=%-42s _V0=%-42s _V1=%s%n",
							key + ":", defVal, v0Val, v1Val);
					if (!defVal.isEmpty() || !v0Val.isEmpty() || !v1Val.isEmpty()) {
						passedCount++;
					}
				}
			}
			System.out.println();

			// 2. Audit Main Menu 3D backdrops
			System.out.println("--- 2. Main Menu 3D Backing Models ---");
			final String[] mainModels = {
					"UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdx",
					"UI\\Glues\\MainMenu\\MainMenu3D_exp\\MainMenu3D_exp.mdx"
			};
			for (final String path : mainModels) {
				auditedCount++;
				final boolean ok = audit3DModel(compound, path, "Main Menu");
				if (ok) {
					passedCount++;
				}
			}
			System.out.println();

			// 3. Audit Campaign backing screens
			System.out.println("--- 3. Single Player Campaign Backdrops ---");
			final String[] campaignFiles = { "UI\\CampaignStrings.txt", "UI\\CampaignStrings_exp.txt" };
			for (final String cFile : campaignFiles) {
				System.out.println("  [" + cFile + "]");
				final byte[] cData = readAny(compound, cFile);
				if (cData == null) {
					System.out.println("    MISSING: " + cFile);
					continue;
				}
				final DataTable cTable = new DataTable(StringBundle.EMPTY);
				try (InputStream in = new ByteArrayInputStream(cData)) {
					cTable.readTXT(in, true);
				}
				final boolean isTft = cFile.contains("_exp");
				for (final String sec : cTable.keySet()) {
					if ("Index".equalsIgnoreCase(sec)) {
						continue;
					}
					final Element el = cTable.get(sec);
					final String bg = el.getField("Background");
					if (bg.isEmpty()) {
						continue;
					}
					auditedCount++;
					// Resolve through skin
					String resolved = bg;
					if (defaultSkin != null) {
						final String versionedKey = bg + (isTft ? "_V1" : "_V0");
						if (defaultSkin.hasField(versionedKey)) {
							resolved = defaultSkin.getField(versionedKey);
						}
						else if (defaultSkin.hasField(bg)) {
							resolved = defaultSkin.getField(bg);
						}
					}
					final String mdxPath = resolved.endsWith(".mdl")
							? resolved.substring(0, resolved.length() - 4) + ".mdx"
							: resolved;
					final boolean ok = audit3DModel(compound, mdxPath, sec + " (" + bg + ")");
					if (ok) {
						passedCount++;
					}
				}
			}
			System.out.println();

			// 4. Audit Score screen models
			System.out.println("--- 4. Score Screen Models ---");
			for (final String path : SCORE_SCREEN_MODELS) {
				auditedCount++;
				final byte[] data = readAny(compound, path);
				if (data == null) {
					System.out.println("  FAIL: Missing score model: " + path);
					continue;
				}
				final MdlxModel mdlx = new MdlxModel(ByteBuffer.wrap(data));
				System.out.printf("  PASS: %-70s (seqs=%d, geoids=%d)%n",
						path, mdlx.sequences.size(), mdlx.geosets.size());
				passedCount++;
			}
			System.out.println();

			// 5. Audit Loading screen models
			System.out.println("--- 5. Loading Screen Models ---");
			for (final String path : LOADING_SCREEN_MODELS) {
				auditedCount++;
				final byte[] data = readAny(compound, path);
				if (data == null) {
					System.out.println("  FAIL: Missing loading screen model: " + path);
					continue;
				}
				final MdlxModel mdlx = new MdlxModel(ByteBuffer.wrap(data));
				System.out.printf("  PASS: %-60s (seqs=%d, geoids=%d)%n",
						path, mdlx.sequences.size(), mdlx.geosets.size());
				passedCount++;
			}
			System.out.println();

			// Summary
			System.out.println("===============================================================================");
			System.out.printf("AUDIT COMPLETE: %d / %d checks passed (%.1f%%)%n",
					passedCount, auditedCount, (passedCount * 100.0) / Math.max(1, auditedCount));
			System.out.println("===============================================================================");
		}
		finally {
			for (final CloseableSource cs : closeables) {
				cs.close();
			}
		}
	}

	private static boolean audit3DModel(final CompoundDataSource compound, final String path, final String desc) {
		final byte[] data = readAny(compound, path);
		if (data == null) {
			System.out.printf("  FAIL [%s]: Missing model: %s%n", desc, path);
			return false;
		}
		try {
			final MdlxModel mdlx = new MdlxModel(ByteBuffer.wrap(data));
			final int cams = mdlx.cameras.size();
			final int seqs = mdlx.sequences.size();
			final float fov = cams > 0 ? mdlx.cameras.get(0).getFieldOfView() : Float.NaN;
			final List<String> seqNames = new ArrayList<>();
			for (final MdlxSequence s : mdlx.sequences) {
				seqNames.add(s.getName());
			}
			System.out.printf("  PASS [%s]: %s%n", desc, path);
			System.out.printf("        cams=%d, fov=%.4frad, seqs=%s%n", cams, fov, seqNames);
			return cams >= 1 && seqs >= 1;
		}
		catch (final Exception e) {
			System.out.printf("  FAIL [%s]: Failed to parse model %s: %s%n", desc, path, e.getMessage());
			return false;
		}
	}

	private static byte[] readAny(final CompoundDataSource compound, final String path) {
		if (compound.has(path)) {
			try (InputStream in = compound.getResourceAsStream(path)) {
				return ByteStreams.toByteArray(in);
			}
			catch (final Exception ignored) {
			}
		}
		return null;
	}

	private static final class CloseableSource implements AutoCloseable {
		private final SeekableByteChannel channel;

		CloseableSource(final SeekableByteChannel channel) {
			this.channel = channel;
		}

		@Override
		public void close() {
			try {
				this.channel.close();
			}
			catch (final Exception ignored) {
			}
		}
	}
}

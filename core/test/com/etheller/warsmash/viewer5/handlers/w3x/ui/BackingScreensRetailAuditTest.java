package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.google.common.io.ByteStreams;
import com.hiveworkshop.rms.parsers.mdlx.MdlxCamera;
import com.hiveworkshop.rms.parsers.mdlx.MdlxModel;

import mpq.MPQArchive;

/**
 * Validates retail 3D and 2D backing screens, score screen models, and loading
 * screen models across Reign of Chaos and The Frozen Throne data archives.
 *
 * <p>Skipped gracefully if retail archives are not available locally.</p>
 */
class BackingScreensRetailAuditTest {

	private static final String[] ARCHIVE_PATHS = {
			"F:\\WC3Data\\war3.mpq",
			"F:\\WC3Data\\War3x.mpq",
			"F:\\WC3Data\\War3xlocal.mpq",
	};

	private static final String[] MAIN_MENU_MODELS = {
			"UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdx",
			"UI\\Glues\\MainMenu\\MainMenu3D_exp\\MainMenu3D_exp.mdx",
	};

	private static final String[] CAMPAIGN_MODELS = {
			"UI\\Glues\\SinglePlayer\\TutorialCampaign3D\\TutorialCampaign3D.mdx",
			"UI\\Glues\\SinglePlayer\\HumanCampaign3D\\HumanCampaign3D.mdx",
			"UI\\Glues\\SinglePlayer\\OrcCampaign3D\\OrcCampaign3D.mdx",
			"UI\\Glues\\SinglePlayer\\UndeadCampaign3D\\UndeadCampaign3D.mdx",
			"UI\\Glues\\SinglePlayer\\NightElfCampaign3D\\NightElfCampaign3D.mdx",
			"UI\\Glues\\SinglePlayer\\Alliance_Exp\\Alliance_Exp.mdx",
			"UI\\Glues\\SinglePlayer\\Orc_Exp\\Orc_Exp.mdx",
			"UI\\Glues\\SinglePlayer\\Undead3D_Exp\\Undead3D_Exp.mdx",
			"UI\\Glues\\SinglePlayer\\NightElf_Exp\\NightElf_Exp.mdx",
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
			"UI\\Glues\\ScoreScreen\\ScoreScreen-Defeat\\ScoreScreen-Defeat.mdx",
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
			"UI\\Glues\\Loading\\Multiplayer\\Load-Multiplayer-Random.mdx",
	};

	@Test
	void backingScreensExistAndParse() throws Exception {
		final List<CloseableSource> closeables = new ArrayList<>();
		final CompoundDataSource compound = openCompound(closeables);
		if (compound == null) {
			return; // Graceful skip on environments without local retail data
		}

		try {
			// 1. Audit Main Menu 3D backdrops
			for (final String modelPath : MAIN_MENU_MODELS) {
				final byte[] data = readModel(compound, modelPath);
				assertNotNull(data, "Main menu backing model missing: " + modelPath);
				final MdlxModel model = new MdlxModel(ByteBuffer.wrap(data));
				assertTrue(model.cameras.size() >= 1, "Main menu model must define at least 1 camera: " + modelPath);
				final MdlxCamera cam = model.cameras.get(0);
				assertTrue(cam.getFieldOfView() > 0, "Camera FOV must be positive: " + modelPath);
				assertTrue(model.sequences.size() >= 3, "Main menu model must have Stand and BattleNet variants: " + modelPath);
			}

			// 2. Audit Single Player Campaign 3D backdrops
			for (final String modelPath : CAMPAIGN_MODELS) {
				final byte[] data = readModel(compound, modelPath);
				assertNotNull(data, "Campaign backing model missing: " + modelPath);
				final MdlxModel model = new MdlxModel(ByteBuffer.wrap(data));
				assertTrue(model.cameras.size() >= 1, "Campaign model must define at least 1 camera: " + modelPath);
				final MdlxCamera cam = model.cameras.get(0);
				assertTrue(cam.getFieldOfView() > 0, "Camera FOV must be positive: " + modelPath);
				assertTrue(model.sequences.size() >= 1, "Campaign model must have sequences: " + modelPath);
			}

			// 3. Audit Score Screen models
			for (final String modelPath : SCORE_SCREEN_MODELS) {
				final byte[] data = readModel(compound, modelPath);
				assertNotNull(data, "Score screen model missing: " + modelPath);
				final MdlxModel model = new MdlxModel(ByteBuffer.wrap(data));
				assertNotNull(model.sequences, "Score screen model must parse sequences: " + modelPath);
			}

			// 4. Audit Loading Screen models
			for (final String modelPath : LOADING_SCREEN_MODELS) {
				final byte[] data = readModel(compound, modelPath);
				assertNotNull(data, "Loading screen model missing: " + modelPath);
				final MdlxModel model = new MdlxModel(ByteBuffer.wrap(data));
				assertNotNull(model.sequences, "Loading screen model must parse sequences: " + modelPath);
			}
		}
		finally {
			for (final CloseableSource cs : closeables) {
				cs.close();
			}
		}
	}

	private static byte[] readModel(final CompoundDataSource compound, final String modelPath) {
		if (compound.has(modelPath)) {
			try (InputStream in = compound.getResourceAsStream(modelPath)) {
				return ByteStreams.toByteArray(in);
			}
			catch (final Exception ignored) {
			}
		}
		return null;
	}

	private static CompoundDataSource openCompound(final List<CloseableSource> closeables) {
		final List<DataSource> sources = new ArrayList<>();
		for (final String pathStr : ARCHIVE_PATHS) {
			try {
				final Path p = Paths.get(pathStr);
				if (Files.exists(p)) {
					final SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ);
					final MPQArchive mpq = new MPQArchive(ch);
					sources.add(new MpqDataSource(mpq, ch));
					closeables.add(new CloseableSource(ch));
				}
			}
			catch (final Exception ignored) {
			}
		}
		if (sources.isEmpty()) {
			return null;
		}
		return new CompoundDataSource(sources);
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

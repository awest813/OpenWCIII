package com.etheller.warsmash.parsers.fdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.util.StringBundle;
import com.etheller.warsmash.util.WarsmashConstants;

/**
 * Tests version-aware skin resolution in {@link GameUI}.
 *
 * <p>Warcraft III {@code UI\war3skins.txt} specifies base defaults alongside
 * {@code _V0} (Reign of Chaos) and {@code _V1} (The Frozen Throne) overrides.
 * {@link GameUI#getSkinField(String)}, {@link GameUI#hasSkinField(String)}, and
 * {@link GameUI#trySkinField(String)} must prioritize versioned keys over unversioned
 * defaults so that TFT backdrops and assets load correctly.</p>
 */
class GameUISkinResolutionTest {

	private int originalGameVersion;
	private GameUI gameUI;

	@BeforeEach
	void setUp() {
		this.originalGameVersion = WarsmashConstants.GAME_VERSION;

		final DataTable skinsTable = new DataTable(StringBundle.EMPTY);
		final Element userSkin = new Element("TestSkin", skinsTable);

		// Main menu backdrops
		userSkin.setField("GlueSpriteLayerBackground", "UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdl");
		userSkin.setField("GlueSpriteLayerBackground_V0", "UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdl");
		userSkin.setField("GlueSpriteLayerBackground_V1", "UI\\Glues\\MainMenu\\MainMenu3D_exp\\MainMenu3D_exp.mdl");

		// Campaign backdrops
		userSkin.setField("HumanBackdrop", "UI\\Glues\\SinglePlayer\\HumanCampaign3D\\HumanCampaign3D.mdl");
		userSkin.setField("HumanBackdrop_V0", "UI\\Glues\\SinglePlayer\\HumanCampaign3D\\HumanCampaign3D.mdl");
		userSkin.setField("HumanBackdrop_V1", "UI\\Glues\\SinglePlayer\\Alliance_Exp\\Alliance_Exp.mdl");

		// Unversioned key fallback
		userSkin.setField("EscMenuBackground", "UI\\Widgets\\EscMenu\\Human\\edit-border.blp");

		// Versioned-only key (no unversioned base)
		userSkin.setField("CampaignFile_V0", "UI\\CampaignStrings.txt");
		userSkin.setField("CampaignFile_V1", "UI\\CampaignStrings_exp.txt");

		final GameSkin skin = new GameSkin(userSkin, skinsTable);
		this.gameUI = new GameUI(skin);
	}

	@AfterEach
	void tearDown() {
		WarsmashConstants.GAME_VERSION = this.originalGameVersion;
	}

	@Test
	void frozenThronePrioritizesVersion1Overrides() {
		WarsmashConstants.GAME_VERSION = 1;

		assertTrue(this.gameUI.hasSkinField("GlueSpriteLayerBackground"));
		assertEquals("UI\\Glues\\MainMenu\\MainMenu3D_exp\\MainMenu3D_exp.mdl",
				this.gameUI.getSkinField("GlueSpriteLayerBackground"));
		assertEquals("UI\\Glues\\MainMenu\\MainMenu3D_exp\\MainMenu3D_exp.mdl",
				this.gameUI.trySkinField("GlueSpriteLayerBackground"));

		assertTrue(this.gameUI.hasSkinField("HumanBackdrop"));
		assertEquals("UI\\Glues\\SinglePlayer\\Alliance_Exp\\Alliance_Exp.mdl",
				this.gameUI.getSkinField("HumanBackdrop"));
		assertEquals("UI\\Glues\\SinglePlayer\\Alliance_Exp\\Alliance_Exp.mdl",
				this.gameUI.trySkinField("HumanBackdrop"));

		assertTrue(this.gameUI.hasSkinField("CampaignFile"));
		assertEquals("UI\\CampaignStrings_exp.txt", this.gameUI.getSkinField("CampaignFile"));
		assertEquals("UI\\CampaignStrings_exp.txt", this.gameUI.trySkinField("CampaignFile"));
	}

	@Test
	void reignOfChaosPrioritizesVersion0Overrides() {
		WarsmashConstants.GAME_VERSION = 0;

		assertTrue(this.gameUI.hasSkinField("GlueSpriteLayerBackground"));
		assertEquals("UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdl",
				this.gameUI.getSkinField("GlueSpriteLayerBackground"));
		assertEquals("UI\\Glues\\MainMenu\\MainMenu3D\\MainMenu3D.mdl",
				this.gameUI.trySkinField("GlueSpriteLayerBackground"));

		assertTrue(this.gameUI.hasSkinField("HumanBackdrop"));
		assertEquals("UI\\Glues\\SinglePlayer\\HumanCampaign3D\\HumanCampaign3D.mdl",
				this.gameUI.getSkinField("HumanBackdrop"));
		assertEquals("UI\\Glues\\SinglePlayer\\HumanCampaign3D\\HumanCampaign3D.mdl",
				this.gameUI.trySkinField("HumanBackdrop"));

		assertTrue(this.gameUI.hasSkinField("CampaignFile"));
		assertEquals("UI\\CampaignStrings.txt", this.gameUI.getSkinField("CampaignFile"));
		assertEquals("UI\\CampaignStrings.txt", this.gameUI.trySkinField("CampaignFile"));
	}

	@Test
	void unversionedFallbackWorksAcrossVersions() {
		WarsmashConstants.GAME_VERSION = 1;
		assertTrue(this.gameUI.hasSkinField("EscMenuBackground"));
		assertEquals("UI\\Widgets\\EscMenu\\Human\\edit-border.blp",
				this.gameUI.getSkinField("EscMenuBackground"));
		assertEquals("UI\\Widgets\\EscMenu\\Human\\edit-border.blp",
				this.gameUI.trySkinField("EscMenuBackground"));

		WarsmashConstants.GAME_VERSION = 0;
		assertTrue(this.gameUI.hasSkinField("EscMenuBackground"));
		assertEquals("UI\\Widgets\\EscMenu\\Human\\edit-border.blp",
				this.gameUI.getSkinField("EscMenuBackground"));
		assertEquals("UI\\Widgets\\EscMenu\\Human\\edit-border.blp",
				this.gameUI.trySkinField("EscMenuBackground"));
	}

	@Test
	void missingKeyThrowsOrReturnsFallback() {
		WarsmashConstants.GAME_VERSION = 1;

		assertFalse(this.gameUI.hasSkinField("NonExistentKey"));
		assertEquals("NonExistentKey", this.gameUI.trySkinField("NonExistentKey"));
		assertEquals("DirectModelPath.mdx", this.gameUI.trySkinField("DirectModelPath.mdx"));
		assertThrows(IllegalStateException.class, () -> this.gameUI.getSkinField("NonExistentKey"));

		assertFalse(this.gameUI.hasSkinField(null));
		assertThrows(NullPointerException.class, () -> this.gameUI.getSkinField(null));
		assertThrows(NullPointerException.class, () -> this.gameUI.trySkinField(null));
	}
}

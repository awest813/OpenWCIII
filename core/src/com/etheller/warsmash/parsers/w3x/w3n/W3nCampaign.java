package com.etheller.warsmash.parsers.w3x.w3n;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of a Warcraft III custom campaign (.w3n / war3campaign.w3f).
 */
public class W3nCampaign {
	private int version = 1;
	private int campaignVersion;
	private int editorVersion;
	private String name = "";
	private String difficulty = "";
	private String author = "";
	private String description = "";
	private int variableDifficulty;
	private int campaignBackground = -1;
	private String backgroundScreenModel = "";
	private String minimapPath = "";
	private int ambientSound = -1;
	private String customAmbientSoundPath = "";
	private int terrainFog;
	private final float[] fogHeight = new float[2];
	private float fogDensity;
	private final short[] fogColor = new short[4];
	private int uiRace;

	private final List<MapButton> mapButtons = new ArrayList<>();
	private final List<CampaignMap> maps = new ArrayList<>();

	public static class MapButton {
		private int visible;
		private String chapterTitle = "";
		private String chapterSubtitle = "";
		private String mapPath = "";

		public MapButton() {
		}

		public MapButton(final int visible, final String chapterTitle, final String chapterSubtitle, final String mapPath) {
			this.visible = visible;
			this.chapterTitle = chapterTitle != null ? chapterTitle : "";
			this.chapterSubtitle = chapterSubtitle != null ? chapterSubtitle : "";
			this.mapPath = mapPath != null ? mapPath : "";
		}

		public int getVisible() {
			return this.visible;
		}

		public void setVisible(final int visible) {
			this.visible = visible;
		}

		public boolean isVisible() {
			return this.visible != 0;
		}

		public String getChapterTitle() {
			return this.chapterTitle;
		}

		public void setChapterTitle(final String chapterTitle) {
			this.chapterTitle = chapterTitle != null ? chapterTitle : "";
		}

		public String getChapterSubtitle() {
			return this.chapterSubtitle;
		}

		public void setChapterSubtitle(final String chapterSubtitle) {
			this.chapterSubtitle = chapterSubtitle != null ? chapterSubtitle : "";
		}

		public String getMapPath() {
			return this.mapPath;
		}

		public void setMapPath(final String mapPath) {
			this.mapPath = mapPath != null ? mapPath : "";
		}
	}

	public static class CampaignMap {
		private int flags;
		private String mapPath = "";

		public CampaignMap() {
		}

		public CampaignMap(final int flags, final String mapPath) {
			this.flags = flags;
			this.mapPath = mapPath != null ? mapPath : "";
		}

		public int getFlags() {
			return this.flags;
		}

		public void setFlags(final int flags) {
			this.flags = flags;
		}

		public String getMapPath() {
			return this.mapPath;
		}

		public void setMapPath(final String mapPath) {
			this.mapPath = mapPath != null ? mapPath : "";
		}
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	public int getCampaignVersion() {
		return this.campaignVersion;
	}

	public void setCampaignVersion(final int campaignVersion) {
		this.campaignVersion = campaignVersion;
	}

	public int getEditorVersion() {
		return this.editorVersion;
	}

	public void setEditorVersion(final int editorVersion) {
		this.editorVersion = editorVersion;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name != null ? name : "";
	}

	public String getDifficulty() {
		return this.difficulty;
	}

	public void setDifficulty(final String difficulty) {
		this.difficulty = difficulty != null ? difficulty : "";
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(final String author) {
		this.author = author != null ? author : "";
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(final String description) {
		this.description = description != null ? description : "";
	}

	public int getVariableDifficulty() {
		return this.variableDifficulty;
	}

	public void setVariableDifficulty(final int variableDifficulty) {
		this.variableDifficulty = variableDifficulty;
	}

	public boolean isVariableDifficulty() {
		return this.variableDifficulty != 0;
	}

	public int getCampaignBackground() {
		return this.campaignBackground;
	}

	public void setCampaignBackground(final int campaignBackground) {
		this.campaignBackground = campaignBackground;
	}

	public String getBackgroundScreenModel() {
		return this.backgroundScreenModel;
	}

	public void setBackgroundScreenModel(final String backgroundScreenModel) {
		this.backgroundScreenModel = backgroundScreenModel != null ? backgroundScreenModel : "";
	}

	public String getMinimapPath() {
		return this.minimapPath;
	}

	public void setMinimapPath(final String minimapPath) {
		this.minimapPath = minimapPath != null ? minimapPath : "";
	}

	public int getAmbientSound() {
		return this.ambientSound;
	}

	public void setAmbientSound(final int ambientSound) {
		this.ambientSound = ambientSound;
	}

	public String getCustomAmbientSoundPath() {
		return this.customAmbientSoundPath;
	}

	public void setCustomAmbientSoundPath(final String customAmbientSoundPath) {
		this.customAmbientSoundPath = customAmbientSoundPath != null ? customAmbientSoundPath : "";
	}

	public int getTerrainFog() {
		return this.terrainFog;
	}

	public void setTerrainFog(final int terrainFog) {
		this.terrainFog = terrainFog;
	}

	public float[] getFogHeight() {
		return this.fogHeight;
	}

	public float getFogDensity() {
		return this.fogDensity;
	}

	public void setFogDensity(final float fogDensity) {
		this.fogDensity = fogDensity;
	}

	public short[] getFogColor() {
		return this.fogColor;
	}

	public int getUiRace() {
		return this.uiRace;
	}

	public void setUiRace(final int uiRace) {
		this.uiRace = uiRace;
	}

	public List<MapButton> getMapButtons() {
		return this.mapButtons;
	}

	public List<CampaignMap> getMaps() {
		return this.maps;
	}
}

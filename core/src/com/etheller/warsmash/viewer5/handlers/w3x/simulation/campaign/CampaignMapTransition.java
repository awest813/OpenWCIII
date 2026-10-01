package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;

/** Captures difficulty before the old mission is disposed and the next config is loaded. */
public final class CampaignMapTransition {
	private final String mapPath;
	private final CMapDifficulty difficulty;
	private final CMapDifficulty defaultDifficulty;

	public CampaignMapTransition(final String mapPath, final War3MapConfig config) {
		this.mapPath = mapPath;
		this.difficulty = config.getGameDifficulty();
		this.defaultDifficulty = config.getDefaultGameDifficulty();
	}

	public String getMapPath() {
		return this.mapPath;
	}

	public void applyDifficulty(final War3MapConfig config) {
		config.setGameDifficulty(this.difficulty);
		config.setDefaultGameDifficulty(this.defaultDifficulty);
	}
}

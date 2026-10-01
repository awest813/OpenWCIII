package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;

class CampaignMapTransitionTest {
	@Test
	void retryPreservesReducedDifficultyAndNextChapterRestoresSelectedDifficulty() {
		final War3MapConfig mission = new War3MapConfig(16);
		mission.setGameDifficulty(CMapDifficulty.HARD);
		mission.setDefaultGameDifficulty(CMapDifficulty.HARD);
		// The retail defeat action lowers only the current mission's difficulty.
		mission.setGameDifficulty(CMapDifficulty.NORMAL);
		final CampaignMapTransition retry = new CampaignMapTransition("Human01.w3m", mission);
		mission.setGameDifficulty(CMapDifficulty.EASY);
		final War3MapConfig reloaded = new War3MapConfig(16);
		retry.applyDifficulty(reloaded);
		assertEquals("Human01.w3m", retry.getMapPath());
		assertEquals(CMapDifficulty.NORMAL, reloaded.getGameDifficulty());
		assertEquals(CMapDifficulty.HARD, reloaded.getDefaultGameDifficulty());
		// CustomVictoryOkBJ restores the original selection before ChangeLevel.
		reloaded.setGameDifficulty(reloaded.getDefaultGameDifficulty());
		final War3MapConfig next = new War3MapConfig(16);
		new CampaignMapTransition("Human02.w3m", reloaded).applyDifficulty(next);
		assertEquals(CMapDifficulty.HARD, next.getGameDifficulty());
		assertEquals(CMapDifficulty.HARD, next.getDefaultGameDifficulty());
	}
}

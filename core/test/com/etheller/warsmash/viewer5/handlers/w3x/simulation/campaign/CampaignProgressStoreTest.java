package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignProgressStoreTest {

	@BeforeEach
	void resetStore() {
		CampaignProgressStore.get().reset();
	}

	@Test
	void missionDefaultsAvailableUntilLocked() {
		assertTrue(CampaignProgressStore.get().isMissionAvailable(0, 1));
		CampaignProgressStore.get().setMissionAvailable(0, 1, false);
		assertFalse(CampaignProgressStore.get().isMissionAvailable(0, 1));
		CampaignProgressStore.get().setMissionAvailable(0, 1, true);
		assertTrue(CampaignProgressStore.get().isMissionAvailable(0, 1));
	}

	@Test
	void campaignAndCinematicAvailabilityTracked() {
		CampaignProgressStore.get().setCampaignAvailable(2, false);
		assertFalse(CampaignProgressStore.get().isCampaignAvailable(2));
		CampaignProgressStore.get().setCampaignAvailable(2, true);
		assertTrue(CampaignProgressStore.get().isCampaignAvailable(2));
		CampaignProgressStore.get().setOpCinematicAvailable(1, 0, false);
		assertFalse(CampaignProgressStore.get().isOpCinematicAvailable(1, 0));
		CampaignProgressStore.get().setEdCinematicAvailable(1, 3, true);
		assertTrue(CampaignProgressStore.get().isEdCinematicAvailable(1, 3));
	}

	@Test
	void defaultOpenStyleSeedingCanLockCampaigns() {
		// Mirrors MenuUI seeding: non-default campaigns are explicitly locked.
		CampaignProgressStore.get().setCampaignAvailable(0, true);
		CampaignProgressStore.get().setCampaignAvailable(1, false);
		assertTrue(CampaignProgressStore.get().isCampaignAvailable(0));
		assertFalse(CampaignProgressStore.get().isCampaignAvailable(1));
	}

	@Test
	void menuRaceAndForceSelectScreen() {
		CampaignProgressStore.get().setCampaignMenuRace(3);
		assertEquals(3, CampaignProgressStore.get().getCampaignMenuRace());
		CampaignProgressStore.get().forceCampaignSelectScreen();
		assertTrue(CampaignProgressStore.get().consumeForceCampaignSelectScreen());
		assertFalse(CampaignProgressStore.get().consumeForceCampaignSelectScreen());
	}
}

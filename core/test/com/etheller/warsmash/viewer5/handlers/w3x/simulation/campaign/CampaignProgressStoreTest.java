package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.Preferences;

class CampaignProgressStoreTest {

	@BeforeEach
	@AfterEach
	void resetStore() {
		CampaignProgressStore.get().reset();
	}

	private static Preferences preferences() {
		final Map<String, Object> values = new HashMap<>();
		return (Preferences) Proxy.newProxyInstance(Preferences.class.getClassLoader(),
				new Class<?>[] { Preferences.class }, (proxy, method, args) -> {
					switch (method.getName()) {
					case "get": return new HashMap<>(values);
					case "putString": values.put((String) args[0], args[1]); return proxy;
					case "remove": values.remove(args[0]); return null;
					case "flush": return null;
					default: throw new UnsupportedOperationException(method.getName());
					}
				});
	}

	@Test
	void progressSurvivesReloadAndMenuDefaults() {
		final Preferences prefs = preferences();
		final CampaignProgressStore store = CampaignProgressStore.get();
		store.loadProfile(prefs, "Arthas");
		store.setCampaignAvailable(2, true);
		store.setMissionAvailable(2, 4, false);
		store.setOpCinematicAvailable(2, 0, false);
		store.setEdCinematicAvailable(2, 0, true);
		store.setTutorialCleared(true);
		store.setCampaignMenuRace(2);
		store.forceCampaignSelectScreen();
		store.loadProfile(prefs, "Arthas");
		store.seedCampaignAvailable(2, false);
		assertTrue(store.isCampaignAvailable(2));
		assertFalse(store.isMissionAvailable(2, 4));
		assertFalse(store.isOpCinematicAvailable(2, 0));
		assertTrue(store.isEdCinematicAvailable(2, 0));
		assertTrue(store.isTutorialCleared());
		assertEquals(2, store.getCampaignMenuRace());
		assertFalse(store.consumeForceCampaignSelectScreen());
	}

	@Test
	void switchingAndDeletingProfilesIsIsolated() {
		final Preferences prefs = preferences();
		final CampaignProgressStore store = CampaignProgressStore.get();
		store.loadProfile(prefs, "Player.一");
		store.setCampaignAvailable(3, true);
		store.loadProfile(prefs, "Player");
		store.seedCampaignAvailable(3, false);
		assertFalse(store.isCampaignAvailable(3));
		store.setMissionAvailable(0, 2, false);
		store.loadProfile(prefs, "Player.一");
		assertTrue(store.isCampaignAvailable(3));
		assertTrue(store.isMissionAvailable(0, 2));
		CampaignProgressStore.removeProfile(prefs, "Player.一");
		store.loadProfile(prefs, "Player.一");
		store.seedCampaignAvailable(3, false);
		assertFalse(store.isCampaignAvailable(3));
		store.loadProfile(prefs, "Player");
		assertFalse(store.isMissionAvailable(0, 2));
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

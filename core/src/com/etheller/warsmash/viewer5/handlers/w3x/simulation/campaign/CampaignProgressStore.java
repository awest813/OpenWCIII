package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Session-scoped campaign progress used by JASS availability natives
 * ({@code SetMissionAvailable}, {@code GetMissionAvailable}, etc.).
 *
 * <p>Default policy for missions matches Warsmash's prior "always available"
 * behavior for missions that have never been explicitly set: unknown missions
 * report as available so progression is not accidentally locked. Explicit
 * {@code false} values are honored.</p>
 *
 * <p>Campaign availability is seeded from {@code CampaignMenuData.DefaultOpen}
 * at menu load (non-default campaigns start locked). After that, JASS natives
 * control unlocks. Unknown campaigns still default to available if never seeded.</p>
 */
public final class CampaignProgressStore {

	private static final CampaignProgressStore INSTANCE = new CampaignProgressStore();

	private final Map<Long, Boolean> missionAvailable = new HashMap<>();
	private final Map<Integer, Boolean> campaignAvailable = new HashMap<>();
	private final Map<Long, Boolean> opCinematicAvailable = new HashMap<>();
	private final Map<Long, Boolean> edCinematicAvailable = new HashMap<>();
	private boolean tutorialCleared;
	private int campaignMenuRace;
	private final Set<Integer> visibleCustomCampaignButtons = new HashSet<>();
	private boolean forceCampaignSelectScreen;

	private CampaignProgressStore() {
	}

	public static CampaignProgressStore get() {
		return INSTANCE;
	}

	private static long key(final int campaign, final int index) {
		return (((long) campaign) << 32) | (index & 0xffffffffL);
	}

	public void setMissionAvailable(final int campaign, final int mission, final boolean available) {
		this.missionAvailable.put(key(campaign, mission), available);
	}

	public boolean isMissionAvailable(final int campaign, final int mission) {
		final Boolean value = this.missionAvailable.get(key(campaign, mission));
		return value == null || value.booleanValue();
	}

	public void setCampaignAvailable(final int campaign, final boolean available) {
		this.campaignAvailable.put(campaign, available);
	}

	public boolean isCampaignAvailable(final int campaign) {
		final Boolean value = this.campaignAvailable.get(campaign);
		return value == null || value.booleanValue();
	}

	public void setOpCinematicAvailable(final int campaign, final int index, final boolean available) {
		this.opCinematicAvailable.put(key(campaign, index), available);
	}

	public boolean isOpCinematicAvailable(final int campaign, final int index) {
		final Boolean value = this.opCinematicAvailable.get(key(campaign, index));
		return value == null || value.booleanValue();
	}

	public void setEdCinematicAvailable(final int campaign, final int index, final boolean available) {
		this.edCinematicAvailable.put(key(campaign, index), available);
	}

	public boolean isEdCinematicAvailable(final int campaign, final int index) {
		final Boolean value = this.edCinematicAvailable.get(key(campaign, index));
		return value == null || value.booleanValue();
	}

	public void setTutorialCleared(final boolean cleared) {
		this.tutorialCleared = cleared;
	}

	public boolean isTutorialCleared() {
		return this.tutorialCleared;
	}

	public void setCampaignMenuRace(final int race) {
		this.campaignMenuRace = race;
	}

	public int getCampaignMenuRace() {
		return this.campaignMenuRace;
	}

	public void setCustomCampaignButtonVisible(final int button, final boolean visible) {
		if (visible) {
			this.visibleCustomCampaignButtons.add(button);
		}
		else {
			this.visibleCustomCampaignButtons.remove(button);
		}
	}

	public boolean isCustomCampaignButtonVisible(final int button) {
		return this.visibleCustomCampaignButtons.contains(button);
	}

	public void forceCampaignSelectScreen() {
		this.forceCampaignSelectScreen = true;
	}

	public boolean consumeForceCampaignSelectScreen() {
		final boolean value = this.forceCampaignSelectScreen;
		this.forceCampaignSelectScreen = false;
		return value;
	}

	/** Clears all session progress (useful for tests). */
	public void reset() {
		this.missionAvailable.clear();
		this.campaignAvailable.clear();
		this.opCinematicAvailable.clear();
		this.edCinematicAvailable.clear();
		this.tutorialCleared = false;
		this.campaignMenuRace = 0;
		this.visibleCustomCampaignButtons.clear();
		this.forceCampaignSelectScreen = false;
	}
}

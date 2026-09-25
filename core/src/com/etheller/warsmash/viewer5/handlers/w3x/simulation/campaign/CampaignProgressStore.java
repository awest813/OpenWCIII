package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.badlogic.gdx.Preferences;

/**
 * Profile-scoped campaign progress used by JASS availability natives
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
	private Preferences preferences;
	private String profilePrefix;

	private CampaignProgressStore() {
	}

	public static CampaignProgressStore get() {
		return INSTANCE;
	}

	/** Switch profiles without leaking availability or transient menu requests. */
	public void loadProfile(final Preferences preferences, final String profile) {
		reset();
		this.preferences = preferences;
		this.profilePrefix = profilePrefix(profile);
		for (final Map.Entry<String, ?> entry : preferences.get().entrySet()) {
			if (!entry.getKey().startsWith(this.profilePrefix)) {
				continue;
			}
			final String key = entry.getKey().substring(this.profilePrefix.length());
			final String value = String.valueOf(entry.getValue());
			try {
				if (key.equals("race")) {
					this.campaignMenuRace = Integer.parseInt(value);
				}
				else if (value.equals("true") || value.equals("false")) {
					final boolean available = Boolean.parseBoolean(value);
					if (key.startsWith("mission.")) {
						this.missionAvailable.put(Long.parseLong(key.substring(8)), available);
					}
					else if (key.startsWith("campaign.")) {
						this.campaignAvailable.put(Integer.parseInt(key.substring(9)), available);
					}
					else if (key.startsWith("opening.")) {
						this.opCinematicAvailable.put(Long.parseLong(key.substring(8)), available);
					}
					else if (key.startsWith("ending.")) {
						this.edCinematicAvailable.put(Long.parseLong(key.substring(7)), available);
					}
					else if (key.equals("tutorial")) {
						this.tutorialCleared = available;
					}
				}
			}
			catch (final NumberFormatException ignored) {
				// A damaged entry must not prevent loading the rest of the profile.
			}
		}
	}

	private static String profilePrefix(final String profile) {
		return "CampaignProgress." + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(profile.getBytes(StandardCharsets.UTF_8)) + ".";
	}

	public static void removeProfile(final Preferences preferences, final String profile) {
		final String prefix = profilePrefix(profile);
		for (final String key : new HashSet<>(preferences.get().keySet())) {
			if (key.startsWith(prefix)) {
				preferences.remove(key);
			}
		}
		preferences.flush();
	}

	private void persist(final String key, final Object value) {
		if (this.preferences != null) {
			this.preferences.putString(this.profilePrefix + key, String.valueOf(value));
			this.preferences.flush();
		}
	}

	/** Menu defaults must never overwrite an explicit script unlock or lock. */
	public void seedCampaignAvailable(final int campaign, final boolean available) {
		this.campaignAvailable.putIfAbsent(campaign, available);
	}

	private static long key(final int campaign, final int index) {
		return (((long) campaign) << 32) | (index & 0xffffffffL);
	}

	public void setMissionAvailable(final int campaign, final int mission, final boolean available) {
		this.missionAvailable.put(key(campaign, mission), available);
		persist("mission." + key(campaign, mission), available);
	}

	public boolean isMissionAvailable(final int campaign, final int mission) {
		final Boolean value = this.missionAvailable.get(key(campaign, mission));
		return value == null || value.booleanValue();
	}

	public void setCampaignAvailable(final int campaign, final boolean available) {
		this.campaignAvailable.put(campaign, available);
		persist("campaign." + campaign, available);
	}

	public boolean isCampaignAvailable(final int campaign) {
		final Boolean value = this.campaignAvailable.get(campaign);
		return value == null || value.booleanValue();
	}

	public void setOpCinematicAvailable(final int campaign, final int index, final boolean available) {
		this.opCinematicAvailable.put(key(campaign, index), available);
		persist("opening." + key(campaign, index), available);
	}

	public boolean isOpCinematicAvailable(final int campaign, final int index) {
		final Boolean value = this.opCinematicAvailable.get(key(campaign, index));
		return value == null || value.booleanValue();
	}

	public void setEdCinematicAvailable(final int campaign, final int index, final boolean available) {
		this.edCinematicAvailable.put(key(campaign, index), available);
		persist("ending." + key(campaign, index), available);
	}

	public boolean isEdCinematicAvailable(final int campaign, final int index) {
		final Boolean value = this.edCinematicAvailable.get(key(campaign, index));
		return value == null || value.booleanValue();
	}

	public void setTutorialCleared(final boolean cleared) {
		this.tutorialCleared = cleared;
		persist("tutorial", cleared);
	}

	public boolean isTutorialCleared() {
		return this.tutorialCleared;
	}

	public void setCampaignMenuRace(final int race) {
		this.campaignMenuRace = race;
		persist("race", race);
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
		this.preferences = null;
		this.profilePrefix = null;
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

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.ProfileGameCacheStore;

/** Mission save slots belong to a profile, including the Quick Save slot. */
public final class MissionSaveStore {
	private MissionSaveStore() { }
	public static File defaultRoot() {
		return new File(new File(System.getProperty("user.home"), ".warsmash"), "saves");
	}
	public static File currentDirectory() {
		return ProfileGameCacheStore.profileDirectory(defaultRoot(), CampaignProgressStore.get().getProfileName());
	}
	/** Keep deleted profiles' files recoverable without giving them to a recreated profile. */
	public static void retireProfile(final File root, final String profile) throws IOException {
		final File directory = ProfileGameCacheStore.profileDirectory(root, profile);
		if (!directory.exists()) return;
		final File retired = new File(root, "retired");
		Files.createDirectories(retired.toPath());
		Files.move(directory.toPath(), new File(retired, directory.getName() + "-" + UUID.randomUUID()).toPath());
	}
}

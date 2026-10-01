package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.ProfileGameCacheStore;

class MissionSaveStoreTest {
	@TempDir File root;
	@Test void identicalSlotsAndCaseOnlyProfileNamesRemainIndependent() throws IOException {
		final File first = ProfileGameCacheStore.profileDirectory(this.root, "Player");
		final File second = ProfileGameCacheStore.profileDirectory(this.root, "player");
		assertNotEquals(first, second);
		Files.createDirectories(first.toPath()); Files.createDirectories(second.toPath());
		Files.writeString(new File(first, "QuickSave.w3s").toPath(), "first mission");
		Files.writeString(new File(second, "QuickSave.w3s").toPath(), "second mission");
		assertEquals("first mission", Files.readString(new File(first, "QuickSave.w3s").toPath()));
		assertEquals("second mission", Files.readString(new File(second, "QuickSave.w3s").toPath()));
		assertEquals(first, ProfileGameCacheStore.profileDirectory(this.root, "Player"));
	}
	@Test void deletionRetiresSaveSlotsAndRecreationStartsEmpty() throws IOException {
		final File profile = ProfileGameCacheStore.profileDirectory(this.root, "../../profile");
		assertTrue(profile.toPath().normalize().startsWith(this.root.toPath()));
		Files.createDirectories(profile.toPath());
		Files.writeString(new File(profile, "QuickSave.w3s").toPath(), "recoverable mission");
		MissionSaveStore.retireProfile(this.root, "../../profile");
		assertFalse(profile.exists());
		final File[] retired = new File(this.root, "retired").listFiles();
		assertNotNull(retired); assertEquals(1, retired.length);
		assertEquals("recoverable mission", Files.readString(new File(retired[0], "QuickSave.w3s").toPath()));
		Files.createDirectories(profile.toPath());
		assertEquals(0, profile.list().length);
	}
}

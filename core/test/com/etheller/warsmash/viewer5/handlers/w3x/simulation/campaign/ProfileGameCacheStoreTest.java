package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;

class ProfileGameCacheStoreTest {
	@TempDir File root;

	private static File save(final File directory, final String value) throws IOException {
		final CGameCache cache = new CGameCache("Campaign.w3v");
		cache.storeString("Human01", "Hero", value);
		final File file = new File(directory, cache.getName());
		cache.save(file);
		return file;
	}

	private static String hero(final File directory) {
		final CGameCache cache = CGameCache.tryLoadFromFile(new File(directory, "Campaign.w3v"), "Campaign.w3v");
		return cache == null ? null : cache.getStoredString("Human01", "Hero");
	}

	@Test void profilesKeepIndependentCachesWhenReopened() throws IOException {
		final File arthas = ProfileGameCacheStore.prepareProfile(this.root, "Arthas");
		save(arthas, "Paladin");
		final File thrall = ProfileGameCacheStore.prepareProfile(this.root, "Thrall");
		assertNull(hero(thrall));
		save(thrall, "Far Seer");
		assertEquals("Paladin", hero(ProfileGameCacheStore.prepareProfile(this.root, "Arthas")));
		assertEquals("Far Seer", hero(ProfileGameCacheStore.prepareProfile(this.root, "Thrall")));
	}

	@Test void legacyCachesBelongOnlyToOriginalProfileAndNeverOverwriteNewerData() throws IOException {
		save(this.root, "Legacy hero");
		ProfileGameCacheStore.claimLegacyOwner(this.root, "Original");
		assertNull(hero(ProfileGameCacheStore.prepareProfile(this.root, "New profile")));
		final File original = ProfileGameCacheStore.prepareProfile(this.root, "Original");
		assertEquals("Legacy hero", hero(original));
		save(original, "New hero");
		// An interrupted import can retry without clobbering a newer profile cache.
		Files.delete(new File(this.root, ".legacy-imported").toPath());
		assertEquals("New hero", hero(ProfileGameCacheStore.prepareProfile(this.root, "Original")));
		assertEquals("Legacy hero", hero(this.root));
	}

	@Test void deletingAndRecreatingNameStartsFreshWithoutDestroyingRecoveryData() throws IOException {
		save(this.root, "Legacy hero");
		ProfileGameCacheStore.claimLegacyOwner(this.root, "Original");
		final File original = ProfileGameCacheStore.prepareProfile(this.root, "Original");
		save(original, "Latest hero");
		final File other = ProfileGameCacheStore.prepareProfile(this.root, "Other");
		save(other, "Other hero");
		ProfileGameCacheStore.retireProfile(this.root, "Original");
		assertNull(hero(ProfileGameCacheStore.prepareProfile(this.root, "Original")));
		assertEquals("Other hero", hero(other));
		assertEquals("Legacy hero", hero(this.root));
		final File[] retired = new File(this.root, "retired").listFiles();
		assertNotNull(retired);
		assertEquals(1, retired.length);
		assertEquals("Latest hero", hero(retired[0]));
	}

	@Test void deletingLegacyOwnerBeforeFirstMissionPreventsLaterReimport() throws IOException {
		save(this.root, "Legacy hero");
		ProfileGameCacheStore.claimLegacyOwner(this.root, "Original");
		ProfileGameCacheStore.retireProfile(this.root, "Original");
		assertNull(hero(ProfileGameCacheStore.prepareProfile(this.root, "Original")));
		assertEquals("Legacy hero", hero(this.root));
	}

	@Test void profileIdentifiersStayBoundedAndDistinctOnCaseInsensitiveFilesystems() throws IOException {
		final File a = ProfileGameCacheStore.prepareProfile(this.root, "aaa");
		final File b = ProfileGameCacheStore.prepareProfile(this.root, "aaG");
		assertNotEquals(a.getName().toLowerCase(Locale.ROOT), b.getName().toLowerCase(Locale.ROOT));
		for (final String profile : new String[] {"", "../outside\\directory", "英雄", "x".repeat(500)}) {
			final File directory = ProfileGameCacheStore.prepareProfile(this.root, profile);
			assertEquals(new File(this.root, "profiles").getCanonicalFile(), directory.getParentFile().getCanonicalFile());
			assertEquals(66, directory.getName().length());
		}
	}

	@Test void damagedOwnershipRecordStopsImportInsteadOfAssigningAnotherProfilesHero() throws IOException {
		save(this.root, "Legacy hero");
		Files.writeString(new File(this.root, ".legacy-owner").toPath(), "damaged");
		assertThrows(IOException.class, () -> ProfileGameCacheStore.prepareProfile(this.root, "Other"));
		assertEquals("Legacy hero", hero(this.root));
	}
}

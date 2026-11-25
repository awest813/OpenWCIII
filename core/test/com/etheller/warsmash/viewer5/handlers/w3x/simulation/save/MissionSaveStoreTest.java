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
	@Test void namedSlotsStayInTheActiveProfileDirectory() {
		final File slot = MissionSaveStore.namedSlot("  Frostmourne.W3S  ");
		assertEquals(MissionSaveStore.currentDirectory(), slot.getParentFile());
		assertEquals("Frostmourne.w3s", slot.getName());
		for (final String invalid : java.util.List.of("", "../escape", "..\\escape", "C:alternate", "NUL",
				"Chapter\u0000", "Chapter.", " .w3s")) {
			assertThrows(IllegalArgumentException.class, () -> MissionSaveStore.namedSlot(invalid));
		}
	}

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

	@Test void campaignZoneArchivesKeepTheirFoldersAndReadLegacyFlatSlots() throws IOException {
		final String zones = "Blizzard\\Zones\\OrcX01.w3z";
		final String manual = "Blizzard\\Durotar\\OrcX01.w3z";
		final File zoneSlot = MissionSaveStore.scriptFile(this.root, zones);
		final File manualSlot = MissionSaveStore.scriptFile(this.root, manual);
		assertEquals(new File(this.root, "Blizzard/Zones/OrcX01.w3z.w3s"), zoneSlot);
		assertNotEquals(zoneSlot, manualSlot);
		final File legacy = new File(this.root, new File(zones).getName() + ".w3s");
		Files.writeString(legacy.toPath(), "previous archive");
		assertEquals(legacy, MissionSaveStore.findScriptFile(this.root, zones));
		Files.createDirectories(zoneSlot.toPath().getParent());
		Files.createDirectories(manualSlot.toPath().getParent());
		Files.writeString(zoneSlot.toPath(), "first zone");
		Files.writeString(manualSlot.toPath(), "manual mission archive");
		assertEquals(zoneSlot, MissionSaveStore.findScriptFile(this.root, zones));
		assertTrue(MissionSaveStore.removeScriptDirectory(this.root, "Blizzard\\Zones"));
		assertFalse(zoneSlot.exists());
		assertEquals("manual mission archive", Files.readString(manualSlot.toPath()));
		for (final String invalid : java.util.List.of("..\\outside", "Blizzard\\..\\outside", "/outside",
				"C:\\outside", "\\\\remote\\outside")) {
			assertThrows(IOException.class, () -> MissionSaveStore.scriptFile(this.root, invalid));
		}
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

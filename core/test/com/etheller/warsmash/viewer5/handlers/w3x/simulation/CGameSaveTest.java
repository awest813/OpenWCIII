package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.util.War3ID;

import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.ArrayJassType;
import com.etheller.interpreter.ast.value.ArrayJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.util.JassSettings;

class CGameSaveTest {
	@Test
	void inPlaceLoadRejectsOtherMapsAndLegacySaves() {
		final CGameSave save = new CGameSave("QuickSave", 1);
		org.junit.jupiter.api.Assertions.assertFalse(save.belongsToMap("Maps/Campaign/Human01.w3m"));
		save.savedMapPath = "Maps\\Campaign\\Human01.w3m";
		assertTrue(save.belongsToMap("maps/campaign/HUMAN01.w3m"));
		org.junit.jupiter.api.Assertions.assertFalse(save.belongsToMap("Maps/Campaign/Human02.w3m"));
		org.junit.jupiter.api.Assertions.assertFalse(save.belongsToMap(null));
	}

	@TempDir
	File tmpDir;

	@Test
	void scriptArraysRoundtripWithoutAliasingAndClearLaterChanges() throws IOException {
		final GlobalScope scope = new GlobalScope();
		final JassType[] types = { JassType.INTEGER, JassType.REAL, JassType.BOOLEAN, JassType.STRING };
		final JassValue[] values = { IntegerJassValue.of(42), RealJassValue.of(1.25),
				BooleanJassValue.TRUE, StringJassValue.of("") };
		for (int i = 0; i < types.length; i++) {
			scope.createGlobalArray("state" + i, new ArrayJassType(types[i]));
			((ArrayJassValue) scope.getGlobal("state" + i)).set(scope, JassSettings.MAX_ARRAY_SIZE - 1, values[i]);
		}
		scope.createGlobal("nullable", JassType.STRING, StringJassValue.of(null));
		((ArrayJassValue) scope.getGlobal("state1")).set(scope, 3, IntegerJassValue.of(7));
		final CGameSave save = new CGameSave("mission", 1);
		save.collectGlobals(scope);
		final ArrayJassValue original = (ArrayJassValue) scope.getGlobal("state0");
		original.set(scope, JassSettings.MAX_ARRAY_SIZE - 1, IntegerJassValue.of(99));
		original.set(scope, 20, IntegerJassValue.of(12));
		((ArrayJassValue) scope.getGlobal("state3")).set(scope, 20, StringJassValue.of("later"));
		final File file = new File(this.tmpDir, "arrays.w3s");
		save.save(file);
		assertTrue(file.length() < 1024, "Default array entries should not bloat every save");
		final CGameSave loaded = CGameSave.tryLoad(file);
		assertNotNull(loaded);
		loaded.restoreGlobals(scope);
		assertSame(original, scope.getGlobal("state0"));
		assertEquals(42, ((IntegerJassValue) original.get(JassSettings.MAX_ARRAY_SIZE - 1)).getValue());
		assertEquals(0, ((IntegerJassValue) original.get(20)).getValue());
		assertEquals(1.25, ((RealJassValue) ((ArrayJassValue) scope.getGlobal("state1"))
				.get(JassSettings.MAX_ARRAY_SIZE - 1)).getValue());
		assertEquals(7.0, ((RealJassValue) ((ArrayJassValue) scope.getGlobal("state1")).get(3)).getValue());
		assertEquals(BooleanJassValue.TRUE, ((ArrayJassValue) scope.getGlobal("state2"))
				.get(JassSettings.MAX_ARRAY_SIZE - 1));
		assertEquals("", ((StringJassValue) ((ArrayJassValue) scope.getGlobal("state3"))
				.get(JassSettings.MAX_ARRAY_SIZE - 1)).getValue());
		assertNull(((StringJassValue) ((ArrayJassValue) scope.getGlobal("state3")).get(20)).getValue());
		assertNull(((StringJassValue) scope.getGlobal("nullable")).getValue());
	}

	@Test
	void failedOverwriteRetainsLastGoodSaveAndCleansTemporaryFile() throws IOException {
		final File file = new File(this.tmpDir, "QuickSave.w3s");
		final CGameSave save = new CGameSave("mission", 1);
		save.gold[0] = 123;
		save.save(file);
		save.gold[0] = 999;
		save.globals.put("tooLong", StringJassValue.of("x".repeat(70000)));
		assertThrows(IOException.class, () -> save.save(file));
		assertEquals(123, CGameSave.tryLoad(file).gold[0]);
		assertEquals(1, this.tmpDir.list().length);
	}

	@Test
	void malformedCountsAndUnknownValueTypesAreRejected() throws IOException {
		for (final int count : new int[] { -1, Integer.MAX_VALUE }) {
			final File file = new File(this.tmpDir, "bad-count.w3s");
			try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(file))) {
				out.writeInt(0x57335331);
				out.writeInt(1);
				out.writeUTF("mission");
				out.writeInt(count);
			}
			assertNull(CGameSave.tryLoad(file));
		}
		final File file = new File(this.tmpDir, "bad-type.w3s");
		try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(file))) {
			out.writeInt(0x57335331);
			out.writeInt(1);
			out.writeUTF("mission");
			out.writeInt(1);
			out.writeUTF("udg_Bad");
			out.writeByte(99);
			out.writeInt(0);
		}
		assertNull(CGameSave.tryLoad(file));
	}

	@Test
	void truncatedAndTrailingDataAreRejected() throws IOException {
		final File file = new File(this.tmpDir, "truncated.w3s");
		new CGameSave("mission", 1).save(file);
		final byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
		java.nio.file.Files.write(file.toPath(), java.util.Arrays.copyOf(bytes, bytes.length - 1));
		assertNull(CGameSave.tryLoad(file));
		java.nio.file.Files.write(file.toPath(), java.util.Arrays.copyOf(bytes, bytes.length + 1));
		assertNull(CGameSave.tryLoad(file));
	}

	@Test
	void v4SavesStillLoadWithoutArraySection() throws IOException {
		final File file = new File(this.tmpDir, "legacy-v4.w3s");
		final CGameSave save = new CGameSave("mission", 1);
		save.gold[0] = 321;
		save.save(file);
		final byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
		java.nio.ByteBuffer.wrap(bytes).putInt(4, 4);
		java.nio.file.Files.write(file.toPath(), java.util.Arrays.copyOf(bytes, bytes.length - 4));
		final CGameSave loaded = CGameSave.tryLoad(file);
		assertNotNull(loaded);
		assertEquals(321, loaded.gold[0]);
	}

	@Test
	void arraysRestoreIntoFreshMapScopeAndSkipChangedTypes() throws IOException {
		final GlobalScope oldScope = new GlobalScope();
		oldScope.createGlobalArray("missionFlags", new ArrayJassType(JassType.BOOLEAN));
		oldScope.createGlobalArray("changed", new ArrayJassType(JassType.INTEGER));
		((ArrayJassValue) oldScope.getGlobal("missionFlags")).set(oldScope, 4, BooleanJassValue.TRUE);
		final CGameSave save = new CGameSave("mission", 1);
		save.collectGlobals(oldScope);
		final File file = new File(this.tmpDir, "fresh.w3s");
		save.save(file);
		final GlobalScope newScope = new GlobalScope();
		newScope.createGlobalArray("missionFlags", new ArrayJassType(JassType.BOOLEAN));
		newScope.createGlobalArray("changed", new ArrayJassType(JassType.STRING));
		((ArrayJassValue) newScope.getGlobal("changed")).set(newScope, 0, StringJassValue.of("new type"));
		CGameSave.tryLoad(file).restoreGlobals(newScope);
		assertEquals(BooleanJassValue.TRUE, ((ArrayJassValue) newScope.getGlobal("missionFlags")).get(4));
		assertEquals("new type", ((StringJassValue) ((ArrayJassValue) newScope.getGlobal("changed")).get(0)).getValue());
	}

	@Test
	void corruptArrayIndexOrElementTypeRejectsEntireSave() throws IOException {
		final File file = new File(this.tmpDir, "bad-array.w3s");
		new CGameSave("mission", 1).save(file);
		final byte[] base = java.nio.file.Files.readAllBytes(file.toPath());
		for (final int index : new int[] { -1, JassSettings.MAX_ARRAY_SIZE, 0 }) {
			try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(file))) {
				out.write(base, 0, base.length - 4);
				out.writeInt(1);
				out.writeUTF("state");
				out.writeByte(0); // integer array
				out.writeInt(1);
				out.writeInt(index);
				out.writeByte(3); // wrong element type, even when index is valid
				out.writeUTF("invalid");
			}
			assertNull(CGameSave.tryLoad(file));
		}
	}

	// ---- roundtrip ----

	@Test
	void roundtripIntegerGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_KillCount", IntegerJassValue.of(42));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_KillCount");
		assertNotNull(v);
		assertTrue(v instanceof IntegerJassValue);
		assertEquals(42, ((IntegerJassValue) v).getValue());
	}

	@Test
	void roundtripRealGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_Timer", RealJassValue.of(3.14));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_Timer");
		assertNotNull(v);
		assertTrue(v instanceof RealJassValue);
		assertEquals(3.14, ((RealJassValue) v).getValue(), 1e-9);
	}

	@Test
	void roundtripBooleanGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_QuestDone", BooleanJassValue.TRUE);
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_QuestDone");
		assertNotNull(v);
		assertTrue(v instanceof BooleanJassValue);
		assertEquals(BooleanJassValue.TRUE, v);
	}

	@Test
	void roundtripStringGlobal() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_HeroName", StringJassValue.of("Arthas"));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		final JassValue v = loaded.globals.get("udg_HeroName");
		assertNotNull(v);
		assertTrue(v instanceof StringJassValue);
		assertEquals("Arthas", ((StringJassValue) v).getValue());
	}

	@Test
	void roundtripPlayerResources() throws IOException {
		final CGameSave save = new CGameSave("mission01", 4);
		save.gold[0] = 500;
		save.lumber[0] = 200;
		save.gold[1] = 0;
		save.lumber[1] = 0;
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(4, loaded.gold.length);
		assertEquals(500, loaded.gold[0]);
		assertEquals(200, loaded.lumber[0]);
		assertEquals(0, loaded.gold[1]);
	}

	@Test
	void roundtripMapPath() throws IOException {
		final CGameSave save = new CGameSave("Human\\Human01", 1);
		final File f = new File(this.tmpDir, "Human01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("Human\\Human01", loaded.mapPath);
	}

	@Test
	void roundtripMultipleGlobals() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_A", IntegerJassValue.of(1));
		save.globals.put("udg_B", RealJassValue.of(2.5));
		save.globals.put("udg_C", BooleanJassValue.FALSE);
		save.globals.put("udg_D", StringJassValue.of("hello"));
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(4, loaded.globals.size());
		assertEquals(1, ((IntegerJassValue) loaded.globals.get("udg_A")).getValue());
		assertEquals(2.5, ((RealJassValue) loaded.globals.get("udg_B")).getValue(), 1e-9);
		assertEquals(BooleanJassValue.FALSE, loaded.globals.get("udg_C"));
		assertEquals("hello", ((StringJassValue) loaded.globals.get("udg_D")).getValue());
	}

	// ---- error handling ----

	@Test
	void tryLoadReturnsNullForMissingFile() {
		final File f = new File(this.tmpDir, "nonexistent.w3s");
		assertNull(CGameSave.tryLoad(f));
	}

	@Test
	void tryLoadReturnsNullForCorruptFile() throws IOException {
		final File f = new File(this.tmpDir, "corrupt.w3s");
		java.nio.file.Files.write(f.toPath(), new byte[]{0x00, 0x01, 0x02, 0x03});
		assertNull(CGameSave.tryLoad(f));
	}

	@Test
	void saveDirCreatedIfMissing() throws IOException {
		final File subDir = new File(this.tmpDir, "nested" + File.separator + "saves");
		final CGameSave save = new CGameSave("test", 1);
		final File f = new File(subDir, "test.w3s");
		save.save(f);
		assertTrue(f.exists());
	}

	// ---- v2: clock + camera ----

	@Test
	void roundtripClockAndCamera() throws IOException {
		final CGameSave save = new CGameSave("mission01", 2);
		save.globals.put("udg_KillCount", IntegerJassValue.of(7));
		save.gold[0] = 500;
		save.lumber[0] = 200;
		save.timeOfDay = 12.5f;
		save.timeOfDayScale = 1.5f;
		save.cameraX = 1234.5f;
		save.cameraY = -2345.25f;
		final File f = new File(this.tmpDir, "mission01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(12.5f, loaded.timeOfDay, 1e-6);
		assertEquals(1.5f, loaded.timeOfDayScale, 1e-6);
		assertEquals(1234.5f, loaded.cameraX, 1e-6);
		assertEquals(-2345.25f, loaded.cameraY, 1e-6);
		assertEquals(500, loaded.gold[0]);
		assertEquals(7, ((IntegerJassValue) loaded.globals.get("udg_KillCount")).getValue());
	}

	@Test
	void v1SavesLoadWithAbsentClockAndCamera() throws IOException {
		final File f = new File(this.tmpDir, "legacy.w3s");
		try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(f))) {
			out.writeInt(0x57335331);
			out.writeInt(1);
			out.writeUTF("mission01");
			out.writeInt(1);
			out.writeUTF("udg_KillCount");
			out.writeByte(0);
			out.writeInt(42);
			out.writeInt(2);
			out.writeInt(500);
			out.writeInt(200);
			out.writeInt(0);
			out.writeInt(0);
		}

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("mission01", loaded.mapPath);
		assertEquals(42, ((IntegerJassValue) loaded.globals.get("udg_KillCount")).getValue());
		assertEquals(500, loaded.gold[0]);
		assertEquals(200, loaded.lumber[0]);
		assertTrue(Float.isNaN(loaded.timeOfDay));
		assertTrue(Float.isNaN(loaded.timeOfDayScale));
		assertTrue(Float.isNaN(loaded.cameraX));
		assertTrue(Float.isNaN(loaded.cameraY));
		assertEquals("", loaded.savedMapPath);
	}

	// ---- v3: saved map path ----

	@Test
	void roundtripSavedMapPath() throws IOException {
		final CGameSave save = new CGameSave("Human01", 2);
		save.savedMapPath = "Maps\\Campaign\\Human01.w3x";
		save.timeOfDay = 8.0f;
		final File f = new File(this.tmpDir, "Human01.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("Maps\\Campaign\\Human01.w3x", loaded.savedMapPath);
		assertEquals(8.0f, loaded.timeOfDay, 1e-6);
	}

	@Test
	void v2SavesLoadWithAbsentMapPath() throws IOException {
		final File f = new File(this.tmpDir, "v2.w3s");
		try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(f))) {
			out.writeInt(0x57335331);
			out.writeInt(2);
			out.writeUTF("mission01");
			out.writeInt(0);
			out.writeInt(1);
			out.writeInt(100);
			out.writeInt(50);
			out.writeFloat(12.0f);
			out.writeFloat(1.0f);
			out.writeFloat(0.0f);
			out.writeFloat(0.0f);
		}

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals(12.0f, loaded.timeOfDay, 1e-6);
		assertEquals("", loaded.savedMapPath);
	}

	// ---- v4: full simulation entities ----

	@Test
	void roundtripV4FullSimulationEntities() throws IOException {
		final CGameSave save = new CGameSave("Human01", 2);
		save.savedMapPath = "Maps\\Campaign\\Human01.w3x";
		save.timeOfDay = 14.5f;

		// 1. Hero unit
		final War3ID arthas = War3ID.fromString("Hlhr");
		final War3ID holyLight = War3ID.fromString("AHhb");
		final War3ID potion = War3ID.fromString("phea");
		final StoredUnitData.StoredItemData[] items = new StoredUnitData.StoredItemData[6];
		items[0] = new StoredUnitData.StoredItemData(potion, 3);
		final List<StoredUnitData.StoredAbilityData> abilities = new ArrayList<>();
		abilities.add(new StoredUnitData.StoredAbilityData(holyLight, 2));

		final StoredUnitData unitData = new StoredUnitData(arthas, 1500, 1, 24, 15, 18, 2, 0, 0,
				"Arthas", items, abilities.toArray(new StoredUnitData.StoredAbilityData[0]));

		save.savedUnits.add(new CGameSave.SavedUnitState(arthas, 0, 1024f, 2048f, 270f, 650f, 250f, unitData));

		// 2. Destructable
		save.savedDestructables.add(new CGameSave.SavedDestructableState(999, 500f, 600f, 0f, true));

		// 3. Ground item
		save.savedGroundItems.add(new CGameSave.SavedItemState(potion, 700f, 800f, 100f, 1));

		final File f = new File(this.tmpDir, "Human01_v4.w3s");
		save.save(f);

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("Maps\\Campaign\\Human01.w3x", loaded.savedMapPath);
		assertEquals(14.5f, loaded.timeOfDay, 1e-6);

		// Assert units
		assertEquals(1, loaded.savedUnits.size());
		final CGameSave.SavedUnitState loadedUnit = loaded.savedUnits.get(0);
		assertEquals(arthas, loadedUnit.unitTypeId);
		assertEquals(0, loadedUnit.playerIndex);
		assertEquals(1024f, loadedUnit.x, 1e-4);
		assertEquals(2048f, loadedUnit.y, 1e-4);
		assertEquals(270f, loadedUnit.facing, 1e-4);
		assertEquals(650f, loadedUnit.life, 1e-4);
		assertEquals(250f, loadedUnit.mana, 1e-4);

		assertNotNull(loadedUnit.unitData);
		assertEquals("Arthas", loadedUnit.unitData.properName);
		assertEquals(1500, loadedUnit.unitData.xp);
		assertEquals(1, loadedUnit.unitData.skillPoints);
		assertEquals(24, loadedUnit.unitData.strengthBase);
		assertEquals(2, loadedUnit.unitData.strengthBonus);
		assertEquals(1, loadedUnit.unitData.abilities.length);
		assertEquals(holyLight, loadedUnit.unitData.abilities[0].abilityId);
		assertEquals(2, loadedUnit.unitData.abilities[0].level);
		assertNotNull(loadedUnit.unitData.items[0]);
		assertEquals(potion, loadedUnit.unitData.items[0].typeId);
		assertEquals(3, loadedUnit.unitData.items[0].charges);

		// Assert destructables
		assertEquals(1, loaded.savedDestructables.size());
		final CGameSave.SavedDestructableState loadedDest = loaded.savedDestructables.get(0);
		assertEquals(999, loadedDest.handleId);
		assertEquals(500f, loadedDest.x, 1e-4);
		assertEquals(600f, loadedDest.y, 1e-4);
		assertTrue(loadedDest.isDead);

		// Assert ground items
		assertEquals(1, loaded.savedGroundItems.size());
		final CGameSave.SavedItemState loadedItem = loaded.savedGroundItems.get(0);
		assertEquals(potion, loadedItem.typeId);
		assertEquals(700f, loadedItem.x, 1e-4);
		assertEquals(800f, loadedItem.y, 1e-4);
		assertEquals(1, loadedItem.charges);
	}

	@Test
	void v3SavesLoadWithEmptyEntities() throws IOException {
		final File f = new File(this.tmpDir, "v3.w3s");
		try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(f))) {
			out.writeInt(0x57335331);
			out.writeInt(3);
			out.writeUTF("mission01");
			out.writeInt(0);
			out.writeInt(1);
			out.writeInt(100);
			out.writeInt(50);
			out.writeFloat(12.0f);
			out.writeFloat(1.0f);
			out.writeFloat(0.0f);
			out.writeFloat(0.0f);
			out.writeUTF("Maps\\Test.w3x");
		}

		final CGameSave loaded = CGameSave.tryLoad(f);
		assertNotNull(loaded);
		assertEquals("Maps\\Test.w3x", loaded.savedMapPath);
		assertTrue(loaded.savedUnits.isEmpty());
		assertTrue(loaded.savedDestructables.isEmpty());
		assertTrue(loaded.savedGroundItems.isEmpty());
	}

	// ---- save listing ----

	@Test
	void listSavesFindsSavesSorted() throws IOException {
		new File(this.tmpDir, "b.w3s").createNewFile();
		new File(this.tmpDir, "A.w3s").createNewFile();
		new File(this.tmpDir, "notes.txt").createNewFile();
		new File(this.tmpDir, "c.W3S").createNewFile();

		final java.util.List<String> names = CGameSave.listSaves(this.tmpDir);
		assertEquals(java.util.Arrays.asList("A.w3s", "b.w3s", "c.W3S"), names);
	}

	@Test
	void listSavesEmptyForMissingDir() {
		final java.util.List<String> names = CGameSave.listSaves(new File(this.tmpDir, "nope"));
		assertTrue(names.isEmpty());
	}
}

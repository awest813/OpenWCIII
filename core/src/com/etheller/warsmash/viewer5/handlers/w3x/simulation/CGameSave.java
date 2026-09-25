package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.GlobalScopeAssignable;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.ArrayJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.util.JassSettings;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredAbilityData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData.StoredItemData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;

/**
 * Manages on-disk save-game state for a running campaign mission.
 *
 * <p>The binary format stores JASS primitive globals (integer, real, boolean,
 * string), per-player resource totals, and (since v2) the time-of-day clock
 * plus the local camera target. Version 5 adds sparse primitive script arrays
 * and preserves null strings. Version 4 introduced an entity snapshot scaffold;
 * gameplay does not yet restore entity identity, trigger/timer state, or queued
 * script execution, so this is not a complete mid-mission checkpoint.</p>
 *
 * <h3>File layout</h3>
 * <pre>
 *   magic    (int)   0x57335331  "W3S1"
 *   version  (int)   1 through 5
 *   mapPath  (UTF)   path passed to SaveGame
 *   nGlobals (int)
 *   for each global:
 *     name   (UTF)
 *     type   (byte)  0=int 1=real 2=bool 3=string 4=null string (v5)
 *     value  (varies)
 *   nPlayers (int)
 *   for each player:
 *     gold   (int)
 *     lumber (int)
 *   [v2+]
 *   timeOfDay      (float, NaN when absent)
 *   timeOfDayScale (float, NaN when absent)
 *   cameraX        (float, NaN when absent)
 *   cameraY        (float, NaN when absent)
 *   [v3+]
 *   savedMapPath (UTF, "" when absent: the map file to reload for this save)
 *   [v4+]
 *   nUnits (int)
 *   for each unit:
 *     typeId, playerIndex, x, y, facing, life, mana, StoredUnitData
 *   nDestructables (int)
 *   for each destructable:
 *     handleId, x, y, life, isDead
 *   nItems (int)
 *   for each item:
 *     typeId, x, y, life, charges
 *   [v5+]
 *   nArrays (int)
 *   for each primitive array:
 *     name (UTF), elementType (byte), nonDefaultCount (int)
 *     for each non-default entry: index (int), typed value
 * </pre>
 */
public final class CGameSave {
	private static final int FILE_MAGIC = 0x57335331; // "W3S1"
	private static final int FILE_VERSION = 5;
	private static final int FILE_VERSION_MIN = 1;

	private static final byte TYPE_INT = 0;
	private static final byte TYPE_REAL = 1;
	private static final byte TYPE_BOOL = 2;
	private static final byte TYPE_STRING = 3;
	private static final byte TYPE_NULL_STRING = 4;

	/** Saved JASS primitive globals: name → JassValue (int/real/bool/string). */
	public final Map<String, JassValue> globals;
	private final Map<String, SavedArray> arrays = new HashMap<>();

	private static final class SavedArray {
		private final byte type;
		private final Map<Integer, JassValue> entries = new HashMap<>();

		private SavedArray(final byte type) {
			this.type = type;
		}
	}

	/** Per-player gold totals (indexed by player slot). */
	public final int[] gold;

	/** Per-player lumber totals (indexed by player slot). */
	public final int[] lumber;

	/** Map path as supplied to SaveGame. */
	public final String mapPath;

	/**
	 * Time-of-day clock at save time, or {@code Float.NaN} when not recorded
	 * (e.g. saves written by v1).
	 */
	public float timeOfDay = Float.NaN;

	/** Time-of-day scale at save time, or {@code Float.NaN} when not recorded. */
	public float timeOfDayScale = Float.NaN;

	/** Local camera target X at save time, or {@code Float.NaN} when not recorded. */
	public float cameraX = Float.NaN;

	/** Local camera target Y at save time, or {@code Float.NaN} when not recorded. */
	public float cameraY = Float.NaN;

	/**
	 * Map file to reload for this save (e.g. {@code Maps\Campaign\Human01.w3x}),
	 * or {@code ""} when not recorded (every v1/v2 save). Main-menu Load Saved
	 * refuses saves without it rather than guessing.
	 */
	public String savedMapPath = "";

	/** Partial in-place restores are only safe on the map that produced the save. */
	public boolean belongsToMap(final String currentMapPath) {
		return (currentMapPath != null) && (this.savedMapPath != null) && !this.savedMapPath.isEmpty()
				&& this.savedMapPath.replace('/', '\\').equalsIgnoreCase(currentMapPath.replace('/', '\\'));
	}

	/** v4: Live battlefield units snapshot. */
	public final List<SavedUnitState> savedUnits = new ArrayList<>();

	/** v4: Live modified destructables snapshot. */
	public final List<SavedDestructableState> savedDestructables = new ArrayList<>();

	/** v4: Ground items snapshot. */
	public final List<SavedItemState> savedGroundItems = new ArrayList<>();

	public static final class SavedUnitState {
		public final War3ID unitTypeId;
		public final int playerIndex;
		public final float x;
		public final float y;
		public final float facing;
		public final float life;
		public final float mana;
		public final StoredUnitData unitData;

		public SavedUnitState(final War3ID unitTypeId, final int playerIndex, final float x, final float y,
				final float facing, final float life, final float mana, final StoredUnitData unitData) {
			this.unitTypeId = unitTypeId;
			this.playerIndex = playerIndex;
			this.x = x;
			this.y = y;
			this.facing = facing;
			this.life = life;
			this.mana = mana;
			this.unitData = unitData;
		}
	}

	public static final class SavedDestructableState {
		public final int handleId;
		public final float x;
		public final float y;
		public final float life;
		public final boolean isDead;

		public SavedDestructableState(final int handleId, final float x, final float y, final float life,
				final boolean isDead) {
			this.handleId = handleId;
			this.x = x;
			this.y = y;
			this.life = life;
			this.isDead = isDead;
		}
	}

	public static final class SavedItemState {
		public final War3ID typeId;
		public final float x;
		public final float y;
		public final float life;
		public final int charges;

		public SavedItemState(final War3ID typeId, final float x, final float y, final float life, final int charges) {
			this.typeId = typeId;
			this.x = x;
			this.y = y;
			this.life = life;
			this.charges = charges;
		}
	}

	public CGameSave(final String mapPath, final int numPlayers) {
		this.mapPath = mapPath;
		this.gold = new int[numPlayers];
		this.lumber = new int[numPlayers];
		this.globals = new HashMap<>();
	}

	private CGameSave(final String mapPath, final int[] gold, final int[] lumber,
			final Map<String, JassValue> globals, final float timeOfDay, final float timeOfDayScale,
			final float cameraX, final float cameraY, final String savedMapPath,
			final List<SavedUnitState> savedUnits, final List<SavedDestructableState> savedDestructables,
			final List<SavedItemState> savedGroundItems) {
		this.mapPath = mapPath;
		this.gold = gold;
		this.lumber = lumber;
		this.globals = globals;
		this.timeOfDay = timeOfDay;
		this.timeOfDayScale = timeOfDayScale;
		this.cameraX = cameraX;
		this.cameraY = cameraY;
		this.savedMapPath = savedMapPath != null ? savedMapPath : "";
		if (savedUnits != null) {
			this.savedUnits.addAll(savedUnits);
		}
		if (savedDestructables != null) {
			this.savedDestructables.addAll(savedDestructables);
		}
		if (savedGroundItems != null) {
			this.savedGroundItems.addAll(savedGroundItems);
		}
	}

	// -----------------------------------------------------------------------
	// Collection helpers
	// -----------------------------------------------------------------------

	/**
	 * Captures primitive scalar globals and independent sparse array snapshots.
	 * Handle and code values are not persisted.
	 */
	public void collectGlobals(final GlobalScope globalScope) {
		this.globals.clear();
		this.arrays.clear();
		for (final Map.Entry<String, GlobalScopeAssignable> entry : globalScope.getAllGlobals().entrySet()) {
			final GlobalScopeAssignable assignable = entry.getValue();
			final JassValue value = assignable.getValue();
			if (value == null) {
				continue;
			}
			if (value instanceof ArrayJassValue) {
				final ArrayJassValue array = (ArrayJassValue) value;
				final byte type = primitiveType(array.getType().getPrimitiveType());
				if (type >= 0) {
					final SavedArray snapshot = new SavedArray(type);
					for (int i = 0; i < JassSettings.MAX_ARRAY_SIZE; i++) {
						JassValue item = array.get(i);
						if ((type == TYPE_REAL) && (item instanceof IntegerJassValue)) {
							item = RealJassValue.of(((IntegerJassValue) item).getValue());
						}
						if (!isDefault(item, type)) {
							snapshot.entries.put(i, item);
						}
					}
					this.arrays.put(entry.getKey(), snapshot);
				}
			}
			// Handle/code values require separate identity restoration.
			if (isPrimitive(value)) {
				this.globals.put(entry.getKey(), value);
			}
		}
	}

	/**
	 * Collects live battlefield units, destructables, and ground items into this save.
	 */
	public void collectSimulation(final CSimulation simulation) {
		this.savedUnits.clear();
		this.savedDestructables.clear();
		this.savedGroundItems.clear();

		if (simulation == null) {
			return;
		}

		// Units
		for (final CUnit unit : simulation.getUnitsForSave()) {
			if ((unit == null) || unit.isDead()) {
				continue;
			}
			final StoredUnitData unitData = snapshotUnit(unit);
			this.savedUnits.add(new SavedUnitState(unit.getTypeId(), unit.getPlayerIndex(), unit.getX(), unit.getY(),
					unit.getFacing(), unit.getLife(), unit.getMana(), unitData));
		}

		// Destructables
		for (final CDestructable dest : simulation.getDestructables()) {
			if (dest != null) {
				this.savedDestructables.add(new SavedDestructableState(dest.getHandleId(), dest.getX(), dest.getY(),
						dest.getLife(), dest.isDead()));
			}
		}

		// Ground items
		for (final CItem item : simulation.getItems()) {
			if ((item != null) && !item.isDead() && !item.isHidden() && (item.getContainedInventory() == null)) {
				this.savedGroundItems.add(new SavedItemState(item.getTypeId(), item.getX(), item.getY(),
						item.getLife(), item.getCharges()));
			}
		}
	}

	public static StoredUnitData snapshotUnit(final CUnit unit) {
		int xp = 0;
		int skillPoints = 0;
		int strBase = 0;
		int agiBase = 0;
		int intBase = 0;
		int strBonus = 0;
		int agiBonus = 0;
		int intBonus = 0;
		String properName = "";
		StoredAbilityData[] abilities = null;
		final CAbilityHero heroData = unit.getHeroData();
		if (heroData != null) {
			xp = heroData.getXp();
			skillPoints = heroData.getSkillPoints();
			strBase = heroData.getStrength().getBase();
			agiBase = heroData.getAgility().getBase();
			intBase = heroData.getIntelligence().getBase();
			strBonus = heroData.getStrength().getBonus();
			agiBonus = heroData.getAgility().getBonus();
			intBonus = heroData.getIntelligence().getBonus();
			properName = heroData.getProperName();
			final List<StoredAbilityData> learned = new ArrayList<>();
			for (final CAbility ability : unit.getAbilities()) {
				if (!(ability instanceof CLevelingAbility)) {
					continue;
				}
				final War3ID code = ability.getCode();
				if ((code == null) || !heroData.getSkillsAvailable().contains(code)) {
					continue;
				}
				final int level = ((CLevelingAbility) ability).getLevel();
				if (level > 0) {
					learned.add(new StoredAbilityData(code, level));
				}
			}
			if (!learned.isEmpty()) {
				abilities = learned.toArray(new StoredAbilityData[0]);
			}
		}
		StoredItemData[] items = null;
		final CAbilityInventory inventoryData = unit.getInventoryData();
		if (inventoryData != null) {
			items = new StoredItemData[inventoryData.getItemCapacity()];
			for (int slot = 0; slot < inventoryData.getItemCapacity(); slot++) {
				final CItem item = inventoryData.getItemInSlot(slot);
				if (item != null) {
					items[slot] = new StoredItemData(item.getTypeId(), item.getCharges());
				}
			}
		}
		return new StoredUnitData(unit.getTypeId(), xp, skillPoints, strBase, agiBase, intBase, strBonus, agiBonus,
				intBonus, properName, items, abilities);
	}

	/**
	 * Restores primitive globals from this save into {@code globalScope}.
	 * Globals that no longer exist in the scope are silently skipped.
	 */
	public void restoreGlobals(final GlobalScope globalScope) {
		for (final Map.Entry<String, SavedArray> entry : this.arrays.entrySet()) {
			final GlobalScopeAssignable assignable = globalScope.getAssignableGlobal(entry.getKey());
			if ((assignable == null) || !(assignable.getValue() instanceof ArrayJassValue)) {
				continue;
			}
			final ArrayJassValue array = (ArrayJassValue) assignable.getValue();
			final SavedArray snapshot = entry.getValue();
			if (primitiveType(array.getType().getPrimitiveType()) != snapshot.type) {
				continue;
			}
			// Keep the existing array object so script references remain valid.
			final JassValue empty = defaultValue(snapshot.type);
			for (int i = 0; i < JassSettings.MAX_ARRAY_SIZE; i++) {
				array.set(globalScope, i, snapshot.entries.getOrDefault(i, empty));
			}
		}
		for (final Map.Entry<String, JassValue> entry : this.globals.entrySet()) {
			try {
				final GlobalScopeAssignable assignable = globalScope.getAssignableGlobal(entry.getKey());
				if (assignable != null) {
					assignable.setValue(entry.getValue());
				}
			}
			catch (final Exception e) {
				System.err.println("CGameSave: could not restore global '" + entry.getKey() + "': " + e.getMessage());
			}
		}
	}

	/**
	 * Restores units, destructables, and ground items into {@code simulation}.
	 */
	public void restoreSimulation(final CSimulation simulation) {
		if (simulation == null) {
			return;
		}

		// Restore destructables
		for (final SavedDestructableState d : this.savedDestructables) {
			CDestructable match = null;
			for (final CDestructable dest : simulation.getDestructables()) {
				if (dest.getHandleId() == d.handleId) {
					match = dest;
					break;
				}
			}
			if (match == null) {
				float closestDistSq = Float.MAX_VALUE;
				for (final CDestructable dest : simulation.getDestructables()) {
					final float dx = dest.getX() - d.x;
					final float dy = dest.getY() - d.y;
					final float distSq = (dx * dx) + (dy * dy);
					if ((distSq < (32f * 32f)) && (distSq < closestDistSq)) {
						closestDistSq = distSq;
						match = dest;
					}
				}
			}
			if (match != null) {
				if (d.isDead) {
					match.setLife(simulation, 0f);
				}
				else {
					match.setLife(simulation, d.life);
				}
			}
		}

		// Restore ground items
		for (final SavedItemState item : this.savedGroundItems) {
			final CItem newItem = simulation.createItem(item.typeId, item.x, item.y);
			if (newItem != null) {
				newItem.setLife(simulation, item.life);
				newItem.setCharges(item.charges);
			}
		}

		// Apply saved vitals last: restoring attributes and gear changes their maxima.
		for (final SavedUnitState u : this.savedUnits) {
			final CUnit restored = u.unitData == null
					? simulation.createUnitSimple(u.unitTypeId, u.playerIndex, u.x, u.y, u.facing)
					: u.unitData.createUnit(simulation, u.playerIndex, u.x, u.y, u.facing);
			if (restored != null) {
				restored.setLife(simulation, u.life);
				restored.setMana(u.mana);
			}
		}
	}

	// -----------------------------------------------------------------------
	// Persistence
	// -----------------------------------------------------------------------

	/**
	 * Writes this save to {@code file}, creating parent directories as needed.
	 *
	 * @throws IOException on I/O failure
	 */
	public void save(final File file) throws IOException {
		final Path target = file.toPath().toAbsolutePath();
		Files.createDirectories(target.getParent());
		final Path temporary = Files.createTempFile(target.getParent(), ".campaign-save-", ".tmp");
		try {
			writeTo(temporary.toFile());
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (final AtomicMoveNotSupportedException e) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void writeTo(final File file) throws IOException {
		try (FileOutputStream stream = new FileOutputStream(file);
				DataOutputStream out = new DataOutputStream(stream)) {
			out.writeInt(FILE_MAGIC);
			out.writeInt(FILE_VERSION);
			out.writeUTF(this.mapPath != null ? this.mapPath : "");

			// Globals
			out.writeInt(this.globals.size());
			for (final Map.Entry<String, JassValue> entry : this.globals.entrySet()) {
				out.writeUTF(entry.getKey());
				writeValue(out, entry.getValue());
			}

			// Player resources
			out.writeInt(this.gold.length);
			for (int i = 0; i < this.gold.length; i++) {
				out.writeInt(this.gold[i]);
				out.writeInt(this.lumber[i]);
			}

			// v2: clock + camera (NaN when not recorded)
			out.writeFloat(this.timeOfDay);
			out.writeFloat(this.timeOfDayScale);
			out.writeFloat(this.cameraX);
			out.writeFloat(this.cameraY);

			// v3: map to reload ("" when not recorded)
			out.writeUTF(this.savedMapPath != null ? this.savedMapPath : "");

			// v4: full simulation entities
			// Units
			out.writeInt(this.savedUnits.size());
			for (final SavedUnitState u : this.savedUnits) {
				out.writeInt(u.unitTypeId.getValue());
				out.writeInt(u.playerIndex);
				out.writeFloat(u.x);
				out.writeFloat(u.y);
				out.writeFloat(u.facing);
				out.writeFloat(u.life);
				out.writeFloat(u.mana);
				writeStoredUnitData(out, u.unitData);
			}

			// Destructables
			out.writeInt(this.savedDestructables.size());
			for (final SavedDestructableState d : this.savedDestructables) {
				out.writeInt(d.handleId);
				out.writeFloat(d.x);
				out.writeFloat(d.y);
				out.writeFloat(d.life);
				out.writeBoolean(d.isDead);
			}

			// Ground items
			out.writeInt(this.savedGroundItems.size());
			for (final SavedItemState item : this.savedGroundItems) {
				out.writeInt(item.typeId.getValue());
				out.writeFloat(item.x);
				out.writeFloat(item.y);
				out.writeFloat(item.life);
				out.writeInt(item.charges);
			}
			out.writeInt(this.arrays.size());
			for (final Map.Entry<String, SavedArray> entry : this.arrays.entrySet()) {
				out.writeUTF(entry.getKey());
				out.writeByte(entry.getValue().type);
				out.writeInt(entry.getValue().entries.size());
				for (final Map.Entry<Integer, JassValue> item : entry.getValue().entries.entrySet()) {
					out.writeInt(item.getKey());
					writeValue(out, item.getValue());
				}
			}
			out.flush();
			stream.getFD().sync();
		}
	}

	/**
	 * Loads a save from {@code file}.
	 *
	 * @return the loaded save, or {@code null} if the file is missing, corrupt, or
	 *         uses an unrecognised format.
	 */
	public static CGameSave tryLoad(final File file) {
		if (!file.exists()) {
			return null;
		}
		try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
			final int magic = in.readInt();
			if (magic != FILE_MAGIC) {
				System.err.println("CGameSave: bad magic in " + file + " (expected " + Integer.toHexString(FILE_MAGIC)
						+ ", got " + Integer.toHexString(magic) + ")");
				return null;
			}
			final int version = in.readInt();
			if ((version < FILE_VERSION_MIN) || (version > FILE_VERSION)) {
				System.err.println("CGameSave: unsupported version " + version + " in " + file);
				return null;
			}
			final String mapPath = in.readUTF();

			final int nGlobals = readCount(in, 100000, "globals");
			final Map<String, JassValue> globals = new HashMap<>(nGlobals * 2);
			for (int i = 0; i < nGlobals; i++) {
				final String name = in.readUTF();
				final JassValue value = readValue(in);
				if (value != null) {
					globals.put(name, value);
				}
			}

			final int nPlayers = readCount(in, 64, "players");
			final int[] gold = new int[nPlayers];
			final int[] lumber = new int[nPlayers];
			for (int i = 0; i < nPlayers; i++) {
				gold[i] = in.readInt();
				lumber[i] = in.readInt();
			}

			float timeOfDay = Float.NaN;
			float timeOfDayScale = Float.NaN;
			float cameraX = Float.NaN;
			float cameraY = Float.NaN;
			if (version >= 2) {
				timeOfDay = in.readFloat();
				timeOfDayScale = in.readFloat();
				cameraX = in.readFloat();
				cameraY = in.readFloat();
			}
			String savedMapPath = "";
			if (version >= 3) {
				savedMapPath = in.readUTF();
			}

			final List<SavedUnitState> savedUnits = new ArrayList<>();
			final List<SavedDestructableState> savedDestructables = new ArrayList<>();
			final List<SavedItemState> savedGroundItems = new ArrayList<>();

			if (version >= 4) {
				final int nUnits = readCount(in, 1000000, "units");
				for (int i = 0; i < nUnits; i++) {
					final War3ID unitTypeId = new War3ID(in.readInt());
					final int playerIndex = in.readInt();
					final float x = in.readFloat();
					final float y = in.readFloat();
					final float facing = in.readFloat();
					final float life = in.readFloat();
					final float mana = in.readFloat();
					final StoredUnitData unitData = readStoredUnitData(in, unitTypeId);
					savedUnits.add(new SavedUnitState(unitTypeId, playerIndex, x, y, facing, life, mana, unitData));
				}

				final int nDestructables = readCount(in, 1000000, "destructables");
				for (int i = 0; i < nDestructables; i++) {
					final int handleId = in.readInt();
					final float x = in.readFloat();
					final float y = in.readFloat();
					final float life = in.readFloat();
					final boolean isDead = in.readBoolean();
					savedDestructables.add(new SavedDestructableState(handleId, x, y, life, isDead));
				}

				final int nItems = readCount(in, 1000000, "items");
				for (int i = 0; i < nItems; i++) {
					final War3ID typeId = new War3ID(in.readInt());
					final float x = in.readFloat();
					final float y = in.readFloat();
					final float life = in.readFloat();
					final int charges = in.readInt();
					savedGroundItems.add(new SavedItemState(typeId, x, y, life, charges));
				}
			}

			final CGameSave save = new CGameSave(mapPath, gold, lumber, globals, timeOfDay, timeOfDayScale, cameraX, cameraY,
					savedMapPath, savedUnits, savedDestructables, savedGroundItems);
			if (version >= 5) {
				final int count = readCount(in, 100000, "arrays");
				for (int i = 0; i < count; i++) {
					final String name = in.readUTF();
					final byte type = in.readByte();
					if ((type < TYPE_INT) || (type > TYPE_STRING)) {
						throw new IOException("Invalid array type: " + type);
					}
					final SavedArray array = new SavedArray(type);
					final int entries = readCount(in, JassSettings.MAX_ARRAY_SIZE, "array entries");
					for (int j = 0; j < entries; j++) {
						final int index = in.readInt();
						final JassValue value = readValue(in);
						if ((index < 0) || (index >= JassSettings.MAX_ARRAY_SIZE)
								|| (valueType(value) != type) || (array.entries.put(index, value) != null)) {
							throw new IOException("Invalid or duplicate array entry in " + name);
						}
					}
					if (save.arrays.put(name, array) != null) {
						throw new IOException("Duplicate array: " + name);
					}
				}
			}
			if (in.read() != -1) {
				throw new IOException("Unexpected trailing save data");
			}
			return save;
		}
		catch (final IOException e) {
			System.err.println("CGameSave: failed to load " + file + ": " + e.getMessage());
			return null;
		}
	}

	// -----------------------------------------------------------------------
	// Save listing
	// -----------------------------------------------------------------------

	/**
	 * Lists save files in {@code saveDir} for the main-menu Load Saved screen.
	 * Returns display names ({@code *.w3s} file names) sorted alphabetically;
	 * a missing directory yields an empty list rather than an error.
	 */
	public static java.util.List<String> listSaves(final File saveDir) {
		final java.util.List<String> names = new java.util.ArrayList<>();
		if (saveDir == null) {
			return names;
		}
		final File[] files = saveDir.listFiles();
		if (files == null) {
			return names;
		}
		for (final File file : files) {
			if (file.isFile() && file.getName().toLowerCase().endsWith(".w3s")) {
				names.add(file.getName());
			}
		}
		java.util.Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
		return names;
	}

	// -----------------------------------------------------------------------
	// Private helpers
	// -----------------------------------------------------------------------

	private static boolean isPrimitive(final JassValue value) {
		return (value instanceof IntegerJassValue)
				|| (value instanceof RealJassValue)
				|| (value instanceof BooleanJassValue)
				|| (value instanceof StringJassValue);
	}

	private static byte primitiveType(final JassType type) {
		if (type == JassType.INTEGER) return TYPE_INT;
		if (type == JassType.REAL) return TYPE_REAL;
		if (type == JassType.BOOLEAN) return TYPE_BOOL;
		if (type == JassType.STRING) return TYPE_STRING;
		return -1;
	}

	private static byte valueType(final JassValue value) {
		if (value instanceof IntegerJassValue) return TYPE_INT;
		if (value instanceof RealJassValue) return TYPE_REAL;
		if (value instanceof BooleanJassValue) return TYPE_BOOL;
		if (value instanceof StringJassValue) return TYPE_STRING;
		return -1;
	}

	private static JassValue defaultValue(final byte type) {
		switch (type) {
		case TYPE_INT: return IntegerJassValue.ZERO;
		case TYPE_REAL: return RealJassValue.ZERO;
		case TYPE_BOOL: return BooleanJassValue.FALSE;
		case TYPE_STRING: return StringJassValue.of(null);
		default: throw new IllegalArgumentException("Unsupported array type: " + type);
		}
	}

	private static boolean isDefault(final JassValue value, final byte type) {
		if (value == null) return true;
		switch (type) {
		case TYPE_INT: return ((IntegerJassValue) value).getValue() == 0;
		case TYPE_REAL: return Double.doubleToLongBits(((RealJassValue) value).getValue()) == 0L;
		case TYPE_BOOL: return !((BooleanJassValue) value).getValue();
		case TYPE_STRING: return ((StringJassValue) value).getValue() == null;
		default: return false;
		}
	}

	private static int readCount(final DataInputStream in, final int maximum, final String label) throws IOException {
		final int count = in.readInt();
		if ((count < 0) || (count > maximum)) {
			throw new IOException("Invalid " + label + " count: " + count);
		}
		return count;
	}

	private static void writeValue(final DataOutputStream out, final JassValue value) throws IOException {
		if (value instanceof IntegerJassValue) {
			out.writeByte(TYPE_INT);
			out.writeInt(((IntegerJassValue) value).getValue());
		}
		else if (value instanceof RealJassValue) {
			out.writeByte(TYPE_REAL);
			out.writeDouble(((RealJassValue) value).getValue());
		}
		else if (value instanceof BooleanJassValue) {
			out.writeByte(TYPE_BOOL);
			final Boolean b = value.visit(BooleanJassValueVisitor.getInstance());
			out.writeBoolean((b != null) && b);
		}
		else if (value instanceof StringJassValue) {
			final String s = value.visit(StringJassValueVisitor.getInstance());
			out.writeByte(s == null ? TYPE_NULL_STRING : TYPE_STRING);
			if (s != null) {
				out.writeUTF(s);
			}
		}
		else {
			throw new IOException("Unsupported saved value type");
		}
	}

	private static JassValue readValue(final DataInputStream in) throws IOException {
		final byte type = in.readByte();
		switch (type) {
		case TYPE_INT:
			return IntegerJassValue.of(in.readInt());
		case TYPE_REAL:
			return RealJassValue.of(in.readDouble());
		case TYPE_BOOL:
			return BooleanJassValue.of(in.readBoolean());
		case TYPE_STRING:
			return StringJassValue.of(in.readUTF());
		case TYPE_NULL_STRING:
			return StringJassValue.of(null);
		default:
			throw new IOException("Unknown saved value type: " + type);
		}
	}

	private static void writeStoredUnitData(final DataOutputStream out, final StoredUnitData data) throws IOException {
		if (data == null) {
			out.writeBoolean(false);
			return;
		}
		out.writeBoolean(true);
		out.writeInt(data.xp);
		out.writeInt(data.skillPoints);
		out.writeInt(data.strengthBase);
		out.writeInt(data.agilityBase);
		out.writeInt(data.intelligenceBase);
		out.writeInt(data.strengthBonus);
		out.writeInt(data.agilityBonus);
		out.writeInt(data.intelligenceBonus);
		out.writeUTF(data.properName != null ? data.properName : "");

		// Items
		if (data.items != null) {
			int count = 0;
			for (final StoredItemData it : data.items) {
				if (it != null) {
					count++;
				}
			}
			out.writeInt(count);
			for (int slot = 0; slot < data.items.length; slot++) {
				final StoredItemData it = data.items[slot];
				if (it != null) {
					out.writeInt(slot);
					out.writeInt(it.typeId.getValue());
					out.writeInt(it.charges);
				}
			}
		}
		else {
			out.writeInt(0);
		}

		// Abilities
		if (data.abilities != null) {
			out.writeInt(data.abilities.length);
			for (final StoredAbilityData ab : data.abilities) {
				out.writeInt(ab.abilityId.getValue());
				out.writeInt(ab.level);
			}
		}
		else {
			out.writeInt(0);
		}
	}

	private static StoredUnitData readStoredUnitData(final DataInputStream in, final War3ID unitTypeId) throws IOException {
		final boolean hasData = in.readBoolean();
		if (!hasData) {
			return null;
		}
		final int xp = in.readInt();
		final int skillPoints = in.readInt();
		final int strBase = in.readInt();
		final int agiBase = in.readInt();
		final int intBase = in.readInt();
		final int strBonus = in.readInt();
		final int agiBonus = in.readInt();
		final int intBonus = in.readInt();
		final String properName = in.readUTF();

		final int itemCount = readCount(in, 6, "inventory items");
		StoredItemData[] items = null;
		if (itemCount > 0) {
			items = new StoredItemData[6];
			for (int i = 0; i < itemCount; i++) {
				final int slot = in.readInt();
				final War3ID itemTypeId = new War3ID(in.readInt());
				final int charges = in.readInt();
				if ((slot >= 0) && (slot < items.length)) {
					items[slot] = new StoredItemData(itemTypeId, charges);
				}
			}
		}

		final int abilityCount = readCount(in, JassSettings.MAX_ARRAY_SIZE, "abilities");
		StoredAbilityData[] abilities = null;
		if (abilityCount > 0) {
			abilities = new StoredAbilityData[abilityCount];
			for (int i = 0; i < abilityCount; i++) {
				final War3ID abilityId = new War3ID(in.readInt());
				final int level = in.readInt();
				abilities[i] = new StoredAbilityData(abilityId, level);
			}
		}

		return new StoredUnitData(unitTypeId, xp, skillPoints, strBase, agiBase, intBase, strBonus, agiBonus, intBonus,
				properName, items, abilities);
	}
}

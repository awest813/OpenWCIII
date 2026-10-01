package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tick-stamped external inputs reconstruct the complete simulation, including script continuations. */
public final class MissionReplayLog {
	public static final int TARGET = 0, POINT = 1, DROP_POINT = 2, DROP_TARGET = 3, IMMEDIATE = 4,
			CANCEL_TRAINING = 5, GUI_EVENT = 6, DIALOG = 7, SELECT = 8, DESELECT = 9, CHAT = 10,
			PAUSE = 11, SCRIPT = 12, TRACKABLE_HIT = 13, TRACKABLE_HOVER = 14, HOST_READ = 15, CACHE_READ = 16, GAME_EVENT = 17;
	public int targetTick;
	public int localPlayer;
	public int difficulty;
	public int defaultDifficulty;
	public int triggerCounter;
	public int cacheCounter;
	public String scriptFingerprint = "";
	public String stateFingerprint = "";
	public String profileName = "";
	public int[] selectedUnits = new int[0];
	public byte[] presentation = new byte[0];
	public final Map<String, byte[]> initialCaches = new LinkedHashMap<>();
	public final Map<String, String> initialProgress = new LinkedHashMap<>();
	public final List<Entry> entries = new ArrayList<>();

	public static final class Entry {
		public final int tick, kind, a, b, c, d, e;
		public final float x, y;
		public final boolean queue;
		public final String text;
		public Entry(final int tick, final int kind, final int a, final int b, final int c, final int d,
				final int e, final float x, final float y, final boolean queue, final String text) {
			this.tick = tick; this.kind = kind; this.a = a; this.b = b; this.c = c; this.d = d;
			this.e = e; this.x = x; this.y = y; this.queue = queue; this.text = text == null ? "" : text;
		}
	}

	public MissionReplayLog copyAt(final int tick, final String fingerprint) {
		final MissionReplayLog copy = new MissionReplayLog();
		copy.targetTick = tick; copy.localPlayer = this.localPlayer; copy.difficulty = this.difficulty;
		copy.defaultDifficulty = this.defaultDifficulty; copy.triggerCounter = this.triggerCounter;
		copy.cacheCounter = this.cacheCounter; copy.scriptFingerprint = this.scriptFingerprint;
		copy.stateFingerprint = fingerprint;
		copy.profileName = this.profileName;
		copy.selectedUnits = this.selectedUnits.clone();
		copy.presentation = this.presentation.clone();
		this.initialCaches.forEach((name, bytes) -> copy.initialCaches.put(name, bytes.clone()));
		copy.initialProgress.putAll(this.initialProgress);
		copy.entries.addAll(this.entries);
		return copy;
	}

	public void write(final DataOutputStream out) throws IOException {
		validate();
		out.writeInt(this.targetTick); out.writeInt(this.localPlayer); out.writeInt(this.difficulty);
		out.writeInt(this.defaultDifficulty); out.writeInt(this.triggerCounter); out.writeInt(this.cacheCounter);
		out.writeUTF(this.scriptFingerprint); out.writeUTF(this.stateFingerprint);
		out.writeUTF(this.profileName);
		out.writeInt(this.selectedUnits.length);
		for (final int handle : this.selectedUnits) out.writeInt(handle);
		out.writeInt(this.initialCaches.size());
		for (final Map.Entry<String, byte[]> entry : this.initialCaches.entrySet()) {
			out.writeUTF(entry.getKey()); out.writeInt(entry.getValue().length); out.write(entry.getValue());
		}
		out.writeInt(this.initialProgress.size());
		for (final Map.Entry<String, String> entry : this.initialProgress.entrySet()) {
			out.writeUTF(entry.getKey()); out.writeUTF(entry.getValue());
		}
		out.writeInt(this.entries.size());
		for (final Entry entry : this.entries) {
			out.writeInt(entry.tick); out.writeByte(entry.kind);
			out.writeInt(entry.a); out.writeInt(entry.b); out.writeInt(entry.c); out.writeInt(entry.d); out.writeInt(entry.e);
			out.writeFloat(entry.x); out.writeFloat(entry.y); out.writeBoolean(entry.queue); out.writeUTF(entry.text);
		}
		out.writeInt(this.presentation.length); out.write(this.presentation);
	}

	private static int count(final DataInputStream in, final int max) throws IOException {
		final int value = in.readInt();
		if (value < 0 || value > max) throw new IOException("Invalid checkpoint count: " + value);
		return value;
	}

	public static MissionReplayLog read(final DataInputStream in) throws IOException { return read(in, true); }

	public static MissionReplayLog read(final DataInputStream in, final boolean hasPresentation) throws IOException {
		final MissionReplayLog log = new MissionReplayLog();
		log.targetTick = count(in, 10_000_000); log.localPlayer = count(in, 27);
		log.difficulty = count(in, 3); log.defaultDifficulty = count(in, 3);
		log.triggerCounter = in.readInt(); log.cacheCounter = in.readInt();
		log.scriptFingerprint = in.readUTF(); log.stateFingerprint = in.readUTF();
		log.profileName = in.readUTF();
		log.selectedUnits = new int[count(in, 10000)];
		for (int i = 0; i < log.selectedUnits.length; i++) log.selectedUnits[i] = in.readInt();
		final int cacheCount = count(in, 10000);
		int totalCacheBytes = 0;
		for (int i = 0; i < cacheCount; i++) {
			final String name = in.readUTF();
			final int size = count(in, 64 * 1024 * 1024);
			totalCacheBytes += size;
			if (totalCacheBytes > 64 * 1024 * 1024) throw new IOException("Checkpoint cache baseline too large");
			final byte[] bytes = new byte[size]; in.readFully(bytes);
			if (log.initialCaches.put(name, bytes) != null) throw new IOException("Duplicate checkpoint cache");
		}
		final int progressCount = count(in, 100000);
		for (int i = 0; i < progressCount; i++) {
			if (log.initialProgress.put(in.readUTF(), in.readUTF()) != null) throw new IOException("Duplicate progress entry");
		}
		final int size = count(in, 2_000_000);
		int previous = 0;
		for (int i = 0; i < size; i++) {
			final int tick = in.readInt(); final int kind = in.readUnsignedByte();
			if (tick < previous || tick > log.targetTick || kind > GAME_EVENT) throw new IOException("Invalid checkpoint input");
			previous = tick;
			log.entries.add(new Entry(tick, kind, in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
					in.readFloat(), in.readFloat(), in.readBoolean(), in.readUTF()));
		}
		if (hasPresentation) { log.presentation = new byte[count(in, 65536)]; in.readFully(log.presentation); }
		log.validate();
		return log;
	}

	public void validate() throws IOException {
		if (this.targetTick < 0 || this.targetTick > 10_000_000 || this.localPlayer < 0
				|| this.localPlayer >= com.etheller.warsmash.util.WarsmashConstants.MAX_PLAYERS
				|| this.difficulty < 0 || this.difficulty > 3 || this.defaultDifficulty < 0 || this.defaultDifficulty > 3
				|| this.triggerCounter < 0 || this.cacheCounter < 0 || this.profileName.isEmpty()
				|| !this.scriptFingerprint.matches("[0-9a-f]{64}") || !this.stateFingerprint.matches("[0-9a-f]{64}")) throw new IOException("Invalid mission checkpoint metadata");
		if (this.initialCaches.size() > 10000 || this.initialProgress.size() > 100000 || this.entries.size() > 2_000_000 || this.selectedUnits.length > 10000 || this.presentation.length > 65536) throw new IOException("Mission checkpoint is too large");
		long size = 0;
		for (final byte[] bytes : this.initialCaches.values()) size += bytes.length;
		if (size > 64 * 1024 * 1024) throw new IOException("Checkpoint cache baseline too large");
		int previous = 0;
		for (final Entry entry : this.entries) {
			if (entry.tick < previous || entry.tick > this.targetTick || entry.kind < 0 || entry.kind > GAME_EVENT) throw new IOException("Invalid checkpoint input");
			if (!Float.isFinite(entry.x) || !Float.isFinite(entry.y)) throw new IOException("Invalid checkpoint coordinates");
			if (entry.kind == GAME_EVENT && (entry.a < 0 || entry.a >= com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3.values().length)) throw new IOException("Invalid checkpoint game event");
			if (entry.kind == CACHE_READ && !this.initialCaches.containsKey("read-" + entry.a)) throw new IOException("Missing checkpoint cache data");
			previous = entry.tick;
		}
		try {
			for (final Map.Entry<String, String> entry : this.initialProgress.entrySet()) {
				final String key = entry.getKey(), value = entry.getValue();
				if (key.equals("race")) Integer.parseInt(value);
				else {
					if (!value.equals("true") && !value.equals("false")) throw new IOException("Invalid progress value");
					if (key.startsWith("mission.")) Long.parseLong(key.substring(8));
					else if (key.startsWith("campaign.")) Integer.parseInt(key.substring(9));
					else if (key.startsWith("opening.")) Long.parseLong(key.substring(8));
					else if (key.startsWith("ending.")) Long.parseLong(key.substring(7));
					else if (key.startsWith("button.")) Integer.parseInt(key.substring(7));
					else if (!key.equals("tutorial") && !key.equals("forceSelect")) throw new IOException("Invalid progress key");
				}
			}
		}
		catch (final NumberFormatException error) { throw new IOException("Invalid progress entry", error); }
	}
}

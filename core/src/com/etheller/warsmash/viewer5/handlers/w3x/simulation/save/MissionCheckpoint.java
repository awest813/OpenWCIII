package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameCache;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MeleeUI;

/** Reconstructs a mission from startup, retaining engine-owned handles and VM continuations. */
public final class MissionCheckpoint {
	private final CSimulation simulation;
	private final MissionReplayLog log;
	private final boolean loading;
	private boolean restoring;
	private int cursor;
	private Consumer<MissionReplayLog.Entry> uiInput;
	private Consumer<File> loadHandler;
	private final List<Runnable> pendingSaves = new ArrayList<>();
	private String failure;
	private java.util.Map<String, String> currentProgress;
	private War3MapViewer viewer;
	private MeleeUI ui;
	private boolean singlePlayer = true;
	private boolean presentationRestore;
	private boolean discardNextFrameTime;
	public boolean shouldDiscardFrameTime() { return this.discardNextFrameTime; }
	public void consumeDiscardFrameTime() { this.discardNextFrameTime = false; }
	private Runnable beforeSave = () -> { };
	private File lastSaveFile;
	public boolean isPresentationRestore() { return this.presentationRestore; }
	public void setSinglePlayer(final boolean value) { this.singlePlayer = value; }
	public void setBeforeSave(final Runnable callback) { this.beforeSave = callback; }
	public File getLastSaveFile() { return this.lastSaveFile; }
	public void setLastSaveFile(final File file) { this.lastSaveFile = file; }
	public void bind(final War3MapViewer viewer, final MeleeUI ui) {
		this.viewer = viewer; this.ui = ui; setUIInput(ui::replayMissionInput);
	}
	public void save(final File file) throws IOException { save(file, this.viewer, this.ui); }

	public MissionCheckpoint(final CSimulation simulation, final MissionReplayLog saved) {
		this.simulation = simulation;
		this.loading = saved != null;
		this.restoring = this.loading;
		this.log = saved == null ? new MissionReplayLog() : saved;
		if (!System.getProperty("warsmash.missionResumeAudit", "").isEmpty()) System.out.println("[MissionResumeProbe] checkpoint startup tick=" + simulation.getGameTurnTick());
		if (this.loading) {
			this.currentProgress = com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().snapshot();
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().restoreSnapshot(saved.initialProgress, true);
			Trigger.restoreNextHandleId(saved.triggerCounter);
			CGameCache.restoreHandleCounter(saved.cacheCounter);
		}
		else {
			this.log.profileName = com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().getProfileName();
			this.log.initialProgress.putAll(com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().snapshot());
			this.log.triggerCounter = Trigger.getNextHandleId();
			this.log.cacheCounter = CGameCache.getHandleCounter();
		}
		simulation.setMissionCheckpoint(this);
	}

	public MissionReplayLog getLog() { return this.log; }
	public boolean isRestoring() { return this.restoring; }
	public boolean isLoading() { return this.loading; }
	public String getFailure() { return this.failure; }
	public void setUIInput(final Consumer<MissionReplayLog.Entry> handler) { this.uiInput = handler; }
	public void setLoadHandler(final Consumer<File> handler) { this.loadHandler = handler; }


	public byte[] readCache(final File file) throws IOException {
		final String name = file.getName().toLowerCase(java.util.Locale.ROOT);
		if (this.restoring) {
			if (this.cursor >= this.log.entries.size()) throw new IOException("Missing saved gamecache read");
			final MissionReplayLog.Entry entry = this.log.entries.get(this.cursor++);
			if (entry.kind != MissionReplayLog.CACHE_READ || entry.tick != this.simulation.getGameTurnTick() || !name.equals(entry.text)) throw new IOException("Saved gamecache read differs");
			final byte[] bytes = this.log.initialCaches.get("read-" + entry.a);
			if (bytes == null) throw new IOException("Missing saved gamecache data");
			return bytes;
		}
		final byte[] bytes = file.isFile() ? Files.readAllBytes(file.toPath()) : new byte[0];
		final int id = this.log.entries.size();
		this.log.initialCaches.put("read-" + id, bytes);
		record(MissionReplayLog.CACHE_READ, id, 0, 0, 0, 0, 0, 0, false, name);
		return bytes;
	}

	public void verifyScripts(final DataSource source, final String[] paths) throws IOException {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final java.util.SortedSet<String> scripts = new java.util.TreeSet<>();
            for (final String requested : paths) {
                final String basename = requested.substring(Math.max(requested.lastIndexOf('/'), requested.lastIndexOf('\\')) + 1);
                if (source.has(requested)) scripts.add(requested);
                else if (source.has(basename)) scripts.add(basename);
                else if (source.getListfile() != null) {
                    final String prefix = requested.toLowerCase(java.util.Locale.ROOT).replace('\\', '/');
                    for (final String path : source.getListfile()) if (path.toLowerCase(java.util.Locale.ROOT).replace('\\', '/').startsWith(prefix)) scripts.add(path);
                }
            }
            for (final String path : scripts) {
                digest.update(path.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                try (InputStream stream = source.getResourceAsStream(path)) { if (stream != null) stream.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest)); }
            }
            if (this.viewer != null) {
                final File map = new File(this.viewer.getCurrentMapPath());
                try (InputStream stream = map.isFile() ? Files.newInputStream(map.toPath()) : this.viewer.dataSource.getResourceAsStream(this.viewer.getCurrentMapPath())) {
                    if (stream != null) stream.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest));
                }
            }
            final java.util.Set<java.nio.file.Path> sources = new java.util.HashSet<>();
            for (final Class<?> type : new Class<?>[] { CSimulation.class, com.etheller.interpreter.ast.scope.GlobalScope.class }) {
                final java.nio.file.Path binary = java.nio.file.Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (!sources.add(binary)) continue;
                if (Files.isDirectory(binary)) {
                    try (java.util.stream.Stream<java.nio.file.Path> files = Files.walk(binary)) {
                        for (final java.nio.file.Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                            digest.update(binary.relativize(file).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            digest.update(Files.readAllBytes(file));
                        }
                    }
                }
                else try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(binary.toFile())) {
                    for (final java.util.zip.ZipEntry entry : archive.stream().filter(item -> item.getName().endsWith(".class")).sorted(java.util.Comparator.comparing(java.util.zip.ZipEntry::getName)).toList()) {
                        digest.update(entry.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        try (InputStream stream = archive.getInputStream(entry)) { stream.transferTo(new java.security.DigestOutputStream(java.io.OutputStream.nullOutputStream(), digest)); }
                    }
                }
            }
			final String fingerprint = hex(digest.digest());
			if (this.loading && !fingerprint.equals(this.log.scriptFingerprint)) {
				throw new IOException("The saved mission uses different scripts or engine binaries");
			}
			this.log.scriptFingerprint = fingerprint;
		}
		catch (final java.net.URISyntaxException error) { throw new IOException(error); }
		catch (final java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
	}

	/** AI scripts can be loaded after mission startup, outside the initial map digest. */
	public void verifyAsset(final DataSource source, final String path) throws IOException {
		if (!source.has(path)) return;
		try (InputStream stream = source.getResourceAsStream(path)) {
			final String actual = hex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
			final String expected = ((com.etheller.interpreter.ast.value.StringJassValue) hostRead("Asset:" + path,
					() -> com.etheller.interpreter.ast.value.StringJassValue.of(actual))).getValue();
			if (!actual.equals(expected)) throw new IOException("The saved mission uses a different AI script: " + path);
		}
		catch (final java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
	}

	static String hex(final byte[] bytes) {
		final StringBuilder text = new StringBuilder();
		for (final byte value : bytes) text.append(String.format("%02x", value & 255));
		return text.toString();
	}

	public void record(final int kind, final int a, final int b, final int c, final int d, final int e,
			final float x, final float y, final boolean queue, final String text) {
		if (!this.restoring && this.failure == null) this.log.entries.add(new MissionReplayLog.Entry(
				this.simulation.getGameTurnTick(), kind, a, b, c, d, e, x, y, queue, text));
	}
	public void fireSaveEvent() { fireEvent(JassGameEventsWar3.EVENT_GAME_SAVE); }
	private void fireEvent(final JassGameEventsWar3 event) {
		this.simulation.recordMissionInput(MissionReplayLog.GAME_EVENT, event.ordinal(), 0, 0, 0, 0, 0, 0, false, "");
		this.simulation.fireGameEvent(event);
	}
	public void runScript(final String function) {
		record(MissionReplayLog.SCRIPT, 0, 0, 0, 0, 0, 0, 0, false, function);
		this.simulation.getGlobalScope().queueThread(this.simulation.getGlobalScope().createThread(function,
				java.util.Collections.emptyList(), com.etheller.interpreter.ast.scope.TriggerExecutionScope.EMPTY));
	}

	/** Replay UI/audio/file observations in call order; their host state need not follow accelerated ticks. */
	public com.etheller.interpreter.ast.value.JassValue hostRead(final String name,
			final java.util.function.Supplier<com.etheller.interpreter.ast.value.JassValue> read) {
		final com.etheller.interpreter.ast.value.JassValue actual = read.get();
		if (this.restoring) {
			if (this.cursor >= this.log.entries.size()) throw new IllegalStateException("Missing saved host observation: " + name);
			final MissionReplayLog.Entry entry = this.log.entries.get(this.cursor++);
			if (entry.kind != MissionReplayLog.HOST_READ || entry.tick != this.simulation.getGameTurnTick()
					|| !entry.text.startsWith(name + "\n")) throw new IllegalStateException("Saved host observation differs: " + name);
			final String text = entry.text.substring(name.length() + 1);
			switch (entry.a) {
			case 0: return com.etheller.interpreter.ast.value.IntegerJassValue.of(entry.b);
			case 1: return com.etheller.interpreter.ast.value.RealJassValue.of(Double.parseDouble(text));
			case 2: return com.etheller.interpreter.ast.value.BooleanJassValue.of(entry.queue);
			case 3: return com.etheller.interpreter.ast.value.StringJassValue.of(entry.queue ? text : null);
			case 4:
				final com.etheller.warsmash.parsers.jass.triggers.LocationJass location = (com.etheller.warsmash.parsers.jass.triggers.LocationJass)
						((com.etheller.interpreter.ast.value.HandleJassValue) actual).getJavaValue();
				location.x = entry.x; location.y = entry.y; return actual;
			default: throw new IllegalStateException("Invalid saved observation type");
			}
		}
		int type = -1, number = 0; float x = 0, y = 0; boolean flag = false; String text = "";
		if (actual instanceof com.etheller.interpreter.ast.value.IntegerJassValue) { type = 0; number = ((com.etheller.interpreter.ast.value.IntegerJassValue) actual).getValue(); }
		else if (actual instanceof com.etheller.interpreter.ast.value.RealJassValue) { type = 1; text = Double.toString(((com.etheller.interpreter.ast.value.RealJassValue) actual).getValue()); }
		else if (actual instanceof com.etheller.interpreter.ast.value.BooleanJassValue) { type = 2; flag = ((com.etheller.interpreter.ast.value.BooleanJassValue) actual).getValue(); }
		else if (actual instanceof com.etheller.interpreter.ast.value.StringJassValue) { type = 3; text = ((com.etheller.interpreter.ast.value.StringJassValue) actual).getValue(); flag = text != null; }
		else if (actual instanceof com.etheller.interpreter.ast.value.HandleJassValue) {
			final Object object = ((com.etheller.interpreter.ast.value.HandleJassValue) actual).getJavaValue();
			if (object instanceof com.etheller.warsmash.parsers.jass.triggers.LocationJass) {
				type = 4; x = ((com.etheller.warsmash.parsers.jass.triggers.LocationJass) object).x; y = ((com.etheller.warsmash.parsers.jass.triggers.LocationJass) object).y;
			}
		}
		if (type < 0) throw new IllegalStateException("Unsupported host observation: " + name);
		record(MissionReplayLog.HOST_READ, type, number, 0, 0, 0, x, y, flag, name + "\n" + (text == null ? "" : text));
		return actual;
	}
	public void releaseProgress() {
		if (this.currentProgress != null) {
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().restoreSnapshot(this.currentProgress, false);
			this.currentProgress = null;
		}
	}

	private void dispatch(final MissionReplayLog.Entry entry) {
		final CPlayerUnitOrderExecutor orders = new CPlayerUnitOrderExecutor(this.simulation, entry.a);
		switch (entry.kind) {
		case MissionReplayLog.TARGET: orders.issueTargetOrder(entry.b, entry.c, entry.d, entry.e, entry.queue); break;
		case MissionReplayLog.POINT: orders.issuePointOrder(entry.b, entry.c, entry.d, entry.x, entry.y, entry.queue); break;
		case MissionReplayLog.DROP_POINT: orders.issueDropItemAtPointOrder(entry.b, entry.c, entry.d, entry.e, entry.x, entry.y, entry.queue); break;
		case MissionReplayLog.DROP_TARGET: orders.issueDropItemAtTargetOrder(entry.b, entry.c, entry.d, entry.e, Integer.parseInt(entry.text), entry.queue); break;
		case MissionReplayLog.IMMEDIATE: orders.issueImmediateOrder(entry.b, entry.c, entry.d, entry.queue); break;
		case MissionReplayLog.CANCEL_TRAINING: orders.unitCancelTrainingItem(entry.b, entry.c); break;
		case MissionReplayLog.GUI_EVENT: orders.issueGuiPlayerEvent(entry.b); break;
		case MissionReplayLog.SELECT: this.simulation.getPlayer(entry.a).fireUnitSelectedEvents(this.simulation.getUnit(entry.b)); break;
		case MissionReplayLog.DESELECT: this.simulation.getPlayer(entry.a).fireUnitDeselectedEvents(this.simulation.getUnit(entry.b)); break;
		case MissionReplayLog.CHAT: this.simulation.getPlayer(entry.a).fireChatEvent(this.simulation.getGlobalScope(), entry.text); break;
		case MissionReplayLog.PAUSE: this.simulation.setGamePaused(entry.queue); break;
		case MissionReplayLog.SCRIPT: runScript(entry.text); break;
		case MissionReplayLog.GAME_EVENT: fireEvent(JassGameEventsWar3.values()[entry.a]); break;
		default: this.uiInput.accept(entry); break;
		}
	}

	/** Bounded work per render; no wall-clock time or live input advances a seeking mission. */
	public void replayFrame() {
		if (!this.restoring || this.failure != null) return;
		try {
			for (int step = 0; step < 128; step++) {
				while (this.cursor < this.log.entries.size()
						&& this.log.entries.get(this.cursor).tick == this.simulation.getGameTurnTick()
						&& this.log.entries.get(this.cursor).kind != MissionReplayLog.HOST_READ
						&& this.log.entries.get(this.cursor).kind != MissionReplayLog.CACHE_READ) {
					dispatch(this.log.entries.get(this.cursor++));
				}
				if (this.simulation.getGameTurnTick() == this.log.targetTick) {
					if (this.cursor != this.log.entries.size()) throw new IOException("Saved inputs were not fully consumed");
					final String actual = MissionStateFingerprint.of(this.simulation);
					if (!actual.equals(this.log.stateFingerprint)) throw new IOException("Mission reconstruction diverged at tick " + this.log.targetTick);
					this.restoring = false;
					this.discardNextFrameTime = true;
					releaseProgress();
					this.presentationRestore = true;
					try { if (this.ui != null) this.ui.restoreMissionSelection(this.log.selectedUnits);
						if (this.ui != null) this.ui.restoreMissionPresentation(this.log.presentation); }
					finally { this.presentationRestore = false; }
					fireEvent(JassGameEventsWar3.EVENT_GAME_LOADED);
					System.out.println("[MissionCheckpoint] resumed tick=" + this.log.targetTick + " state=" + actual);
					return;
				}
				this.simulation.update();
			}
		}
		catch (final Exception error) {
			releaseProgress();
			this.failure = error.getMessage();
			System.err.println("[MissionCheckpoint] " + this.failure);
		}
	}

	public void afterTick() {
		if (this.restoring) return;
		final List<Runnable> work = new ArrayList<>(this.pendingSaves);
		this.pendingSaves.clear();
		for (final Runnable save : work) save.run();
		if (this.viewer != null) com.etheller.warsmash.viewer5.handlers.w3x.ui.MissionResumeProbe.afterTick(this.viewer, this.ui);
	}

	public void save(final File file, final War3MapViewer viewer, final MeleeUI ui) throws IOException {
		if (this.restoring || this.failure != null) throw new IOException("Mission is still loading");
		if (!this.singlePlayer) throw new IOException("Mission checkpoints are available in single-player games");
		if (this.simulation.getPlayer(this.log.localPlayer).getPlayerState(com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState.GAME_RESULT)
				!= com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerGameResult.NEUTRAL.ordinal()) throw new IOException("The mission has ended");
		fireSaveEvent();
		if (this.simulation.isUpdating()) {
			this.pendingSaves.add(() -> {
				try { capture(file, viewer, ui); }
				catch (final Exception error) { ui.showGameMessage("Save failed: " + error.getMessage(), 5f); }
			});
			return;
		}
		capture(file, viewer, ui);
	}

	private void capture(final File file, final War3MapViewer viewer, final MeleeUI ui) throws IOException {
		this.beforeSave.run();
		final CGameSave save = new CGameSave(file.getName(), WarsmashConstants.MAX_PLAYERS);
		save.savedMapPath = viewer.getCurrentMapPath();
		save.collectGlobals(this.simulation.getGlobalScope());
		save.collectSimulation(this.simulation);
		save.timeOfDay = this.simulation.getGameTimeOfDay();
		save.timeOfDayScale = this.simulation.getTimeOfDayScale();
		save.cameraX = ui.getCameraManager().target.x; save.cameraY = ui.getCameraManager().target.y;
		for (int i = 0; i < save.gold.length; i++) {
			save.gold[i] = this.simulation.getPlayer(i).getGold(); save.lumber[i] = this.simulation.getPlayer(i).getLumber();
		}
		save.checkpoint = this.log.copyAt(this.simulation.getGameTurnTick(), MissionStateFingerprint.of(this.simulation));
		save.checkpoint.selectedUnits = ui.snapshotMissionSelection();
		save.checkpoint.presentation = ui.snapshotMissionPresentation();
		save.save(file);
		this.lastSaveFile = file;
		ui.showGameMessage("Game saved: " + file.getName(), 3f);
	}

	public void load(final File file) throws IOException {
		if (this.restoring) return; // Never perform historical disk loads while replaying.
		final CGameSave save = CGameSave.tryLoad(file);
		if (save == null) throw new IOException("No valid save found");
		if (save.checkpoint == null) throw new IOException("This older save has no complete mission checkpoint");
		if (!save.checkpoint.profileName.equals(com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().getProfileName())) throw new IOException("This mission save belongs to another player profile");
		if (!this.singlePlayer) throw new IOException("Mission checkpoints are available in single-player games");
		if (this.loadHandler == null) throw new IOException("Mission loading is unavailable in this session");
		this.loadHandler.accept(file);
	}
}

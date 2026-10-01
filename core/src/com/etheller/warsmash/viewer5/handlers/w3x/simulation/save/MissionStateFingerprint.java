package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

/** Read-only digest of gameplay objects and interpreter runtime, excluding render resources and compiled code. */
public final class MissionStateFingerprint {
	private final MessageDigest digest;
	private final IdentityHashMap<Object, Integer> seen = new IdentityHashMap<>();
	private final StringBuilder trace = System.getProperty("warsmash.missionResumeAudit", "").isEmpty() ? null : new StringBuilder();
	private static final Set<String> SIMULATION_FIELDS = Set.of("units", "newUnits", "removedUnits", "destructables",
			"removedDestructables", "items", "players", "projectiles", "newProjectiles", "handleIdAllocator",
			"gameTurnTick", "currentGameDayTimeElapsed", "handleIdToAbility", "activeTimers", "addedTimers",
			"removedTimers", "onTickTriggers", "addedOnTickTriggers", "removedOnTickTriggers", "globalScope",
			"aiGlobalScopes", "aiEnvironments", "pathfindingProcessors", "pathingGrid", "worldCollision", "eventTypeToEvents", "ownedTreeSet", "postUpdateCallbacks", "runningPostUpdateCallbacks", "timeOfDaySuspended", "timeOfDayScale", "gamePaused", "nextGameTime", "falseTimeOfDay",
			"daytime", "fogMaskEnabled", "fogEnabled", "unitInRangeEvents", "playerStateEvents", "timeOfDayVariableEvents", "seededRandom");
	private static final Set<String> GLOBAL_FIELDS = Set.of("indexedGlobals", "triggerQueue", "runningTriggerQueue", "threads", "newThreads");

	private MissionStateFingerprint() {
		try { this.digest = MessageDigest.getInstance("SHA-256"); }
		catch (final Exception error) { throw new AssertionError(error); }
	}
	public static String of(final CSimulation simulation) {
		final MissionStateFingerprint fingerprint = new MissionStateFingerprint();
		fingerprint.visit(simulation);
		if (fingerprint.trace != null) {
			try {
				final java.nio.file.Path directory = java.nio.file.Path.of(System.getProperty("user.home"), ".warsmash", "saves");
				java.nio.file.Files.createDirectories(directory);
				java.nio.file.Files.writeString(directory.resolve("state-" + System.getProperty("warsmash.missionResumeAudit") + "-" + simulation.getGameTurnTick() + ".txt"), fingerprint.trace);
			}
			catch (final java.io.IOException error) { throw new IllegalStateException(error); }
		}
		return MissionCheckpoint.hex(fingerprint.digest.digest());
	}
	/** Also useful for checking stack/timer fixtures without a renderer. */
	public static String ofRuntime(final Object runtime) {
		final MissionStateFingerprint fingerprint = new MissionStateFingerprint();
		fingerprint.visit(runtime);
		return MissionCheckpoint.hex(fingerprint.digest.digest());
	}
	private void token(final Object value) {
		this.digest.update((String.valueOf(value) + "\0").getBytes(StandardCharsets.UTF_8));
		if (this.trace != null) this.trace.append(String.valueOf(value).replace("\n", "\\n")).append('\n');
	}
	private String key(final Object value) {
		if (value instanceof com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing.CPathfindingProcessor.Node) {
			try {
				final Field pointField = value.getClass().getDeclaredField("point"); pointField.setAccessible(true);
				final java.awt.geom.Point2D point = (java.awt.geom.Point2D) pointField.get(value);
				return point.getX() + ":" + point.getY();
			}
			catch (final ReflectiveOperationException error) { throw new IllegalStateException(error); }
		}
		if (value instanceof CHandle) return value.getClass().getName() + ":" + ((CHandle) value).getHandleId();
		if (value == null || value instanceof Number || value instanceof String || value instanceof Enum<?> || value instanceof Boolean) return String.valueOf(value);
		if (value instanceof com.etheller.warsmash.util.War3ID) return value.toString();
		throw new IllegalStateException("Unsupported unordered checkpoint key: " + value.getClass().getName());
	}
	private void visit(final Object value) {
		if (value == null) { token("null"); return; }
		final Class<?> type = value.getClass();
		if (value instanceof java.util.Random) {
			try {
				final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
				new java.io.ObjectOutputStream(bytes).writeObject(value);
				if (this.trace != null) this.trace.append("random:").append(MissionCheckpoint.hex(bytes.toByteArray())).append('\n');
				this.digest.update(bytes.toByteArray()); return;
			}
			catch (final java.io.IOException error) { throw new IllegalStateException(error); }
		}
		if (value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Character || type.isEnum()) {
			token(type.getName()); token(value); return;
		}
		if (value instanceof java.util.concurrent.atomic.AtomicInteger) { token(((java.util.concurrent.atomic.AtomicInteger) value).get()); return; }
		if (value instanceof java.nio.ByteBuffer) {
			final java.nio.ByteBuffer buffer = ((java.nio.ByteBuffer) value).duplicate(); buffer.clear();
			if (this.trace != null) this.trace.append("buffer:").append(buffer.hashCode()).append('\n');
			while (buffer.hasRemaining()) this.digest.update(buffer.get()); return;
		}
		final String name = type.getName().split("/", 2)[0];
		// Java lambda numbering is process-specific. Capture fields are still visited below.
		token(name.contains("$$Lambda$") ? name.substring(0, name.indexOf("$$Lambda$")) + "$$Lambda" : name);
		if (this.seen.containsKey(value)) { token("ref"); token(this.seen.get(value)); return; }
		this.seen.put(value, this.seen.size());
		if (value instanceof com.etheller.interpreter.ast.value.ArrayJassValue) {
			final com.etheller.interpreter.ast.value.ArrayJassValue array = (com.etheller.interpreter.ast.value.ArrayJassValue) value;
			token(array.getType().getName());
			for (int i = 0; i < com.etheller.interpreter.ast.util.JassSettings.MAX_ARRAY_SIZE; i++) {
				final com.etheller.interpreter.ast.value.JassValue element = array.get(i);
				if (element == null || element == com.etheller.interpreter.ast.value.IntegerJassValue.ZERO
						|| element == com.etheller.interpreter.ast.value.RealJassValue.ZERO || element == com.etheller.interpreter.ast.value.BooleanJassValue.FALSE) continue;
				token(i); visit(element);
			}
			token("end-array"); return;
		}
		if (type.isArray()) {
			token(Array.getLength(value)); for (int i = 0; i < Array.getLength(value); i++) visit(Array.get(value, i)); return;
		}
		if (value instanceof com.badlogic.gdx.utils.Array<?>) {
			final com.badlogic.gdx.utils.Array<?> entries = (com.badlogic.gdx.utils.Array<?>) value;
			token(entries.size); for (final Object entry : entries) visit(entry); return;
		}
		if (value instanceof Map<?, ?>) {
			final ArrayList<Map.Entry<?, ?>> entries = new ArrayList<>(((Map<?, ?>) value).entrySet());
			entries.sort(Comparator.comparing(entry -> key(entry.getKey())));
			token(entries.size()); for (final Map.Entry<?, ?> entry : entries) { visit(entry.getKey()); visit(entry.getValue()); } return;
		}
		if (value instanceof Collection<?>) {
			final ArrayList<?> entries = new ArrayList<>((Collection<?>) value);
			if (value instanceof Set<?>) entries.sort(Comparator.comparing(this::key));
			token(entries.size()); for (final Object entry : entries) visit(entry); return;
		}
		if (!name.startsWith("com.etheller.interpreter.ast.") && !name.startsWith("com.etheller.warsmash.viewer5.handlers.w3x.simulation.")
				&& !name.startsWith("com.etheller.warsmash.parsers.jass.scope.") && !name.equals("com.etheller.warsmash.util.War3ID")
				&& !name.startsWith("com.etheller.warsmash.parsers.jass.triggers.")
				&& !name.startsWith("com.etheller.warsmash.parsers.jass.JassAIEnvironment")
				&& !name.startsWith("com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid")
				&& !name.startsWith("com.etheller.warsmash.util.Quadtree")
				&& !name.startsWith("com.badlogic.gdx.math.") && !name.startsWith("java.awt.geom.Point2D")) return;
		if (name.contains("simulation.data.") || name.contains("simulation.util.SimulationRender")) return;
		for (Class<?> base = type; base != null && base != Object.class; base = base.getSuperclass()) {
			final Field[] fields = base.getDeclaredFields(); Arrays.sort(fields, Comparator.comparing(Field::getName));
			for (final Field field : fields) {
				if (name.equals("com.etheller.warsmash.util.Quadtree") && (field.getName().equals("nodeAdder") || field.getName().equals("uniqueNodeAdder"))) continue;
				if (name.equals("com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWorldCollision") && !field.getName().endsWith("Collision") && !field.getName().endsWith("ForEnum") && !field.getName().equals("maxCollisionRadius")) continue;
				if (name.contains("simulation.pathing.CPathfindingProcessor") && Set.of("nodes", "cornerNodes", "searchGraph", "worldCollision", "pathingGrid").contains(field.getName())) continue;
				if (name.contains("environment.PathingGrid") && !Set.of("pathingGrid", "dynamicPathingOverlay", "pathingGridSizes", "centerOffset").contains(field.getName())) continue;
				if (value instanceof com.etheller.warsmash.parsers.jass.JassAIEnvironment && !Set.of("aiPlayerIndex", "captainHomeX", "captainHomeY", "captainX", "captainY", "captainAtHome", "assaultGroup", "nextExpansionX", "nextExpansionY", "guardPosts").contains(field.getName())) continue;
				if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;
				if (value instanceof CSimulation && !SIMULATION_FIELDS.contains(field.getName())) continue;
				if (value instanceof GlobalScope && !GLOBAL_FIELDS.contains(field.getName())) continue;
				if (field.getName().startsWith("this$") || field.getName().equals("simulation") || field.getName().equals("game")
						|| field.getName().equals("globalScope") && !(value instanceof CSimulation)
						|| field.getName().equals("animationListener")
						|| field.getName().equals("lastStartTimestamp") || field.getName().equals("lastSoundInstanceId")) continue;
				try { field.setAccessible(true); token(field.getName()); visit(field.get(value)); }
				catch (final ReflectiveOperationException error) { throw new IllegalStateException("Cannot validate mission field " + field, error); }
			}
		}
	}
}

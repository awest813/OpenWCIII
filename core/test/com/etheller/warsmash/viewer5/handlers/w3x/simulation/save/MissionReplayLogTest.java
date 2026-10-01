package com.etheller.warsmash.viewer5.handlers.w3x.simulation.save;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.interpreter.ast.execution.JassStackFrame;
import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;

class MissionReplayLogTest {
	@TempDir File directory;
	private MissionReplayLog fixture() {
		final MissionReplayLog log = new MissionReplayLog();
		log.localPlayer = 1; log.difficulty = 2; log.defaultDifficulty = 2;
		log.triggerCounter = 452354453; log.cacheCounter = 9;
		log.scriptFingerprint = "a".repeat(64); log.stateFingerprint = "b".repeat(64);
		log.profileName = "WorldEdit"; log.targetTick = 100;
		log.initialCaches.put("read-0", new byte[] { 3, 2, 1 });
		log.initialProgress.put("mission.4294967297", "false");
		log.presentation = new byte[] { 1, 2, 3 };
		log.selectedUnits = new int[] { 8193, 8194 };
		log.entries.add(new MissionReplayLog.Entry(0, MissionReplayLog.CACHE_READ, 0, 0, 0, 0, 0, 0, 0, false, "human01.w3v"));
		log.entries.add(new MissionReplayLog.Entry(75, MissionReplayLog.POINT, 1, 8193, 8300, 851986, 0, 123, -345, true, ""));
		log.entries.add(new MissionReplayLog.Entry(100, MissionReplayLog.HOST_READ, 1, 0, 0, 0, 0, 0, 0, false, "GetCameraField\n1.2345"));
		return log;
	}
	@Test void checkpointRoundtripsThroughAtomicSaveWithoutAliasing() throws IOException {
		final MissionReplayLog original = fixture();
		final CGameSave save = new CGameSave("checkpoint", 2);
		save.savedMapPath = "Maps\\Campaign\\Human01.w3m";
		save.checkpoint = original.copyAt(100, original.stateFingerprint);
		original.initialCaches.get("read-0")[0] = 99;
		original.selectedUnits[0] = 0;
		original.presentation[0] = 99;
		original.entries.clear();
		final File file = new File(this.directory, "checkpoint.w3s");
		save.save(file);
		final MissionReplayLog loaded = CGameSave.tryLoad(file).checkpoint;
		assertArrayEquals(new byte[] { 1, 2, 3 }, loaded.presentation);
		assertEquals(100, loaded.targetTick); assertEquals(3, loaded.entries.size());
		assertEquals(9, loaded.cacheCounter); assertEquals(452354453, loaded.triggerCounter);
		assertArrayEquals(new byte[] { 3, 2, 1 }, loaded.initialCaches.get("read-0"));
		assertArrayEquals(new int[] { 8193, 8194 }, loaded.selectedUnits);
		assertEquals(1.2345, Double.parseDouble(loaded.entries.get(2).text.split("\n")[1]));
		assertEquals("false", loaded.initialProgress.get("mission.4294967297"));
		assertEquals(save.checkpoint.scriptFingerprint, loaded.scriptFingerprint);
	}
	@Test void truncatedCheckpointCannotReplaceAValidRuntime() throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		fixture().write(new DataOutputStream(bytes));
		final byte[] full = bytes.toByteArray();
		for (final int length : new int[] { 0, 8, full.length - 1 }) assertThrows(IOException.class,
				() -> MissionReplayLog.read(new DataInputStream(new ByteArrayInputStream(Arrays.copyOf(full, length)))));
	}
	@Test void unorderedAndFutureInputsAreRejected() throws IOException {
		for (final int tick : new int[] { -1, 101 }) {
			final MissionReplayLog log = fixture();
			log.entries.add(new MissionReplayLog.Entry(tick, MissionReplayLog.GUI_EVENT, 1, 0, 0, 0, 0, 0, 0, false, ""));
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			assertThrows(IOException.class, () -> log.write(new DataOutputStream(bytes)));
		}
	}
	@Test void digestDetectsSleepingLocalValuesAndReturnAddresses() {
		final JassStackFrame frame = new JassStackFrame(); frame.push(IntegerJassValue.of(731));
		final JassThread thread = new JassThread(frame, new GlobalScope(), TriggerExecutionScope.EMPTY, 120);
		thread.sleeping = true;
		final String original = MissionStateFingerprint.ofRuntime(thread);
		frame.contents.set(0, IntegerJassValue.of(732));
		assertNotEquals(original, MissionStateFingerprint.ofRuntime(thread));
		frame.contents.set(0, IntegerJassValue.of(731)); frame.returnAddressInstructionPtr = 27;
		assertNotEquals(original, MissionStateFingerprint.ofRuntime(thread));
		frame.returnAddressInstructionPtr = 0;
		assertEquals(original, MissionStateFingerprint.ofRuntime(thread));
	}

	@Test void v6JournalRemainsReadableWithoutPresentation() throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		final MissionReplayLog original = fixture();
		original.write(new DataOutputStream(bytes));
		final byte[] legacy = Arrays.copyOf(bytes.toByteArray(), bytes.size() - 4 - original.presentation.length);
		final MissionReplayLog read = MissionReplayLog.read(new DataInputStream(new ByteArrayInputStream(legacy)), false);
		assertEquals(original.targetTick, read.targetTick);
		assertEquals(original.entries.size(), read.entries.size());
		assertEquals(0, read.presentation.length);
	}
}

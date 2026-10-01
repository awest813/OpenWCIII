package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.warsmash.datasources.CompoundDataSource;
import com.etheller.warsmash.datasources.MpqDataSource;
import com.etheller.warsmash.parsers.w3x.War3Map;
import com.etheller.warsmash.testutil.RetailTestData;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.RetailSimulationTestSupport;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint;

import mpq.MPQArchive;

class JassAIExpansionTest {

	@Test
	void testExpansionAndGuardPostNativesRegistered() {
		final JassProgram program = new JassProgram();
		final JassAIEnvironment ai = new JassAIEnvironment(program, null, null, null, null, null, null, 1);
		assertNotNull(ai);

		final GlobalScope globals = program.getGlobals();

		// GetNextExpansion returns -1 when no expansion mine found
		final JassFunction getNextExpansion = program.getJassNativeManager().getNative("GetNextExpansion");
		assertNotNull(getNextExpansion);
		final JassValue nextExpVal = getNextExpansion.call(Collections.emptyList(), globals,
				TriggerExecutionScope.EMPTY);
		assertEquals(-1, nextExpVal.visit(IntegerJassValueVisitor.getInstance()));

		// GetExpansionX / GetExpansionY return town center coords (0 default when no simulation)
		final JassFunction getExpX = program.getJassNativeManager().getNative("GetExpansionX");
		assertNotNull(getExpX);
		final JassValue expXVal = getExpX.call(Collections.emptyList(), globals, TriggerExecutionScope.EMPTY);
		assertEquals(0, expXVal.visit(IntegerJassValueVisitor.getInstance()));

		final JassFunction getExpY = program.getJassNativeManager().getNative("GetExpansionY");
		assertNotNull(getExpY);
		final JassValue expYVal = getExpY.call(Collections.emptyList(), globals, TriggerExecutionScope.EMPTY);
		assertEquals(0, expYVal.visit(IntegerJassValueVisitor.getInstance()));

		// TownCount returns 0 when no simulation
		final JassFunction townCount = program.getJassNativeManager().getNative("TownCount");
		assertNotNull(townCount);
		final JassValue tcVal = townCount.call(Collections.singletonList(IntegerJassValue.of(0)), globals,
				TriggerExecutionScope.EMPTY);
		assertEquals(0, tcVal.visit(IntegerJassValueVisitor.getInstance()));

		// CreepsOnMap returns false when no simulation
		final JassFunction creepsOnMap = program.getJassNativeManager().getNative("CreepsOnMap");
		assertNotNull(creepsOnMap);
		final JassValue creepsVal = creepsOnMap.call(Collections.emptyList(), globals,
				TriggerExecutionScope.EMPTY);
		assertFalse(creepsVal.visit(BooleanJassValueVisitor.getInstance()));

		// GetCreepCamp returns null unit when no creeps
		final JassFunction getCreepCamp = program.getJassNativeManager().getNative("GetCreepCamp");
		assertNotNull(getCreepCamp);
		final JassValue campVal = getCreepCamp.call(Arrays.asList(IntegerJassValue.of(1), IntegerJassValue.of(10)),
				globals, TriggerExecutionScope.EMPTY);
		assertNotNull(campVal);

		// AddGuardPost, FillGuardPosts, ReturnGuardPosts execute cleanly
		final JassFunction addGuardPost = program.getJassNativeManager().getNative("AddGuardPost");
		assertNotNull(addGuardPost);
		addGuardPost.call(
				Arrays.asList(IntegerJassValue.of(1234), RealJassValue.of(500.0), RealJassValue.of(600.0)), globals,
				TriggerExecutionScope.EMPTY);

		final JassFunction fillGuardPosts = program.getJassNativeManager().getNative("FillGuardPosts");
		assertNotNull(fillGuardPosts);
		fillGuardPosts.call(Collections.emptyList(), globals, TriggerExecutionScope.EMPTY);

		final JassFunction returnGuardPosts = program.getJassNativeManager().getNative("ReturnGuardPosts");
		assertNotNull(returnGuardPosts);
		returnGuardPosts.call(Collections.emptyList(), globals, TriggerExecutionScope.EMPTY);
	}

	@Test
	void checkpointDigestIncludesCaptainAndGuardPostState() {
		final JassProgram program = new JassProgram();
		final JassAIEnvironment ai = new JassAIEnvironment(program, null, null, null, null, null, null, 1);
		final GlobalScope globals = program.getGlobals();
		final String initial = MissionStateFingerprint.ofRuntime(ai);
		program.getJassNativeManager().getNative("SetCaptainHome").call(
				Arrays.asList(IntegerJassValue.ZERO, RealJassValue.of(500), RealJassValue.of(600)), globals, TriggerExecutionScope.EMPTY);
		assertNotEquals(initial,
				MissionStateFingerprint.ofRuntime(ai));
		program.getJassNativeManager().getNative("SetCaptainHome").call(
				Arrays.asList(IntegerJassValue.ZERO, RealJassValue.ZERO, RealJassValue.ZERO), globals, TriggerExecutionScope.EMPTY);
		assertEquals(initial, MissionStateFingerprint.ofRuntime(ai));
		program.getJassNativeManager().getNative("AddGuardPost").call(
				Arrays.asList(IntegerJassValue.of(1234), RealJassValue.of(500), RealJassValue.of(600)), globals, TriggerExecutionScope.EMPTY);
		assertNotEquals(initial,
				MissionStateFingerprint.ofRuntime(ai));
	}

	@Test
	void spawnedUnitsAreImmediatelyAvailableToAssaultsGuardsAndCounts() throws Exception {
		final List<MpqDataSource> archives = new ArrayList<>();
		final var previousRaceManager = WarsmashConstants.RACE_MANAGER;
		try {
			for (final String name : new String[] { "war3.mpq", "War3x.mpq", "War3xlocal.mpq" }) {
				final var channel = Files.newByteChannel(RetailTestData.require(name));
				archives.add(new MpqDataSource(new MPQArchive(channel), channel));
			}
			final var source = new CompoundDataSource(new ArrayList<>(archives));
			try (final var map = new War3Map(source, "Maps\\Campaign\\Human01.w3m")) {
				final var simulation = RetailSimulationTestSupport.simulation(map);
				final var archer = simulation.createUnitSimple(War3ID.fromString("earc"), 1, 0, 0, 0);
				final var guard = simulation.createUnitSimple(War3ID.fromString("hfoo"), 1, 128, 128, 0);
				assertTrue(simulation.getUnits().isEmpty(), "Fixture must exercise pending creations");
				final JassProgram program = new JassProgram();
				final JassAIEnvironment ai = new JassAIEnvironment(program, null, null, null, null,
						new War3MapConfig(WarsmashConstants.MAX_PLAYERS), simulation, 1);
				final var natives = program.getJassNativeManager();
				final var globals = ai.getGlobalScope();
				assertTrue(natives.getNative("AddAssault").call(Arrays.asList(IntegerJassValue.of(1), IntegerJassValue.of(archer.getTypeId().getValue())),
						globals, TriggerExecutionScope.EMPTY).visit(BooleanJassValueVisitor.getInstance()));
				assertEquals(1, natives.getNative("CaptainGroupSize").call(Collections.emptyList(), globals,
						TriggerExecutionScope.EMPTY).visit(IntegerJassValueVisitor.getInstance()));
				natives.getNative("AddGuardPost").call(Arrays.asList(IntegerJassValue.of(guard.getTypeId().getValue()), RealJassValue.of(256), RealJassValue.ZERO),
						globals, TriggerExecutionScope.EMPTY);
				natives.getNative("FillGuardPosts").call(Collections.emptyList(), globals, TriggerExecutionScope.EMPTY);
				assertTrue(guard.getCurrentBehavior() instanceof CBehaviorMove);
				simulation.removeUnit(archer);
				assertEquals(0, natives.getNative("GetUnitCount").call(Collections.singletonList(IntegerJassValue.of(archer.getTypeId().getValue())),
						globals, TriggerExecutionScope.EMPTY).visit(IntegerJassValueVisitor.getInstance()), "Queued removals must not count as available units");
			}
		}
		finally {
			WarsmashConstants.RACE_MANAGER = previousRaceManager;
			for (final var archive : archives) archive.close();
		}
	}
}
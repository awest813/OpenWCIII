package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

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
}

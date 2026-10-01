package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.CodeJassValue;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;

import net.warsmash.parsers.jass.SmashJassParser;

class JassTriggerExecutionTest {
	@Test
	void nestedInitializationCompletesBeforeOpeningReadsHeroState() throws Exception {
		final var program = new JassProgram();
		final var globals = program.getGlobals();
		final var load = new Trigger();
		final var hero = new Trigger();
		program.jassNativeManager.createNative("LoadHeroes", (args, g, s) -> {
			load.executeImmediately(g, s);
			return null;
		});
		program.jassNativeManager.createNative("LoadHero", (args, g, s) -> {
			hero.executeImmediately(g, s);
			return null;
		});
		new SmashJassParser(new StringReader("""
				native LoadHeroes takes nothing returns nothing
				native LoadHero takes nothing returns nothing
				globals
				integer heroState = 0
				integer openingState = 0
				endglobals
				function hero takes nothing returns nothing
				set heroState = 7
				endfunction
				function load takes nothing returns nothing
				call LoadHero()
				endfunction
				function main takes nothing returns nothing
				call LoadHeroes()
				set openingState = heroState
				endfunction
				""")).scanAndParse("trigger-test.j", program);
		program.initialize();
		load.addAction(new CodeJassValue(globals.getUserFunctionInstructionPtr("load")));
		hero.addAction(new CodeJassValue(globals.getUserFunctionInstructionPtr("hero")));
		globals.queueThread(globals.createThread("main", Collections.emptyList(), TriggerExecutionScope.EMPTY));
		globals.runThreads();
		assertEquals(7, globals.getGlobal("openingState").visit(IntegerJassValueVisitor.getInstance()));
	}

	@Test
	void sleepingActionContinuesSeparatelyWithoutRepeatingItsPrefix() throws Exception {
		final var program = new JassProgram();
		final var globals = program.getGlobals();
		final var trigger = new Trigger();
		final JassThread[] sleeping = new JassThread[1];
		program.jassNativeManager.createNative("Sleep", (args, g, s) -> {
			sleeping[0] = g.getCurrentThread();
			sleeping[0].setSleeping(true);
			return null;
		});
		program.jassNativeManager.createNative("Execute", (args, g, s) -> {
			trigger.executeImmediately(g, s);
			return null;
		});
		new SmashJassParser(new StringReader("""
				native Sleep takes nothing returns nothing
				native Execute takes nothing returns nothing
				globals
				integer phase = 0
				integer callerPhase = 0
				endglobals
				function action takes nothing returns nothing
				set phase = phase + 1
				call Sleep()
				set phase = phase + 1
				endfunction
				function main takes nothing returns nothing
				call Execute()
				set callerPhase = phase
				endfunction
				""")).scanAndParse("trigger-test.j", program);
		program.initialize();
		trigger.addAction(new CodeJassValue(globals.getUserFunctionInstructionPtr("action")));
		globals.queueThread(globals.createThread("main", Collections.emptyList(), TriggerExecutionScope.EMPTY));
		assertFalse(globals.runThreads());
		assertEquals(1, globals.getGlobal("callerPhase").visit(IntegerJassValueVisitor.getInstance()));
		sleeping[0].setSleeping(false);
		assertTrue(globals.runThreads());
		assertEquals(2, globals.getGlobal("phase").visit(IntegerJassValueVisitor.getInstance()));
	}
}

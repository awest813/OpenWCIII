package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import net.warsmash.parsers.jass.SmashJassParser;

class JassIncrementalInitializationTest {
    private static void load(JassProgram program, String source) throws Exception {
        new SmashJassParser(new StringReader(source)).scanAndParse("preload-test.j", program);
        program.initialize();
    }

    private static int integer(GlobalScope globals, String name) {
        return globals.getGlobal(name).visit(IntegerJassValueVisitor.getInstance());
    }

    @Test void functionOnlyPreloadPreservesRunningMissionGlobals() throws Exception {
        JassProgram program = new JassProgram();
        load(program, "globals\ninteger missionState = 0\nhandle missionForce = null\nendglobals\n"
                + "function start takes nothing returns nothing\nset missionState = 7\nendfunction\n");
        GlobalScope globals = program.getGlobals();
        globals.runThreadUntilCompletion(globals.createThread("start", java.util.Collections.emptyList(),
                com.etheller.interpreter.ast.scope.TriggerExecutionScope.EMPTY));
        HandleJassValue missionForce = new HandleJassValue(globals.handleType, new Object());
        globals.setGlobal("missionForce", missionForce);
        load(program, "function PreloadFiles takes nothing returns nothing\nendfunction\n");
        assertEquals(7, integer(globals, "missionState"), "Preloader must not reset the running mission");
        assertSame(missionForce, globals.getGlobal("missionForce"), "The cinematic's player force must survive");
        program.initialize();
        assertEquals(7, integer(globals, "missionState"), "An empty load must also preserve mission state");
    }

    @Test void laterGlobalsInitializeOnceAndCanReadExistingState() throws Exception {
        JassProgram program = new JassProgram();
        load(program, "globals\ninteger missionState = 7\nendglobals\n");
        load(program, "globals\ninteger preloadState = missionState + 1\nendglobals\n");
        assertEquals(8, integer(program.getGlobals(), "preloadState"));
        load(program, "function PreloadFiles takes nothing returns nothing\nset preloadState = 20\nendfunction\n");
        program.getGlobals().runThreadUntilCompletion(program.getGlobals().createThread("PreloadFiles",
                java.util.Collections.emptyList(), com.etheller.interpreter.ast.scope.TriggerExecutionScope.EMPTY));
        program.initialize();
        assertEquals(20, integer(program.getGlobals(), "preloadState"));
    }
}

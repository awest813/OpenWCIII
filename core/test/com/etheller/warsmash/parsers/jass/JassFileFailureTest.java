package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.warsmash.datasources.DataSource;

class JassFileFailureTest {
    @Test
    void unaryPlusPreservesValuesAndArithmeticPrecedence() throws Exception {
        final JassProgram program = new JassProgram();
        new net.warsmash.parsers.jass.SmashJassParser(new java.io.StringReader(
                "globals\ninteger positive = +3 * 2 + 1\nreal negative = +(-2.5)\nendglobals\n"))
                .scanAndParse("unary-plus.ai", program);
        program.initialize();
        assertEquals(7, program.getGlobals().getGlobal("positive").visit(
                com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor.getInstance()));
        assertEquals(-2.5, program.getGlobals().getGlobal("negative").visit(
                com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor.getInstance()));
    }

    @Test
    void invalidScriptCannotBeReportedAsLoaded() {
        final DataSource source = (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getResourceAsStream")) {
                        return new ByteArrayInputStream("function broken takes returns\n".getBytes(StandardCharsets.UTF_8));
                    }
                    throw new AssertionError(method.getName());
                });
        final IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> Jass2.readJassFile(source, new JassProgram(), "broken.ai"));
        assertTrue(failure.getMessage().contains("broken.ai"));
        assertNotNull(failure.getCause());
    }
}

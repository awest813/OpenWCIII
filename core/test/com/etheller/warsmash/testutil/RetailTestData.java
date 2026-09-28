package com.etheller.warsmash.testutil;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;

/** Owned retail fixtures are optional locally and mandatory in explicit retail validation. */
public final class RetailTestData {
    private RetailTestData() { }

    public static Path require(String name) {
        final Path path = Path.of(System.getProperty("warsmash.test.assets", "F:/WC3Data")).resolve(name);
        final String message = "Missing retail archive: " + path + "; configure -PretailAssets=<directory>";
        if (!Files.isRegularFile(path) && Boolean.getBoolean("warsmash.test.requireAssets")) {
            throw new AssertionError(message);
        }
        Assumptions.assumeTrue(Files.isRegularFile(path), message);
        return path;
    }

    public static String[] requireAll() {
        return new String[] { require("war3.mpq").toString(), require("War3x.mpq").toString(),
                require("War3xlocal.mpq").toString() };
    }
}

package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.datasources.DataSource;

class CommonAiPreloadTest {

	@Test
	void preloadCommonAiWithDummySourceDoesNotCrash() {
		final DataSource dummySource = new DataSource() {
			@Override
			public InputStream getResourceAsStream(final String filepath) {
				if (filepath.toLowerCase().contains("common.ai")) {
					return new ByteArrayInputStream("// empty common.ai for test\n".getBytes(StandardCharsets.UTF_8));
				}
				return null;
			}

			@Override
			public boolean has(final String filepath) {
				return filepath.toLowerCase().contains("common.ai");
			}

			@Override
			public Collection<String> getListfile() {
				return Collections.emptyList();
			}

			@Override
			public java.io.File getFile(final String filepath) {
				return null;
			}

			@Override
			public java.io.File getDirectory(final String filepath) {
				return null;
			}

			@Override
			public java.nio.ByteBuffer read(final String path) {
				return null;
			}

			@Override
			public void close() {
			}
		};

		try {
			assertDoesNotThrow(() -> {
				JassAIEnvironment.preloadCommonAi(dummySource);
				// Second call should hit the cache without re-parsing
				JassAIEnvironment.preloadCommonAi(dummySource);
			}, "preloadCommonAi should execute and cache without throwing exceptions");
		}
		finally {
			JassAIEnvironment.resetCachedCommonAi();
		}
	}
}

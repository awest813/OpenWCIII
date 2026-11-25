package com.etheller.warsmash.parsers.jass;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.datasources.FolderDataSource;

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

	@Test
	void newChapterUsesItsOwnCommonAiDefinitions(@TempDir final Path directory) throws Exception {
		final FolderDataSource first = createChapter(directory.resolve("first"), 10, 1);
		final FolderDataSource second = createChapter(directory.resolve("second"), 20, 2);
		try {
			JassAIEnvironment.preloadCommonAi(first);
			assertAiChapterValue(first, 11);
			assertAiChapterValue(second, 22);
			assertAiChapterValue(first, 11);
		}
		finally {
			JassAIEnvironment.resetCachedCommonAi();
		}
	}

	private static FolderDataSource createChapter(final Path chapter, final int base, final int bonus) throws Exception {
		final Path scripts = Files.createDirectories(chapter.resolve("Scripts"));
		Files.writeString(scripts.resolve("common.j"), "globals\ninteger chapterBase = " + base + "\nendglobals\n");
		Files.writeString(scripts.resolve("common.ai"), "function ChapterValue takes nothing returns integer\n"
				+ "return chapterBase + " + bonus + "\nendfunction\n");
		Files.writeString(scripts.resolve("chapter.ai"), """
				globals
				integer chapterResult = 0
				endglobals
				function main takes nothing returns nothing
					set chapterResult = ChapterValue()
				endfunction
				""");
		return new FolderDataSource(chapter);
	}

	private static void assertAiChapterValue(final DataSource source, final int expected) {
		final JassAIEnvironment ai = JassAIEnvironment.loadAI(source, null, null, null, null, null, 1, "chapter.ai");
		assertNotNull(ai);
		ai.main();
		final var globals = ai.getGlobalScope();
		globals.runThreads();
		assertEquals(expected, globals.getGlobal("chapterResult").visit(IntegerJassValueVisitor.getInstance()));
	}
}

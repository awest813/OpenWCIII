package com.etheller.warsmash.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class CompoundDataSourceCacheTest {

	private static class CountingDataSource implements DataSource {
		private final String presentFile;
		public final AtomicInteger hasCalls = new AtomicInteger(0);

		public CountingDataSource(final String presentFile) {
			this.presentFile = presentFile;
		}

		@Override
		public InputStream getResourceAsStream(String filepath) {
			return null;
		}

		@Override
		public File getFile(String filepath) {
			return null;
		}

		@Override
		public File getDirectory(String filepath) {
			return null;
		}

		@Override
		public ByteBuffer read(String path) {
			return null;
		}

		@Override
		public boolean has(String filepath) {
			this.hasCalls.incrementAndGet();
			return this.presentFile.equalsIgnoreCase(filepath);
		}

		@Override
		public Collection<String> getListfile() {
			return Collections.singletonList(this.presentFile);
		}

		@Override
		public void close() {
		}
	}

	@Test
	public void testPositiveAndNegativeLookupCaching() {
		final CountingDataSource ds = new CountingDataSource("doodads\\tree.mdx");
		final CompoundDataSource compound = new CompoundDataSource(Collections.singletonList(ds));

		// Positive lookup
		assertTrue(compound.has("doodads\\tree.mdx"));
		assertEquals(1, ds.hasCalls.get(), "First positive lookup queries underlying data source");

		assertTrue(compound.has("doodads\\tree.mdx"));
		assertEquals(1, ds.hasCalls.get(), "Second positive lookup is answered from hasCache in O(1)");

		// Negative lookup
		assertFalse(compound.has("doodads\\tree_missing.mdx"));
		assertEquals(2, ds.hasCalls.get(), "First negative lookup queries underlying data source");

		assertFalse(compound.has("doodads\\tree_missing.mdx"));
		assertEquals(2, ds.hasCalls.get(), "Second negative lookup is answered from hasCache in O(1)");
	}

	@Test
	public void testNullSafety() {
		final CountingDataSource ds = new CountingDataSource("any.mdx");
		final CompoundDataSource compound = new CompoundDataSource(Collections.singletonList(ds));

		assertFalse(compound.has(null), "Null filepath returns false without NPE");
		assertEquals(0, ds.hasCalls.get(), "Null filepath does not query underlying data sources");
	}

	@Test
	public void testRefreshClearsCache() {
		final CountingDataSource ds1 = new CountingDataSource("file1.mdx");
		final CompoundDataSource compound = new CompoundDataSource(Collections.singletonList(ds1));

		assertTrue(compound.has("file1.mdx"));
		assertEquals(1, ds1.hasCalls.get());

		// Second query hits cache
		assertTrue(compound.has("file1.mdx"));
		assertEquals(1, ds1.hasCalls.get());

		// Refresh with empty descriptors
		compound.refresh(new ArrayList<>());
		assertFalse(compound.has("file1.mdx"), "After refresh and clearing, file is no longer present");
	}
}

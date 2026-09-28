package com.etheller.warsmash.viewer5.handlers.mdx;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.hiveworkshop.rms.parsers.mdlx.MdlxGeoset;

class GraphicsQualityTest {
	private static MdlxGeoset geoset(final int lod) {
		final MdlxGeoset geoset = new MdlxGeoset();
		geoset.lod = lod;
		return geoset;
	}

	@Test void authoredLevelsAndLegacyModelsSelectOneUsableLevel() {
		final List<MdlxGeoset> levels = Arrays.asList(geoset(-1), geoset(0), geoset(1), geoset(2));
		assertEquals(0, ModelLod.select(levels, 2));
		assertEquals(1, ModelLod.select(levels, 1));
		assertEquals(2, ModelLod.select(levels, 0));
		for (int quality = 0; quality < 3; quality++) {
			assertEquals(0, ModelLod.select(Arrays.asList(geoset(0), geoset(-1)), quality));
		}
		assertEquals(0, ModelLod.select(Arrays.asList(geoset(0), geoset(2)), 1));
		assertEquals(2, ModelLod.select(Arrays.asList(geoset(-1), geoset(2)), 2));
	}

	@Test void skeletalQualitySamplesAt15And30HzWithoutDroppingFrameTime() {
		for (int quality = 0; quality < 3; quality++) {
			final AnimationSampleClock clock = new AnimationSampleClock();
			assertTrue(clock.advance(0, 0, quality, false));
			int samples = 0;
			for (int frame = 0; frame < 120; frame++) {
				if (clock.advance(1f / 60f, 0, quality, false)) samples++;
			}
			assertEquals(quality == 0 ? 30 : quality == 1 ? 60 : 120, samples);
		}
	}

	@Test void animationChangesAndForcedUpdatesSampleImmediately() {
		final AnimationSampleClock clock = new AnimationSampleClock();
		assertTrue(clock.advance(0, 0, 0, false));
		assertFalse(clock.advance(0.01f, 0, 0, false));
		assertTrue(clock.advance(0, 1, 0, false));
		assertTrue(clock.advance(0, 1, 0, true));
		assertTrue(clock.advance(0, 1, 1, false));
		assertTrue(clock.advance(0.5f, 1, 1, false));
	}
}

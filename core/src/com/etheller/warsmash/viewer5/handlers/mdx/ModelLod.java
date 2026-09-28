package com.etheller.warsmash.viewer5.handlers.mdx;

import java.util.List;
import com.hiveworkshop.rms.parsers.mdlx.MdlxGeoset;

/** Chooses one authored detail level, retaining shared (-1) geosets separately. */
public final class ModelLod {
	private ModelLod() { }

	public static int select(final List<MdlxGeoset> geosets, final int quality) {
		final int requested = 2 - Math.max(0, Math.min(2, quality));
		int best = -1;
		int minimum = Integer.MAX_VALUE;
		for (final MdlxGeoset geoset : geosets) {
			if (geoset.lod >= 0) {
				minimum = Math.min(minimum, geoset.lod);
				if (geoset.lod <= requested) best = Math.max(best, geoset.lod);
			}
		}
		return best >= 0 ? best : (minimum == Integer.MAX_VALUE ? 0 : minimum);
	}
}

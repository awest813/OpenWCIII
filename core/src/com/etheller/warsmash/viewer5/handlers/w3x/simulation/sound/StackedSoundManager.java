package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages stacked sound registrations. In Warcraft III retail, RegisterStackedSound
 * registers ambient or 3D sounds with a bounding box and concurrency limits to prevent
 * excessive sound accumulation and volume distortion in congested areas.
 */
public final class StackedSoundManager {

	public static final class StackedSoundRegistration {
		public final CSound sound;
		public final boolean byPosition;
		public final float rectWidth;
		public final float rectHeight;

		public StackedSoundRegistration(final CSound sound, final boolean byPosition, final float rectWidth,
				final float rectHeight) {
			this.sound = sound;
			this.byPosition = byPosition;
			this.rectWidth = rectWidth;
			this.rectHeight = rectHeight;
		}
	}

	private final List<StackedSoundRegistration> registrations = new ArrayList<>();

	public void register(final CSound sound, final boolean byPosition, final float rectWidth,
			final float rectHeight) {
		if (sound == null) {
			return;
		}
		unregister(sound, byPosition, rectWidth, rectHeight);
		this.registrations.add(new StackedSoundRegistration(sound, byPosition, rectWidth, rectHeight));
	}

	public void unregister(final CSound sound, final boolean byPosition, final float rectWidth,
			final float rectHeight) {
		if (sound == null) {
			return;
		}
		this.registrations.removeIf(reg -> reg.sound == sound);
	}

	public boolean isRegistered(final CSound sound) {
		for (final StackedSoundRegistration reg : this.registrations) {
			if (reg.sound == sound) {
				return true;
			}
		}
		return false;
	}

	public int getRegisteredCount() {
		return this.registrations.size();
	}

	public void clear() {
		this.registrations.clear();
	}
}

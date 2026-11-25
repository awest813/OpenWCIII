package com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Keeps scripted spatial sounds attached to their moving sources for the life of a map. */
public final class AttachedSoundManager {
	private final Map<CSound, Consumer<CSound>> attachments = new WeakHashMap<>();

	public void attach(final CSound sound, final Consumer<CSound> source) {
		this.attachments.put(sound, source);
		source.accept(sound);
	}

	public void detach(final CSound sound) {
		this.attachments.remove(sound);
	}

	public void updatePosition(final CSound sound) {
		final Consumer<CSound> source = this.attachments.get(sound);
		if (source != null) {
			source.accept(sound);
		}
	}

	public void updatePlayback() {
		this.attachments.forEach((sound, source) -> {
			if (sound.isPlaying()) {
				source.accept(sound);
			}
		});
	}

	public void stopAll() {
		this.attachments.keySet().forEach(CSound::stop);
		this.attachments.clear();
	}
}

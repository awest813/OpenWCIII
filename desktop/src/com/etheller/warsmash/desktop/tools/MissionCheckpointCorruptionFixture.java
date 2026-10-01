package com.etheller.warsmash.desktop.tools;

import java.io.File;

import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;

/** Generates isolated invalid checkpoints for the opt-in rendered load-failure audit. */
public final class MissionCheckpointCorruptionFixture {
	public static void main(final String[] arguments) throws Exception {
		final File original = new File(arguments[0]);
		final CGameSave save = CGameSave.tryLoad(original);
		if (save == null || save.checkpoint == null) throw new IllegalStateException("Missing complete checkpoint");
		final String fingerprint = save.checkpoint.scriptFingerprint;
		save.checkpoint.scriptFingerprint = (fingerprint.charAt(0) == '0' ? "1" : "0") + fingerprint.substring(1);
		save.save(new File(original.getParentFile(), "WrongEngine.w3s"));
		save.checkpoint.scriptFingerprint = fingerprint;
		final String state = save.checkpoint.stateFingerprint;
		save.checkpoint.stateFingerprint = (state.charAt(0) == '0' ? "1" : "0") + state.substring(1);
		save.save(new File(original.getParentFile(), "WrongState.w3s"));
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes;

import com.etheller.interpreter.ast.util.CHandle;

public enum CSoundVolumeGroup implements CHandle {
	UNITMOVEMENT,
	UNITSOUNDS,
	COMBAT,
	SPELLS,
	UI,
	MUSIC,
	AMBIENTSOUNDS,
	FIRE,
	// some reforged related thing below:
	CINEMATIC_GENERAL,
	CINEMATIC_AMBIENT,
	CINEMATIC_MUSIC,
	CINEMATIC_DIALOGUE,
	CINEMATIC_SOUND_EFFECTS_1,
	CINEMATIC_SOUND_EFFECTS_2,
	CINEMATIC_SOUND_EFFECTS_3;

	public static CSoundVolumeGroup[] VALUES = values();

	public static CSoundVolumeGroup forClassicSoundChannel(final int channel) {
		return switch (channel) {
		case 1, 2, 4 -> UNITSOUNDS;
		case 3, 9 -> UNITMOVEMENT;
		case 5 -> COMBAT;
		case 6, 8 -> UI;
		case 7 -> MUSIC;
		case 10 -> AMBIENTSOUNDS;
		case 14 -> FIRE;
		default -> null;
		};
	}

	@Override
	public int getHandleId() {
		return ordinal();
	}
}

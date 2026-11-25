package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;

public class CTimedCircleFogModifier extends CFogModifier {
	private final CFogState state;
	private final byte detections;
	private boolean enabled = true;
	private float myX;
	private float myY;
	private float radius;
	private float duration;
	private int endTurnTick;

	public CTimedCircleFogModifier(final CFogState fogState, final float radius, final float x, final float y, final float duration) {
		this(fogState, radius, x, y, duration, (byte) 0);
	}

	public CTimedCircleFogModifier(final CFogState fogState, final float radius, final float x, final float y,
			final float duration, final byte detections) {
		this.state = fogState;
		this.detections = detections;
		this.radius = radius;
		this.myX = x;
		this.myY = y;
		this.duration = duration;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public void onAdd(final CSimulation game, final CPlayer player) {
		this.endTurnTick = (int) Math.floor(game.getGameTurnTick() + (this.duration / WarsmashConstants.SIMULATION_STEP_TIME));
	}

	@Override
	public void update(final CSimulation game, final CPlayer player, final PathingGrid pathingGrid, final CPlayerFogOfWar fogOfWar) {
		if (game.getGameTurnTick() > endTurnTick) {
			player.removeFogModifer(game, this);
			return;
		}
		if (this.enabled && this.radius > 0) {
			fogOfWar.setFogStateRadius(pathingGrid, this.myX, this.myY, this.radius, this.state, this.detections);
		}
	}
}

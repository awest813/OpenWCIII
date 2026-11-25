package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public abstract class CTimer {
	private int engineFireTick;
	private int scheduleTick;
	private float timeoutTime;
	private float cycleDuration;
	private int elapsedTicks;
	private int pausedTicksRemaining;
	private boolean running;
	private boolean paused;
	private boolean expired;
	private boolean repeats;

	public void setTimeoutTime(final float timeoutTime) {
		this.timeoutTime = timeoutTime;
	}

	public boolean isRepeats() {
		return this.repeats;
	}

	public boolean isRunning() {
		return this.running;
	}

	public float getTimeoutTime() {
		return this.timeoutTime;
	}

	public void start(final CSimulation simulation) {
		startCycle(simulation, this.timeoutTime);
	}

	public void startRepeatingTimerWithDelay(final CSimulation simulation, final float delay) {
		this.repeats = true;
		startCycle(simulation, delay);
	}

	private void startCycle(final CSimulation simulation, final float duration) {
		this.cycleDuration = Math.max(0, duration);
		this.elapsedTicks = 0;
		this.expired = false;
		schedule(simulation, (int) (this.cycleDuration / WarsmashConstants.SIMULATION_STEP_TIME));
	}

	private void schedule(final CSimulation simulation, final int ticks) {
		this.running = true;
		this.paused = false;
		this.scheduleTick = simulation.getGameTurnTick();
		this.engineFireTick = this.scheduleTick + ticks;
		simulation.registerTimer(this);
	}

	public void pause(final CSimulation simulation) {
		if (!this.running) {
			return;
		}
		final int currentTick = simulation.getGameTurnTick();
		this.elapsedTicks += currentTick - this.scheduleTick;
		this.pausedTicksRemaining = Math.max(0, this.engineFireTick - currentTick);
		this.running = false;
		this.paused = true;
		simulation.unregisterTimer(this);
	}

	public void resume(final CSimulation simulation) {
		if (this.paused) {
			schedule(simulation, this.pausedTicksRemaining);
		}
	}

	public float getElapsed(final CSimulation simulation) {
		if (this.expired) {
			return this.cycleDuration;
		}
		final int elapsedTicks = this.elapsedTicks + (this.running ? simulation.getGameTurnTick() - this.scheduleTick : 0);
		return Math.min(elapsedTicks * WarsmashConstants.SIMULATION_STEP_TIME, this.cycleDuration);
	}

	public float getRemaining(final CSimulation simulation) {
		return this.cycleDuration - getElapsed(simulation);
	}

	public void setRepeats(final boolean repeats) {
		this.repeats = repeats;
	}

	public int getEngineFireTick() {
		return this.engineFireTick;
	}

	public abstract void onFire(final CSimulation simulation);

	public void fire(final CSimulation simulation) {
		if (!this.running) {
			return;
		}
		this.running = false;
		this.expired = true;
		final boolean repeats = this.repeats;
		onFire(simulation);
		if (repeats && !this.running) {
			start(simulation);
		}
	}
}

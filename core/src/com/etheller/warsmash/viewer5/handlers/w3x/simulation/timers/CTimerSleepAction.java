package com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers;

import com.etheller.interpreter.ast.execution.JassThread;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;

public class CTimerSleepAction extends CTimer {
	private final JassThread sleepingThread;

	private boolean cancelled = false;

	public CTimerSleepAction(final JassThread sleepingThread) {
		this.sleepingThread = sleepingThread;
	}

	public void cancel() {
		this.cancelled = true;
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void onFire(final CSimulation simulation) {
		if (!this.cancelled) {
			this.sleepingThread.setSleeping(false);
		}
	}

}

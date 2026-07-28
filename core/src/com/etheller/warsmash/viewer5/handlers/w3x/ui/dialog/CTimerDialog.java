package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

public class CTimerDialog {
	private final CTimer timer;
	private final UIFrame timerDialogFrame;
	private final StringFrame valueFrame;
	private final StringFrame titleFrame;
	private float speedMultFactor = 1f;

	public CTimerDialog(final CTimer timer, final UIFrame timerDialogFrame, final StringFrame valueFrame,
			final StringFrame titleFrame) {
		this.timer = timer;
		this.timerDialogFrame = timerDialogFrame;
		this.valueFrame = valueFrame;
		this.titleFrame = titleFrame;
	}

	public void setTitle(final GameUI rootFrame, final String title) {
		rootFrame.setText(this.titleFrame, title);
	}

	public void setValue(final GameUI rootFrame, final String value) {
		rootFrame.setText(this.valueFrame, value);
	}

	public void setTitleColor(final int red, final int green, final int blue, final int alpha) {
		if (this.titleFrame != null) {
			this.titleFrame.setColor(new Color(red / 255f, green / 255f, blue / 255f, alpha / 255f));
		}
	}

	public void setTimeColor(final int red, final int green, final int blue, final int alpha) {
		if (this.valueFrame != null) {
			this.valueFrame.setColor(new Color(red / 255f, green / 255f, blue / 255f, alpha / 255f));
		}
	}

	public void setSpeed(final float speedMultFactor) {
		this.speedMultFactor = speedMultFactor > 0f ? speedMultFactor : 1f;
	}

	public void setVisible(final boolean visible) {
		this.timerDialogFrame.setVisible(visible);
	}

	public boolean isVisible() {
		return this.timerDialogFrame.isVisible();
	}

	public void update(final GameUI rootFrame, final CSimulation simulation) {
		if (this.timerDialogFrame.isVisible() && (this.timer != null)) {
			final float remaining = this.timer.getRemaining(simulation) / this.speedMultFactor;
			final int secondsRemaining = Math.max(0, (int) remaining);
			final int minutes = secondsRemaining / 60;
			final int seconds = secondsRemaining % 60;

			rootFrame.setText(this.valueFrame, minutes + ":" + String.format("%02d", seconds));
		}
	}

}

package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.AbstractUIFrame;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;

public class CTimerDialog {
	private final CTimer timer;
	private final AbstractUIFrame timerDialogFrame;
	private final StringFrame valueFrame;
	private final StringFrame titleFrame;
	private float speedMultFactor = 1f;
	private int layoutSlot = -1;
	private int displayedSeconds = -1;

	public CTimerDialog(final CTimer timer, final AbstractUIFrame timerDialogFrame, final StringFrame valueFrame,
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

	public void position(final GameUI rootFrame, final Viewport viewport, final UIFrame anchor, final int slot) {
		if (this.layoutSlot != slot) {
			this.layoutSlot = slot;
			this.timerDialogFrame.clearFramePointAssignments();
			this.timerDialogFrame.addSetPoint(new SetPoint(FramePoint.TOPRIGHT, anchor, FramePoint.BOTTOMRIGHT, 0,
					-GameUI.convertY(viewport, 0.004f + slot * 0.032f)));
			this.timerDialogFrame.positionBounds(rootFrame, viewport);
		}
	}

	public void remove(final GameUI rootFrame) {
		setVisible(false);
		rootFrame.remove(this.timerDialogFrame);
	}

	public void update(final GameUI rootFrame, final CSimulation simulation) {
		if (this.timerDialogFrame.isVisible() && (this.timer != null)) {
			final float remaining = this.timer.getRemaining(simulation) / this.speedMultFactor;
			final int secondsRemaining = Math.max(0, (int) remaining);
			if (secondsRemaining != this.displayedSeconds) {
				this.displayedSeconds = secondsRemaining;
				final int minutes = secondsRemaining / 60;
				final int seconds = secondsRemaining % 60;
				rootFrame.setText(this.valueFrame, minutes + ":" + (seconds < 10 ? "0" : "") + seconds);
			}
		}
	}

}

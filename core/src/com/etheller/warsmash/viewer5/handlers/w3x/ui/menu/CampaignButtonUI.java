package com.etheller.warsmash.viewer5.handlers.w3x.ui.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.AbstractUIFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.ClickableFrame;

public class CampaignButtonUI extends AbstractUIFrame implements ClickableFrame {

	private GlueButtonFrame buttonArt;
	private boolean enabled = true;
	private StringFrame headerText;
	private StringFrame nameText;
	private Color defaultNameColor;
	private Color defaultHeaderColor;
	private static final Color DISABLED_TEXT = new Color(0.5f, 0.5f, 0.5f, 1f);

	public CampaignButtonUI(final String name, final UIFrame parent) {
		super(name, parent);
	}

	public void setButtonArt(final GlueButtonFrame buttonArt) {
		this.buttonArt = buttonArt;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
		this.buttonArt.setEnabled(enabled);
		restoreTextColors();
	}

	public void setOnClick(final Runnable onClick) {
		this.buttonArt.setOnClick(() -> {
			if (this.enabled && isVisible()) onClick.run();
		});
	}

	@Override
	public UIFrame touchUp(final float screenX, final float screenY, final int button) {
		if (isVisible() && this.enabled && this.renderBounds.contains(screenX, screenY)) {
			return this;
		}
		return super.touchUp(screenX, screenY, button);
	}

	@Override
	public UIFrame touchDown(final float screenX, final float screenY, final int button) {
		if (isVisible() && this.enabled && this.renderBounds.contains(screenX, screenY)) {
			return this;
		}
		return super.touchDown(screenX, screenY, button);
	}

	@Override
	public UIFrame getFrameChildUnderMouse(final float screenX, final float screenY) {
		if (isVisible() && this.enabled && this.renderBounds.contains(screenX, screenY)) {
			final UIFrame childResult = this.buttonArt.getFrameChildUnderMouse(screenX, screenY);
			if (childResult != null) {
				return childResult;
			}
			return this;
		}
		return super.getFrameChildUnderMouse(screenX, screenY);
	}

	@Override
	public void mouseDown(final GameUI gameUI, final Viewport uiViewport) {
		this.buttonArt.mouseDown(gameUI, uiViewport);
	}

	@Override
	public void mouseUp(final GameUI gameUI, final Viewport uiViewport) {
		this.buttonArt.mouseUp(gameUI, uiViewport);
	}

	@Override
	public void mouseEnter(final GameUI gameUI, final Viewport uiViewport) {
		if (this.enabled) {
			this.headerText.setColor(Color.WHITE);
			this.nameText.setColor(Color.WHITE);
		}
	}

	@Override
	public void mouseExit(final GameUI gameUI, final Viewport uiViewport) {
		restoreTextColors();
	}

	@Override
	public void onClick(final int button) {
		if (this.enabled && isVisible()) this.buttonArt.onClick(button);
	}

	@Override
	public void mouseDragged(GameUI rootFrame, Viewport uiViewport, float x, float y) {

	}

	public void setHeaderText(final StringFrame headerText) {
		this.headerText = headerText;
		this.defaultHeaderColor = new Color(headerText.getColor());
		restoreTextColors();
	}

	public void setNameText(final StringFrame nameText) {
		this.nameText = nameText;
		this.defaultNameColor = new Color(nameText.getColor());
		restoreTextColors();
	}

	private void restoreTextColors() {
		if (this.headerText != null) this.headerText.setColor(this.enabled ? this.defaultHeaderColor : DISABLED_TEXT);
		if (this.nameText != null) this.nameText.setColor(this.enabled ? this.defaultNameColor : DISABLED_TEXT);
	}

	@Override
	public String getSoundKey() {
		return SOUND_KEY_GLUE_SCREEN_CLICK;
	}
}

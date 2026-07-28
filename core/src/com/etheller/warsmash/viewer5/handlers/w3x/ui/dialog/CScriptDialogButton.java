package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

public class CScriptDialogButton {
	private final GlueTextButtonFrame buttonFrame;
	private final StringFrame buttonText;
	private final List<Trigger> eventTriggers = new ArrayList<>();
	private char hotkey;
	private CScriptDialog ownerDialog;

	public CScriptDialogButton(final GlueTextButtonFrame buttonFrame, final StringFrame buttonText) {
		this(buttonFrame, buttonText, '\0');
	}

	public CScriptDialogButton(final GlueTextButtonFrame buttonFrame, final StringFrame buttonText,
			final char hotkey) {
		this.buttonFrame = buttonFrame;
		this.buttonText = buttonText;
		this.hotkey = Character.toUpperCase(hotkey);
	}

	public GlueTextButtonFrame getButtonFrame() {
		return this.buttonFrame;
	}

	public char getHotkey() {
		return this.hotkey;
	}

	public void setHotkey(final char hotkey) {
		this.hotkey = Character.toUpperCase(hotkey);
	}

	public void setText(final GameUI rootFrame, final String text) {
		rootFrame.setText(this.buttonText, text);
	}

	public void setupEvents(final CScriptDialog dialog) {
		this.ownerDialog = dialog;
		this.buttonFrame.setOnClick(this::click);
	}

	/** Fires button-click triggers and closes the owning dialog. */
	public void click() {
		if (this.ownerDialog == null) {
			return;
		}
		for (final Trigger trigger : this.eventTriggers) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope.triggerDialogScope(
					JassGameEventsWar3.EVENT_DIALOG_BUTTON_CLICK, trigger, this.ownerDialog, this);
			this.ownerDialog.getGlobalScope().queueTrigger(null, null, trigger, scope, scope);
		}
		this.ownerDialog.onButtonClick(this);
	}

	public RemovableTriggerEvent addEvent(final Trigger trigger) {
		this.eventTriggers.add(trigger);
		return new RemovableTriggerEvent(trigger) {
			@Override
			public void remove() {
				CScriptDialogButton.this.eventTriggers.remove(trigger);
			}
		};
	}
}

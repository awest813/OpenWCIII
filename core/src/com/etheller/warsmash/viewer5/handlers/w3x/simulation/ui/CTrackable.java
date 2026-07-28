package com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui;

import java.util.ArrayList;
import java.util.List;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

/**
 * JASS {@code trackable}: a clickable/hoverable world marker used by campaign
 * scripts (cinematic click-spots, etc.).
 */
public class CTrackable {
	private final String modelPath;
	private final float x;
	private final float y;
	private final float facing;
	private final List<Trigger> hitTriggers = new ArrayList<>();
	private final List<Trigger> trackTriggers = new ArrayList<>();
	private boolean trackedHover;

	public CTrackable(final String modelPath, final float x, final float y, final float facing) {
		this.modelPath = modelPath != null ? modelPath : "";
		this.x = x;
		this.y = y;
		this.facing = facing;
	}

	public String getModelPath() {
		return this.modelPath;
	}

	public float getX() {
		return this.x;
	}

	public float getY() {
		return this.y;
	}

	public float getFacing() {
		return this.facing;
	}

	public RemovableTriggerEvent addHitEvent(final Trigger trigger) {
		this.hitTriggers.add(trigger);
		return new RemovableTriggerEvent(trigger) {
			@Override
			public void remove() {
				CTrackable.this.hitTriggers.remove(trigger);
			}
		};
	}

	public RemovableTriggerEvent addTrackEvent(final Trigger trigger) {
		this.trackTriggers.add(trigger);
		return new RemovableTriggerEvent(trigger) {
			@Override
			public void remove() {
				CTrackable.this.trackTriggers.remove(trigger);
			}
		};
	}

	public void fireHit(final GlobalScope globalScope) {
		for (final Trigger trigger : this.hitTriggers) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope.trackableScope(
					JassGameEventsWar3.EVENT_GAME_TRACKABLE_HIT, trigger, this);
			globalScope.queueTrigger(null, null, trigger, scope, scope);
		}
	}

	public void fireTrack(final GlobalScope globalScope) {
		for (final Trigger trigger : this.trackTriggers) {
			final CommonTriggerExecutionScope scope = CommonTriggerExecutionScope.trackableScope(
					JassGameEventsWar3.EVENT_GAME_TRACKABLE_TRACK, trigger, this);
			globalScope.queueTrigger(null, null, trigger, scope, scope);
		}
	}

	public boolean isTrackedHover() {
		return this.trackedHover;
	}

	public void setTrackedHover(final boolean trackedHover) {
		this.trackedHover = trackedHover;
	}

	/** Hit/hover radius in world units. */
	public float getInteractionRadius() {
		return 96f;
	}
}

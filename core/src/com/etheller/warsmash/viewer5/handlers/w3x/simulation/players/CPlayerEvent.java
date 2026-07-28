package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

public class CPlayerEvent extends RemovableTriggerEvent {
	private final GlobalScope globalScope;
	private final CPlayerJass player;
	private final Trigger trigger;
	private final JassGameEventsWar3 eventType;
	private final TriggerBooleanExpression filter;
	private final String chatMatch;
	private final boolean chatExactMatch;

	public CPlayerEvent(final GlobalScope globalScope, final CPlayerJass player, final Trigger trigger,
			final JassGameEventsWar3 eventType, final TriggerBooleanExpression filter) {
		this(globalScope, player, trigger, eventType, filter, null, false);
	}

	public CPlayerEvent(final GlobalScope globalScope, final CPlayerJass player, final Trigger trigger,
			final JassGameEventsWar3 eventType, final TriggerBooleanExpression filter, final String chatMatch,
			final boolean chatExactMatch) {
		super(trigger);
		this.globalScope = globalScope;
		this.player = player;
		this.trigger = trigger;
		this.eventType = eventType;
		this.filter = filter;
		this.chatMatch = chatMatch;
		this.chatExactMatch = chatExactMatch;
	}

	public Trigger getTrigger() {
		return this.trigger;
	}

	public JassGameEventsWar3 getEventType() {
		return this.eventType;
	}

	public String getChatMatch() {
		return this.chatMatch;
	}

	public boolean isChatExactMatch() {
		return this.chatExactMatch;
	}

	public boolean matchesChat(final String message) {
		return matchesChatMessage(message, this.chatMatch, this.chatExactMatch);
	}

	static boolean matchesChatMessage(final String message, final String chatMatch, final boolean chatExactMatch) {
		if (message == null) {
			return false;
		}
		final String match = chatMatch != null ? chatMatch : "";
		if (match.isEmpty()) {
			return true;
		}
		if (chatExactMatch) {
			return message.equalsIgnoreCase(match);
		}
		return message.toLowerCase().contains(match.toLowerCase());
	}

	@Override
	public void remove() {
		this.player.removeEvent(this);
	}

	public void fire(final CUnit hero, final TriggerExecutionScope scope) {
		this.globalScope.queueTrigger(this.filter, CommonTriggerExecutionScope.filterScope(scope, hero), this.trigger,
				scope, scope);
	}

	public void fire(final CPlayer player, final TriggerExecutionScope scope) {
		this.globalScope.queueTrigger(this.filter, CommonTriggerExecutionScope.filterScope(scope, player), this.trigger,
				scope, scope);
	}
}

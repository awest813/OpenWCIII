package com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger;

import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.interpreter.ast.scope.variableevent.VariableEvent;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;

/**
 * A TriggerRegisterPlayerStateEvent registration: watches one player state and
 * fires when the state's value starts satisfying the limit comparison.
 *
 * <p>
 * Retail fires on the transition into a matching value, not on every tick
 * while the value stays matching, so this event remembers the previous
 * evaluation and fires on the rising edge only.
 */
public class CPlayerStateEvent extends VariableEvent {
	private final GlobalScope globalScope;
	private final CPlayer player;
	private final CPlayerState state;
	private boolean wasMatching = false;

	public CPlayerStateEvent(final GlobalScope globalScope, final Trigger trigger, final CPlayer player,
			final CPlayerState state, final CLimitOp limitOp, final double limitval) {
		super(trigger, limitOp, limitval);
		this.globalScope = globalScope;
		this.player = player;
		this.state = state;
	}

	public void update(final CSimulation simulation) {
		if (this.player == null) {
			return;
		}
		final boolean matching = isMatching(this.player.getPlayerState(simulation, this.state));
		if (matching && !this.wasMatching) {
			fire();
		}
		this.wasMatching = matching;
	}

	/**
	 * Records the current evaluation without firing, so a registration made
	 * while the state already satisfies the limit does not fire spuriously on
	 * its first tick. Retail fires on state changes, not on registration.
	 */
	public void captureInitial(final CSimulation simulation) {
		if (this.player != null) {
			this.wasMatching = isMatching(this.player.getPlayerState(simulation, this.state));
		}
	}

	private void fire() {
		final CommonTriggerExecutionScope eventScope = CommonTriggerExecutionScope.playerStateScope(
				JassGameEventsWar3.EVENT_PLAYER_STATE_LIMIT, getTrigger(), this.player, this.state);
		this.globalScope.queueTrigger(null, null, getTrigger(), eventScope, eventScope);
	}
}

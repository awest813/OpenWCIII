package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionReplayLog;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item.shop.CAbilityNeutralBuilding;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorHoldPosition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderDropItemAtPoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderDropItemAtTargetWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderNoTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetPoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.COrderTargetWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;

public class CPlayerUnitOrderExecutor implements CPlayerUnitOrderListener {
	private final CSimulation game;
	private final int playerIndex;

	public CPlayerUnitOrderExecutor(final CSimulation game, final int playerIndex) {
		this.game = game;
		this.playerIndex = playerIndex;
	}

	private void record(final int kind, final int b, final int c, final int d, final int e, final float x, final float y, final boolean queue, final String text) {
		this.game.recordMissionInput(kind, this.playerIndex, b, c, d, e, x, y, queue, text);
	}

	private boolean sharedControl(final CUnit unit) {
		boolean controlShared = this.game.getPlayer(unit.getPlayerIndex()).hasAlliance(this.playerIndex,
				CAllianceType.SHARED_CONTROL);
		if (!controlShared) {
			final CAbilityNeutralBuilding neutralBuildingData = unit.getNeutralBuildingData();
			if (neutralBuildingData != null) {
				final CUnit selectedPlayerUnit = neutralBuildingData.getSelectedPlayerUnit(this.playerIndex);
				if (selectedPlayerUnit != null) {
					controlShared = true;
				}
			}
		}
		return controlShared;
	}

	@Override
	public void issueTargetOrder(final int unitHandleId, final int abilityHandleId, final int orderId,
			final int targetHandleId, final boolean queue) {
		record(MissionReplayLog.TARGET, unitHandleId, abilityHandleId, orderId, targetHandleId, 0, 0, queue, "");
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			unit.order(this.game, new COrderTargetWidget(abilityHandleId, orderId, targetHandleId, queue), queue);
		}
	}

	@Override
	public void issueDropItemAtPointOrder(final int unitHandleId, final int abilityHandleId, final int orderId,
			final int targetHandleId, final float x, final float y, final boolean queue) {
		record(MissionReplayLog.DROP_POINT, unitHandleId, abilityHandleId, orderId, targetHandleId, x, y, queue, "");
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			unit.order(this.game, new COrderDropItemAtPoint(abilityHandleId, orderId, targetHandleId,
					new AbilityPointTarget(x, y), queue), queue);
		}
	}

	@Override
	public void issueDropItemAtTargetOrder(final int unitHandleId, final int abilityHandleId, final int orderId,
			final int targetItemHandleId, final int targetHeroHandleId, final boolean queue) {
		record(MissionReplayLog.DROP_TARGET, unitHandleId, abilityHandleId, orderId, targetItemHandleId, 0, 0, queue, Integer.toString(targetHeroHandleId));
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			unit.order(this.game, new COrderDropItemAtTargetWidget(abilityHandleId, orderId, targetItemHandleId,
					targetHeroHandleId, queue), queue);
		}
	}

	@Override
	public void issuePointOrder(final int unitHandleId, final int abilityHandleId, final int orderId, final float x,
			final float y, final boolean queue) {
		record(MissionReplayLog.POINT, unitHandleId, abilityHandleId, orderId, 0, x, y, queue, "");
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			unit.order(this.game, new COrderTargetPoint(abilityHandleId, orderId, new AbilityPointTarget(x, y), queue),
					queue);
		}
	}

	@Override
	public void issueImmediateOrder(final int unitHandleId, final int abilityHandleId, final int orderId,
			final boolean queue) {
		record(MissionReplayLog.IMMEDIATE, unitHandleId, abilityHandleId, orderId, 0, 0, 0, queue, "");
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			if (abilityHandleId == 0) {
				if (orderId == OrderIds.stop) {
					unit.order(this.game, null, queue);
				}
				else if (orderId == OrderIds.holdposition) {
					unit.order(this.game, null, queue);
					final CBehaviorHoldPosition holdPositionBehavior = unit.getHoldPositionBehavior();
					if (holdPositionBehavior != null) {
						unit.setDefaultBehavior(holdPositionBehavior);
					}
				}
			}
			else {
				unit.order(this.game, new COrderNoTarget(abilityHandleId, orderId, queue), queue);
			}
		}
	}

	@Override
	public void unitCancelTrainingItem(final int unitHandleId, final int cancelIndex) {
		record(MissionReplayLog.CANCEL_TRAINING, unitHandleId, cancelIndex, 0, 0, 0, 0, false, "");
		final CUnit unit = this.game.getUnit(unitHandleId);
		if (unit == null) {
			return;
		}
		if ((this.playerIndex == unit.getPlayerIndex()) || sharedControl(unit)) {
			unit.cancelBuildQueueItem(this.game, cancelIndex);
		}
	}

	@Override
	public void issueGuiPlayerEvent(final int eventId) {
		record(MissionReplayLog.GUI_EVENT, eventId, 0, 0, 0, 0, 0, false, "");
		final CPlayer player = this.game.getPlayer(playerIndex);
		final JassGameEventsWar3 eventType = JassGameEventsWar3.getByEventId(eventId);
		if (eventType != null) {
			player.firePlayerEvents(CommonTriggerExecutionScope::guiPlayerEventTriggerScope, eventType);
		}
	}

}

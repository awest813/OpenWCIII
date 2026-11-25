package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import java.util.EnumSet;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.ResourceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponent;

/** Dismantles an owned Undead structure and returns resources as it loses life. */
public final class CAbilityUnsummon extends CAbilityTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Buns");
	private float salvageRatio;
	private int lifePerSecond;
	private War3ID buffId;

	public CAbilityUnsummon(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.unsummon;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.salvageRatio = Math.max(0, data.getFieldAsFloat(AbilityFields.DATA_A + level, 0));
		this.lifePerSecond = Math.max(1, data.getFieldAsInteger(AbilityFields.DATA_B + level, 0));
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		if (War3ID.NONE.equals(this.buffId)) this.buffId = DEFAULT_BUFF;
		if (getTargetsAllowed().isEmpty()) {
			final var targets = CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED));
			setTargetsAllowed(targets.isEmpty() ? EnumSet.of(CTargetType.STRUCTURE, CTargetType.PLAYERUNITS) : targets);
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit building) || !building.isBuilding()
				|| building.getUnitType().getRace() != CUnitRace.UNDEAD) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_AN_UNDEAD_BUILDING);
		}
		else if (building.isUnsummoning()) {
			receiver.targetCheckFailed(CommandStringErrorKeys.TARGET_IS_BEING_UNSUMMONED);
		}
		else if (building.isConstructing() && !building.isUpgrading()) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_BUILDING_IS_CURRENTLY_UNDER_CONSTRUCTION);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (target instanceof CUnit building && !building.isDead() && !building.isHidden()
				&& building.getPlayerIndex() == caster.getPlayerIndex()) {
			startUnsummoning(game, building);
		}
		return false;
	}

	private void startUnsummoning(final CSimulation game, final CUnit building) {
		if (isDisabled() || building.isUnsummoning() || building.isDead() || building.isInvulnerable()
				|| building.isConstructing() && !building.isUpgrading()) return;
		building.setUnsummoning(true);
		final SimulationRenderComponent art = game.createPersistentSpellEffectOnUnit(building, this.buffId, CEffectType.TARGET);
		final var demolition = new Demolition(building, this.lifePerSecond, this.salvageRatio, art);
		demolition.setTimeoutTime(1);
		demolition.onFire(game);
	}

	private static final class Demolition extends CTimer {
		private final CUnit building;
		private final int playerIndex;
		private final int lifePerSecond;
		private final double goldPerLife;
		private final double lumberPerLife;
		private final SimulationRenderComponent art;
		private double removedLife;
		private int paidGold;
		private int paidLumber;

		private Demolition(final CUnit building, final int lifePerSecond, final float salvageRatio,
				final SimulationRenderComponent art) {
			this.building = building;
			this.playerIndex = building.getPlayerIndex();
			this.lifePerSecond = lifePerSecond;
			this.goldPerLife = (double) salvageRatio * building.getUnitType().getGoldCost() / building.getMaximumLife();
			this.lumberPerLife = (double) salvageRatio * building.getUnitType().getLumberCost() / building.getMaximumLife();
			this.art = art;
		}

		@Override
		public void onFire(final CSimulation game) {
			if (this.building.isDead() || this.building.getPlayerIndex() != this.playerIndex
					|| game.getUnit(this.building.getHandleId()) != this.building) {
				finish();
				return;
			}
			final float lostLife = Math.min(this.lifePerSecond, this.building.getLife());
			this.removedLife += lostLife;
			final int gold = (int) Math.floor(this.removedLife * this.goldPerLife + 0.000001);
			final int lumber = (int) Math.floor(this.removedLife * this.lumberPerLife + 0.000001);
			final var owner = game.getPlayer(this.playerIndex);
			if (gold > this.paidGold) {
				owner.addGold(gold - this.paidGold);
				game.unitGainResourceEvent(this.building, this.playerIndex, ResourceType.GOLD, gold - this.paidGold);
				this.paidGold = gold;
			}
			if (lumber > this.paidLumber) {
				owner.addLumber(lumber - this.paidLumber);
				game.unitGainResourceEvent(this.building, this.playerIndex, ResourceType.LUMBER, lumber - this.paidLumber);
				this.paidLumber = lumber;
			}
			this.building.setLife(game, this.building.getLife() - lostLife);
			if (this.building.isDead()) finish();
			else start(game);
		}

		private void finish() {
			this.building.setUnsummoning(false);
			this.art.remove();
		}
	}
}

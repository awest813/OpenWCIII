package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehavior;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.skills.CBehaviorTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityGetCorpse extends CAbilityTargetSpellBase implements CAutocastAbility {
	private float searchRadius;
	private boolean autocast;

	public CAbilityGetCorpse(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.loadcorpse; }
	@Override public int getAutoCastOnOrderId() { return OrderIds.loadcorpseon; }
	@Override public int getAutoCastOffOrderId() { return OrderIds.loadcorpseoff; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }
	@Override public AutocastType getAutocastType() { return AutocastType.NOTARGET; }
	@Override public boolean isUniversal() { return true; }

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.searchRadius = definition.getFieldAsFloat(AbilityFields.AREA + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void setAutoCastOn(final CUnit wagon, final boolean enabled) {
		if (this.autocast == enabled) return;
		this.autocast = enabled;
		wagon.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit wagon) {
		setAutoCastOn(wagon, false);
	}

	@Override
	protected CBehaviorTargetSpellBase createBehavior(final CUnit wagon) {
		return new CBehaviorTargetSpellBase(wagon, this) {
			@Override
			protected boolean checkTargetStillValid(final CSimulation game) {
				return this.target instanceof CUnit corpse && game.containsUnit(corpse)
						&& wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo && cargo.canStoreCorpse(corpse);
			}
		};
	}

	private CUnit nearbyCorpse(final CSimulation game, final CUnit wagon) {
		if (!(wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo) || !cargo.hasCapacity(1)) return null;
		final CUnit[] closest = { null };
		game.getWorldCollision().enumCorpsesInRange(wagon.getX(), wagon.getY(), this.searchRadius, corpse -> {
			if (cargo.canStoreCorpse(corpse) && (closest[0] == null
					|| wagon.distanceSquaredNoCollision(corpse) < wagon.distanceSquaredNoCollision(closest[0]))) closest[0] = corpse;
			return false;
		});
		return closest[0];
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit wagon, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		if (nearbyCorpse(game, wagon) != null) receiver.targetOk(null);
		else receiver.targetCheckFailed(CommandStringErrorKeys.THERE_ARE_NO_USABLE_CORPSES_NEARBY);
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit wagon, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo && cargo.canStoreCorpse(unit)) {
			receiver.targetOk(target);
		}
		else receiver.targetCheckFailed(CommandStringErrorKeys.THERE_ARE_NO_USABLE_CORPSES_NEARBY);
	}

	@Override
	public CBehavior beginNoTarget(final CSimulation game, final CUnit wagon, final int orderId) {
		final CUnit corpse = nearbyCorpse(game, wagon);
		return corpse == null ? wagon.pollNextOrderBehavior(game) : begin(game, wagon, orderId, corpse);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit wagon, final AbilityTarget target) {
		if (target instanceof CUnit corpse && wagon.getCargoData() instanceof CAbilityMeatWagonCargo cargo) {
			cargo.storeCorpse(game, wagon, corpse);
		}
		return false;
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit wagon, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit wagon, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit wagon, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		innerCheckCanTargetNoTarget(game, wagon, orderId, receiver);
	}
}

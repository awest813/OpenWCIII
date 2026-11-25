package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityUnitOrPointTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemSoulTrap extends CAbilityUnitOrPointTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIso");
	private CItem item;

	public CAbilityItemSoulTrap(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemsoultrap;
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.item = item;
	}

	@Override
	public boolean consumesItemChargeOnEffect() {
		return true;
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit victim) || victim == caster || victim.isHidden() || victim.isInvulnerable()
				|| alreadyCarriesSoul(game, caster)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
			return;
		}
		super.innerCheckCanTarget(game, caster, orderId, target, receiver);
	}

	@Override
	protected void innerCheckCanTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit victim = (CUnit) target;
		final var inventory = caster.getInventoryData();
		if (victim.isHidden() || victim.isInvulnerable() || alreadyCarriesSoul(game, caster)
				|| this.item != null && (inventory == null || inventory.getSlot(this.item) < 0 || this.item.getCharges() <= 0)) {
			return false;
		}
		new CAbilitySoulPossession(game.getHandleIdAllocator().createId(), CAbilitySoulPossession.CODE)
				.capture(game, victim, caster);
		if (this.item != null) inventory.consumeItemCharge(game, caster, this.item);
		return false;
	}

	private static boolean alreadyCarriesSoul(final CSimulation game, final CUnit caster) {
		for (final CUnit unit : game.getUnitsIncludingPending()) {
			final var possession = unit.getFirstAbilityOfType(CAbilitySoulPossession.class);
			if (possession != null && possession.isCarriedBy(caster)) return true;
		}
		return false;
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold.CAbilityDevourCargo;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityDevour extends CAbilityTargetSpellBase {
	private static final War3ID CREEP_DEVOUR = War3ID.fromString("ACdv");
	private int maxCreepLevel;
	private float damagePerSecond;
	private int baseOrder = OrderIds.devour;

	public CAbilityDevour(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return this.baseOrder;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.maxCreepLevel = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.damagePerSecond = definition.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.baseOrder = CREEP_DEVOUR.equals(getCode())
				? OrderIds.creepdevour : OrderIds.devour;
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityActivationReceiver receiver) {
		final CAbilityDevourCargo cargo = caster.getFirstAbilityOfType(CAbilityDevourCargo.class);
		if (cargo == null || !cargo.isEmpty()) receiver.activationCheckFailed(CommandStringErrorKeys.CARGO_CAPACITY_UNAVAILABLE);
		else receiver.useOk();
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CUnit victim) || !isValidVictim(victim)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
		}
		else if (tooPowerful(victim)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_CREATURE_IS_TOO_POWERFUL);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CAbilityDevourCargo cargo = caster.getFirstAbilityOfType(CAbilityDevourCargo.class);
		if (cargo != null && cargo.isEmpty() && target instanceof CUnit victim && isValidVictim(victim) && !tooPowerful(victim)) {
			cargo.swallow(game, caster, victim, this.damagePerSecond);
		}
		return false;
	}

	private boolean isValidVictim(final CUnit victim) {
		return !victim.isDead() && !victim.isHidden() && !victim.isBuilding() && !victim.isInvulnerable()
				&& victim.getHeroData() == null && victim.getFirstAbilityOfType(CAbilityDevourCargo.class) == null;
	}

	private boolean tooPowerful(final CUnit victim) {
		return this.maxCreepLevel > 0 && victim.getPlayerIndex() == WarsmashConstants.MAX_PLAYERS - 4
				&& victim.getUnitType().getLevel() > this.maxCreepLevel;
	}
}

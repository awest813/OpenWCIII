package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.AutocastType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.autocast.CAutocastAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityEntanglingProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;

public final class CAbilityWeb extends CAbilityEntanglingProjectile implements CAutocastAbility {
	private final int baseOrderId;
	private boolean autocast;

	public CAbilityWeb(final int handleId, final War3ID alias) {
		this(handleId, alias, OrderIds.web);
	}

	public CAbilityWeb(final int handleId, final War3ID alias, final int baseOrderId) {
		super(handleId, alias, alias, alias, StateModBuffType.WEBBED);
		this.baseOrderId = baseOrderId;
	}

	@Override
	protected String getFallbackArt() {
		return "Abilities\\Spells\\Undead\\Web\\Web_AirTarget.mdl";
	}

	@Override
	protected void populateDefaultTargets(final GameObject data) {
		final var targets = CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED));
		if (targets.isEmpty()) {
			targets.add(CTargetType.AIR);
			targets.add(CTargetType.ENEMIES);
		}
		targets.add(CTargetType.NON_ETHEREAL);
		setTargetsAllowed(targets);
	}

	@Override public int getBaseOrderId() { return this.baseOrderId; }
	@Override public int getAutoCastOnOrderId() { return this.baseOrderId == OrderIds.web ? OrderIds.webon : 0; }
	@Override public int getAutoCastOffOrderId() { return this.baseOrderId == OrderIds.web ? OrderIds.weboff : 0; }
	@Override public boolean isAutoCastOn() { return this.autocast; }
	@Override public void setAutoCastOff() { this.autocast = false; }
	@Override public AutocastType getAutocastType() { return AutocastType.NEARESTENEMY; }

	@Override
	public void setAutoCastOn(final CUnit caster, final boolean enabled) {
		if (this.baseOrderId != OrderIds.web || this.autocast == enabled) return;
		this.autocast = enabled;
		caster.setAutocastAbility(enabled ? this : null);
	}

	@Override
	public void onRemove(final CSimulation game, final CUnit caster) {
		setAutoCastOn(caster, false);
		super.onRemove(game, caster);
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId, final CWidget target,
			final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && !unit.isWebbed() && caster.canReach(unit, getCastRange())) {
			checkCanTarget(game, caster, orderId, target, receiver);
		}
		else receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityPointTarget target, final AbilityTargetCheckReceiver<AbilityPointTarget> receiver) {
		receiver.orderIdNotAccepted();
	}

	@Override
	public void checkCanAutoTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		receiver.orderIdNotAccepted();
	}
}

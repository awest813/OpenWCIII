package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import java.util.ArrayList;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilityItemRestore extends CAbilityNoTargetSpellBase {
	private final int orderId;
	private float life;
	private float mana;
	private float radius;
	private boolean powerup;

	public CAbilityItemRestore(final int handleId, final War3ID alias, final int orderId) {
		super(handleId, alias);
		this.orderId = orderId;
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		final float primary = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.life = this.orderId == OrderIds.itemmanarestoreaoe ? 0 : primary;
		this.mana = this.orderId == OrderIds.itemmanarestoreaoe ? primary
				: data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.powerup = item != null && item.getItemType().isUseAutomaticallyWhenAcquired();
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		if (this.powerup && orderId == this.orderId) {
			// The inventory removes the temporary ability in the same pickup event.
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (this.orderId == OrderIds.itemrestore && unit.getLife() >= unit.getMaximumLife()
				&& unit.getMana() >= unit.getMaximumMana()) {
			receiver.activationCheckFailed(CommandStringErrorKeys.UNABLE_TO_USE_THIS_ITEM);
			return;
		}
		receiver.useOk();
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (this.orderId == OrderIds.itemrestore) {
			restore(game, caster);
			return false;
		}
		final var recipients = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
			if (!unit.isDead() && !unit.isHidden() && unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				recipients.add(unit);
			}
			return false;
		});
		for (final CUnit unit : recipients) {
			if (!unit.isDead() && game.containsUnit(unit)) {
				restore(game, unit);
			}
		}
		return false;
	}

	private void restore(final CSimulation game, final CUnit unit) {
		if (this.life > 0) {
			unit.heal(game, this.life);
		}
		if (this.mana > 0 && unit.getMaximumMana() > 0) {
			unit.restoreMana(game, this.mana);
		}
		game.createTemporarySpellEffectOnUnit(unit, getAlias(),
				this.orderId == OrderIds.itemrestore ? CEffectType.CASTER : CEffectType.TARGET);
	}
}

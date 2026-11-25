package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityItemSpeed extends CAbilityNoTargetSpellBase {
	public static final War3ID POTION = War3ID.fromString("AIsp");
	public static final War3ID AREA_SPEED = War3ID.fromString("AIsa");
	private float bonus;
	private float radius;
	private War3ID buff;
	private boolean powerup;

	public CAbilityItemSpeed(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemspeed;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.bonus = definition.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.radius = definition.getFieldAsFloat(AbilityFields.AREA + level, 0);
		final String buffId = definition.getField(AbilityFields.BUFF + level, 0);
		this.buff = buffId != null && buffId.length() == 4 ? War3ID.fromString(buffId) : getAlias();
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public void setItemAbility(final CItem item, final int slot) {
		this.powerup = item != null && item.getItemType().isUseAutomaticallyWhenAcquired();
	}

	@Override
	public boolean checkBeforeQueue(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTarget target) {
		if (this.powerup && orderId == getBaseOrderId()) {
			doEffect(game, caster, target);
			return false;
		}
		return super.checkBeforeQueue(game, caster, orderId, target);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (POTION.equals(getCode())) {
			applySpeed(game, caster);
		}
		else {
			final var receiver = new BooleanAbilityTargetCheckReceiver<CWidget>();
			game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
				if (!unit.isDead() && !unit.isHidden() && unit.getSpeed() > 0
						&& unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(), receiver.reset())) {
					applySpeed(game, unit);
				}
				return false;
			});
		}
		return false;
	}

	private void applySpeed(final CSimulation game, final CUnit unit) {
		final NonStackingStatBuff modifier = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "ItemSpeed", this.bonus);
		unit.add(game, new CBuffTimed(game.getHandleIdAllocator().createId(), getCode(), this.buff, getDurationForTarget(unit)) {
			@Override
			protected void onBuffAdd(final CSimulation game, final CUnit recipient) {
				recipient.addNonStackingStatBuff(modifier);
			}

			@Override
			protected void onBuffRemove(final CSimulation game, final CUnit recipient) {
				recipient.removeNonStackingStatBuff(modifier);
			}

			@Override
			public boolean isDispellable() {
				return true;
			}

			@Override
			public DispelPolarity getDispelPolarity() {
				return DispelPolarity.BENEFICIAL;
			}

			@Override
			public boolean isTimedLifeBar() {
				return false;
			}
		});
	}
}

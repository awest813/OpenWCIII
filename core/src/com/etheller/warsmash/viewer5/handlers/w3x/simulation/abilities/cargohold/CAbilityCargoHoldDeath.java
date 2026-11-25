package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.cargohold;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityPassiveSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityCargoHoldDeath extends CAbilityPassiveSpellBase {
	public static final War3ID CODE = War3ID.fromString("Achd");
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("Bchd");
	private War3ID dizzyBuff;
	private float movementSpeedModifier;
	private float attackSpeedModifier;

	public CAbilityCargoHoldDeath(final int handleId, final War3ID alias) {
		super(handleId, CODE, alias);
		setIconShowing(false);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.movementSpeedModifier = 1 / Math.max(1, data.getFieldAsFloat(AbilityFields.DATA_A + level, 0)) - 1;
		this.attackSpeedModifier = 1 / Math.max(1, data.getFieldAsFloat(AbilityFields.DATA_B + level, 0)) - 1;
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		this.dizzyBuff = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	public void onPassengerReleased(final CSimulation game, final CUnit carrier, final CUnit passenger) {
		final float duration = getDurationForTarget(passenger);
		if (isDisabled() || duration <= 0 || passenger.isDead() || passenger.getTransport() != null
				|| !passenger.canBeTargetedBy(game, carrier, false, getTargetsAllowed(),
						new BooleanAbilityTargetCheckReceiver<CWidget>())) return;
		final Dizzy previous = passenger.getFirstAbilityOfType(Dizzy.class);
		if (previous != null) passenger.remove(game, previous);
		passenger.add(game, new Dizzy(game.getHandleIdAllocator().createId(), this.dizzyBuff, duration,
				this.movementSpeedModifier, this.attackSpeedModifier));
	}

	private static final class Dizzy extends CBuffTimed {
		private final NonStackingStatBuff movement;
		private final NonStackingStatBuff attack;

		private Dizzy(final int handleId, final War3ID alias, final float duration, final float movement, final float attack) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.movement = new NonStackingStatBuff(NonStackingStatBuffType.MVSPDPCT, "cargoDizzy", movement);
			this.attack = new NonStackingStatBuff(NonStackingStatBuffType.ATKSPD, "cargoDizzy", attack);
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.movement);
			unit.addNonStackingStatBuff(this.attack);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.movement);
			unit.removeNonStackingStatBuff(this.attack);
		}

		@Override public boolean isTimedLifeBar() { return false; }
	}
}

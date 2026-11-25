package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.HashSet;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

public final class CAbilitySoulPreservation extends CAbilityTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("ANsl");
	private static final War3ID DEFAULT_ZOMBIE = War3ID.fromString("nzom");
	private final Set<CUnit> pending = new HashSet<>();
	private War3ID zombieType;

	public CAbilitySoulPreservation(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.soulpreservation; }

	@Override
	public void populateData(final GameObject data, final int level) {
		final String unit = data.getFieldAsString(AbilityFields.UNIT_ID + level, 0);
		this.zombieType = unit.length() == 4 ? War3ID.fromString(unit) : DEFAULT_ZOMBIE;
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	private boolean canPreserve(final CUnit target) {
		return this.zombieType.equals(target.getTypeId()) && !target.isDead() && !target.isHidden() && !this.pending.contains(target);
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && canPreserve(unit)) receiver.targetOk(unit);
		else receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_UNIT_WITH_THIS_ACTION);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CUnit zombie) || !canPreserve(zombie) || !zombie.checkForAbilityEffectReaction(game, caster, this)) return false;
		this.pending.add(zombie);
		game.createTemporarySpellEffectOnUnit(zombie, getAlias(), CEffectType.TARGET);
		final CTimer transfer = new CTimer() {
			@Override
			public void onFire(final CSimulation simulation) {
				pending.remove(zombie);
				if (!zombie.isDead() && simulation.containsUnit(zombie)) simulation.removeUnit(zombie);
			}
		};
		transfer.setTimeoutTime(getDurationForTarget(zombie));
		transfer.start(game);
		return false;
	}
}

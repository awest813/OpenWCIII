package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.undead.deathknight;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/** Raises nearby corpses as temporary units controlled by the caster. */
public final class CAbilityAnimateDead extends CAbilityNoTargetSpellBase {
	private final int orderId;
	private int corpseCount;
	private float area;
	private boolean invulnerable;

	public CAbilityAnimateDead(final int handleId, final War3ID alias, final int orderId) {
		super(handleId, alias);
		this.orderId = orderId;
	}

	@Override
	public int getBaseOrderId() {
		return this.orderId;
	}

	@Override
	public void populateData(final GameObject ability, final int level) {
		this.corpseCount = ability.getFieldAsInteger("DataA" + level, 0);
		this.area = ability.getFieldAsFloat("Area" + level, 0);
		this.invulnerable = ability.getFieldAsInteger("DataB" + level, 0) != 0;
		if (!getTargetsAllowed().contains(CTargetType.DEAD)) {
			final var targets = CTargetType.parseTargetTypeSet(ability.getFieldAsList("targs"));
			if (!targets.isEmpty()) setTargetsAllowed(targets);
		}
	}

	@Override
	protected void innerCheckCanTargetNoTarget(final CSimulation game, final CUnit caster, final int orderId,
			final AbilityTargetCheckReceiver<Void> receiver) {
		final boolean[] found = { false };
		game.getWorldCollision().enumCorpsesInRange(caster.getX(), caster.getY(), this.area, unit -> {
			found[0] = canRaise(game, caster, unit);
			return found[0];
		});
		if (this.corpseCount > 0 && found[0]) receiver.targetOk(null);
		else receiver.targetCheckFailed(CommandStringErrorKeys.THERE_ARE_NO_USABLE_CORPSES_NEARBY);
	}

	private boolean canRaise(final CSimulation game, final CUnit caster, final CUnit corpse) {
		return !corpse.isHidden() && corpse.isRaisable() && corpse.isDecays() && !corpse.isHero() && !corpse.isBuilding()
				&& corpse.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
						BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset());
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final Set<CUnit> corpses = new LinkedHashSet<>();
		game.getWorldCollision().enumCorpsesInRange(caster.getX(), caster.getY(), this.area, corpse -> {
			if (canRaise(game, caster, corpse)) corpses.add(corpse);
			return false;
		});
		final var strongest = Comparator.<CUnit>comparingInt(unit -> unit.getUnitType().getLevel()).reversed()
				.thenComparingDouble(caster::distanceSquaredNoCollision).thenComparingInt(CUnit::getHandleId);
		for (final CUnit corpse : corpses.stream().sorted(strongest).limit(this.corpseCount).toList()) {
			final CUnit raised = game.createUnit(corpse.getTypeId(), caster.getPlayerIndex(), corpse.getX(), corpse.getY(),
					corpse.getFacing());
			if (raised == null) continue;
			game.removeUnit(corpse);
			game.getPlayer(caster.getPlayerIndex()).addTechtreeUnlocked(game, raised.getTypeId());
			raised.addClassification(CUnitClassification.SUMMONED);
			raised.setRaisable(false);
			raised.setDecays(false);
			raised.setInvulnerable(this.invulnerable);
			raised.add(game, new CBuffTimedLife(game.getHandleIdAllocator().createId(), getAlias(), getDuration(), true));
			raised.setPointAndCheckUnstuck(raised.getX(), raised.getY(), game);
			game.createTemporarySpellEffectOnUnit(raised, getAlias(), CEffectType.SPECIAL);
		}
		return false;
	}
}

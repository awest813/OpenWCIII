package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.neutral;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.NonStackingStatBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;

public final class CAbilityHowlOfTerror extends CAbilityNoTargetSpellBase {
	private static final War3ID DEFAULT_BUFF = War3ID.fromString("BNht");
	private War3ID buffId;
	private float radius;
	private float damageReduction;
	private float armorReduction;
	private float regenerationReduction;

	public CAbilityHowlOfTerror(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override public int getBaseOrderId() { return OrderIds.howlofterror; }

	@Override
	public void populateData(final GameObject ability, final int level) {
		final War3ID buff = AbstractCAbilityTypeDefinition.getBuffId(ability, level);
		this.buffId = War3ID.NONE.equals(buff) ? DEFAULT_BUFF : buff;
		this.radius = ability.getFieldAsFloat(AbilityFields.AREA + level, 0);
		this.damageReduction = ability.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.armorReduction = ability.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.regenerationReduction = ability.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		if (CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(ability.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final List<CUnit> affected = new ArrayList<>();
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
			if (unit != caster && !unit.isDead() && unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) affected.add(unit);
			return false;
		});
		for (final CUnit unit : affected) {
			if (!unit.checkForAbilityEffectReaction(game, caster, this)) continue;
			final HowlDebuff previous = unit.getFirstAbilityOfType(HowlDebuff.class);
			if (previous != null) unit.remove(game, previous);
			unit.add(game, new HowlDebuff(game.getHandleIdAllocator().createId(), this.buffId, getDurationForTarget(unit),
					this.damageReduction, this.armorReduction, this.regenerationReduction));
		}
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}

	private static final class HowlDebuff extends CBuffTimed {
		private final List<NonStackingStatBuff> reductions;

		private HowlDebuff(final int handleId, final War3ID alias, final float duration, final float damage,
				final float armor, final float regeneration) {
			super(handleId, DEFAULT_BUFF, alias, duration);
			this.reductions = List.of(new NonStackingStatBuff(NonStackingStatBuffType.ALLATKPCT, "howl of terror", -damage),
					new NonStackingStatBuff(NonStackingStatBuffType.DEF, "howl of terror", -armor),
					new NonStackingStatBuff(NonStackingStatBuffType.HPGEN, "howl of terror", -regeneration));
		}

		@Override protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			this.reductions.forEach(unit::addNonStackingStatBuff);
		}

		@Override protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			this.reductions.forEach(unit::removeNonStackingStatBuff);
		}

		@Override public boolean isDispellable() { return true; }
		@Override public DispelPolarity getDispelPolarity() { return DispelPolarity.HARMFUL; }
		@Override public boolean isHeroSpellBuff() { return true; }
		@Override public boolean isTimedLifeBar() { return false; }
	}
}

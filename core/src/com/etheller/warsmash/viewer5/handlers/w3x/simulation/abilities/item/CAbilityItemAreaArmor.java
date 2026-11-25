package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.item;

import java.util.ArrayList;

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

public final class CAbilityItemAreaArmor extends CAbilityNoTargetSpellBase {
	public static final War3ID CODE = War3ID.fromString("AIda");
	private float armor;
	private float radius;
	private War3ID buffId;

	public CAbilityItemAreaArmor(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.itemdefenseaoe;
	}

	@Override
	public boolean isUniversal() {
		return true;
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.armor = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.radius = data.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(data, level);
		if (War3ID.NONE.equals(this.buffId)) {
			this.buffId = getAlias();
		}
		if (getTargetsAllowed().isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final var recipients = new ArrayList<CUnit>();
		game.getWorldCollision().enumUnitsInRange(caster.getX(), caster.getY(), this.radius, unit -> {
			if (!unit.isHidden() && !unit.isDead() && unit.canBeTargetedBy(game, caster, false, getTargetsAllowed(),
					BooleanAbilityTargetCheckReceiver.<CWidget>getInstance().reset())) {
				recipients.add(unit);
			}
			return false;
		});
		for (final CUnit unit : recipients) {
			final ArmorBuff previous = unit.getFirstAbilityOfType(ArmorBuff.class);
			if (previous != null) {
				unit.remove(game, previous);
			}
			unit.add(game, new ArmorBuff(game.getHandleIdAllocator().createId(), this.buffId,
					getDurationForTarget(unit), this.armor));
		}
		game.createTemporarySpellEffectOnUnit(caster, getAlias(), CEffectType.CASTER);
		return false;
	}

	private static final class ArmorBuff extends CBuffTimed {
		private final NonStackingStatBuff defense;

		private ArmorBuff(final int handleId, final War3ID alias, final float duration, final float armor) {
			super(handleId, CODE, alias, duration);
			this.defense = new NonStackingStatBuff(NonStackingStatBuffType.DEF, CODE.toString(), armor);
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

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.addNonStackingStatBuff(this.defense);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.removeNonStackingStatBuff(this.defense);
		}
	}
}

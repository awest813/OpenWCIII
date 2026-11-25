package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid.MovementType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.StateModBuffType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

/** Critter transformations keep the original unit and its inventory in the simulation. */
public final class CAbilityPolymorph extends CAbilityTargetSpellBase {
	public static final War3ID HEX = War3ID.fromString("AOhx");
	public static final War3ID POLYMORPH = War3ID.fromString("Aply");

	private boolean hex;
	private int speedOrCreepLimit;
	private War3ID buffId;
	private List<CUnitTypeRequirement> requirements = List.of();
	private List<War3ID> landForms = List.of();
	private List<War3ID> airForms = List.of();
	private List<War3ID> amphibiousForms = List.of();
	private List<War3ID> waterForms = List.of();

	public CAbilityPolymorph(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public int getBaseOrderId() {
		return this.hex ? OrderIds.hex : OrderIds.polymorph;
	}

	@Override
	public void populateData(final GameObject definition, final int level) {
		this.hex = HEX.equals(getCode());
		this.speedOrCreepLimit = definition.getFieldAsInteger(AbilityFields.DATA_A + level, 0);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(definition, level);
		this.requirements = CUnitData.parseRequirements(definition.getFieldAsList("Requires"),
				definition.getFieldAsList("Requiresamount"));
		this.landForms = readForms(definition, AbilityFields.DATA_B + level);
		this.airForms = readForms(definition, AbilityFields.DATA_C + level);
		this.amphibiousForms = readForms(definition, AbilityFields.DATA_D + level);
		this.waterForms = readForms(definition, AbilityFields.DATA_E + level);
		if (CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			getTargetsAllowed().addAll(CTargetType.parseTargetTypeSet(definition.getFieldAsList(AbilityFields.TARGETS_ALLOWED)));
		}
	}

	private static List<War3ID> readForms(final GameObject definition, final String field) {
		final List<War3ID> forms = new ArrayList<>();
		for (final String id : definition.getFieldAsList(field)) {
			if (id.length() == 4) forms.add(War3ID.fromString(id));
		}
		return forms;
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit caster) {
		final var player = game.getPlayer(caster.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement ->
				player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel());
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit caster, final AbilityActivationReceiver receiver) {
		final var player = game.getPlayer(caster.getPlayerIndex());
		for (final var requirement : this.requirements) {
			if (player.getTechtreeUnlocked(requirement.getRequirement()) < requirement.getRequiredLevel()) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	private boolean canTransform(final CSimulation game, final CUnit unit) {
		return !unit.isBuilding() && !unit.hasMorphImmunity() && !unit.isUnitType(CUnitTypeJass.SUMMONED)
				&& (this.hex || !unit.isHero() && (game.getPlayer(unit.getPlayerIndex()).getController() != CMapControl.NEUTRAL
						|| this.speedOrCreepLimit <= 0 || unit.getUnitType().getLevel() <= this.speedOrCreepLimit));
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit caster, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (target instanceof CUnit unit && canTransform(game, unit)) receiver.targetOk(target);
		else receiver.targetCheckFailed(CommandStringErrorKeys.UNABLE_TO_TARGET_THIS_UNIT);
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		final CUnit unit = target.visit(AbilityTargetVisitor.UNIT);
		if (unit == null || !canTransform(game, unit) || !unit.checkForAbilityEffectReaction(game, caster, this)) return false;
		final float duration = getDurationForTarget(unit);
		if (duration <= 0) return false;
		final MovementType movement = unit.getMovementType();
		final List<War3ID> movementForms = switch (movement) {
		case FLY -> this.airForms;
		case AMPHIBIOUS -> this.amphibiousForms;
		case FLOAT -> this.waterForms;
		default -> this.landForms;
		};
		final List<War3ID> forms = movementForms.isEmpty() ? this.landForms : movementForms;
		final War3ID form = forms.isEmpty() ? War3ID.fromString(movement == MovementType.FLY ? "nshf" : "nshe")
				: forms.get(game.getSeededRandom().nextInt(forms.size()));
		final CritterForm previous = unit.getFirstAbilityOfType(CritterForm.class);
		if (previous != null) unit.remove(game, previous);
		unit.add(game, new CritterForm(game.getHandleIdAllocator().createId(), getCode(), this.buffId, duration, form,
				this.hex ? this.speedOrCreepLimit : 0, !this.hex));
		return false;
	}

	private static final class CritterForm extends CBuffTimed {
		private final War3ID form;
		private final int speed;
		private final boolean mediumArmor;
		private final StateModBuff disarmed = new StateModBuff(StateModBuffType.DISABLE_ATTACK, 1);
		private final StateModBuff silenced = new StateModBuff(StateModBuffType.DISABLE_SPELLS, 1);
		private final StateModBuff noAutocast = new StateModBuff(StateModBuffType.DISABLE_AUTO_CAST, 1);

		private CritterForm(final int handle, final War3ID code, final War3ID buffId, final float duration,
				final War3ID form, final int speed, final boolean mediumArmor) {
			super(handle, code, buffId, duration);
			this.form = form;
			this.speed = speed;
			this.mediumArmor = mediumArmor;
		}

		@Override
		protected void onBuffAdd(final CSimulation game, final CUnit unit) {
			unit.setCritterForm(game, this.form, this.speed, this.mediumArmor);
			unit.addStateModBuff(this.disarmed);
			unit.addStateModBuff(this.silenced);
			unit.addStateModBuff(this.noAutocast);
			updateAbilities(game, unit);
		}

		@Override
		protected void onBuffRemove(final CSimulation game, final CUnit unit) {
			unit.setCritterForm(game, null, 0, false);
			unit.removeStateModBuff(this.disarmed);
			unit.removeStateModBuff(this.silenced);
			unit.removeStateModBuff(this.noAutocast);
			updateAbilities(game, unit);
		}

		private void updateAbilities(final CSimulation game, final CUnit unit) {
			unit.computeUnitState(game, StateModBuffType.DISABLE_ATTACK);
			unit.computeUnitState(game, StateModBuffType.DISABLE_AUTO_CAST);
			unit.checkDisabledAbilities(game, false);
		}

		@Override
		public boolean isDispellable() {
			return true;
		}

		@Override
		public DispelPolarity getDispelPolarity() {
			return DispelPolarity.HARMFUL;
		}

		@Override
		public boolean isTimedLifeBar() {
			return false;
		}
	}
}

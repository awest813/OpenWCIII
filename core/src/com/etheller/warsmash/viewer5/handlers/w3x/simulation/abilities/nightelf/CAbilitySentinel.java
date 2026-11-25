package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.nightelf;

import java.util.EnumSet;
import java.util.List;

import com.etheller.warsmash.units.GameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitTypeRequirement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CDestructableBuff;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CAbilityProjectileListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.projectile.CProjectile;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.data.CUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CFogModifier;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.SimulationRenderComponentModel;

public final class CAbilitySentinel extends CAbilityTargetSpellBase {
	private List<CUnitTypeRequirement> requirements;
	private float flightSight;
	private float hoveringSight;
	private float hoveringHeight;
	private float missileSpeed;
	private int owls;
	private int owlsUsed;

	public CAbilitySentinel(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final GameObject data, final int level) {
		this.requirements = CUnitData.parseRequirements(data.getFieldAsList("Requires"), data.getFieldAsList("Requiresamount"));
		this.flightSight = data.getFieldAsFloat(AbilityFields.DATA_A + level, 0);
		this.hoveringSight = data.getFieldAsFloat(AbilityFields.DATA_B + level, 0);
		this.hoveringHeight = data.getFieldAsFloat(AbilityFields.DATA_C + level, 0);
		this.owls = data.getFieldAsInteger(AbilityFields.DATA_D + level, 0);
		this.missileSpeed = data.getFieldAsFloat(AbilityFields.PROJECTILE_SPEED, 0);
		if (CTargetType.parseTargetTypeSet(data.getFieldAsList(AbilityFields.TARGETS_ALLOWED + level)).isEmpty()) {
			setTargetsAllowed(EnumSet.of(CTargetType.TREE));
		}
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.sentinel;
	}

	@Override
	public int getUsesRemaining() {
		return Math.max(0, this.owls - this.owlsUsed);
	}

	@Override
	public boolean isRequirementsMet(final CSimulation game, final CUnit unit) {
		final CPlayer player = game.getPlayer(unit.getPlayerIndex());
		return this.requirements.stream().allMatch(requirement -> hasRequirement(player, requirement));
	}

	@Override
	public void checkRequirementsMet(final CSimulation game, final CUnit unit, final AbilityActivationReceiver receiver) {
		final CPlayer player = game.getPlayer(unit.getPlayerIndex());
		for (final CUnitTypeRequirement requirement : this.requirements) {
			if (!hasRequirement(player, requirement)) {
				receiver.missingRequirement(requirement.getRequirement(), requirement.getRequiredLevel());
			}
		}
	}

	private static boolean hasRequirement(final CPlayer player, final CUnitTypeRequirement requirement) {
		return player.getTechtreeUnlocked(requirement.getRequirement()) >= requirement.getRequiredLevel();
	}

	@Override
	protected void innerCheckCanUseSpell(final CSimulation game, final CUnit unit, final int orderId,
			final AbilityActivationReceiver receiver) {
		if (getUsesRemaining() == 0) {
			receiver.noChargesRemaining();
		}
		else {
			receiver.useOk();
		}
	}

	@Override
	protected void innerCheckCanTargetSpell(final CSimulation game, final CUnit unit, final int orderId,
			final CWidget target, final AbilityTargetCheckReceiver<CWidget> receiver) {
		if (!(target instanceof CDestructable tree) || tree.isDead()
				|| !tree.getDestType().getTargetedAs().contains(CTargetType.TREE)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.MUST_TARGET_A_TREE);
		}
		else if (tree.hasBuff(SentinelOwl.class)) {
			receiver.targetCheckFailed(CommandStringErrorKeys.THAT_TREE_IS_OCCUPIED_BY_AN_OWL);
		}
		else {
			receiver.targetOk(target);
		}
	}

	@Override
	public boolean doEffect(final CSimulation game, final CUnit caster, final AbilityTarget target) {
		if (!(target instanceof CDestructable tree) || tree.isDead() || tree.hasBuff(SentinelOwl.class)
				|| getUsesRemaining() == 0) {
			return false;
		}
		this.owlsUsed++;
		caster.notifyOrdersChanged();
		final var owl = new SentinelOwl(game.getHandleIdAllocator().createId(), getAlias(), getLevel(),
				game.getPlayer(caster.getPlayerIndex()), this.flightSight, this.hoveringSight, this.hoveringHeight, tree);
		tree.add(game, owl);
		if (this.missileSpeed > 0) {
			game.createProjectile(caster, getAlias(), caster.getX(), caster.getY(), (float) caster.angleTo(tree),
					this.missileSpeed, false, tree, owl);
		}
		else {
			owl.onHit(game, null, tree);
		}
		return false;
	}

	private static final class SentinelOwl extends CFogModifier implements CDestructableBuff, CAbilityProjectileListener {
		private static final byte TRUE_SIGHT = 3;
		private final int handleId;
		private final War3ID alias;
		private final CPlayer owner;
		private final float flightSight;
		private final float hoveringSight;
		private final float hoveringHeight;
		private final CDestructable tree;
		private int level;
		private boolean attached;
		private boolean hovering;
		private CProjectile projectile;
		private SimulationRenderComponentModel model;

		private SentinelOwl(final int handleId, final War3ID alias, final int level, final CPlayer owner,
				final float flightSight, final float hoveringSight, final float hoveringHeight, final CDestructable tree) {
			this.handleId = handleId;
			this.alias = alias;
			this.level = level;
			this.owner = owner;
			this.flightSight = flightSight;
			this.hoveringSight = hoveringSight;
			this.hoveringHeight = hoveringHeight;
			this.tree = tree;
		}

		@Override
		public void onAdd(final CSimulation game, final CDestructable tree) {
			this.attached = true;
			this.owner.addFogModifer(game, this, false);
		}

		@Override
		public void onLaunch(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
			this.projectile = projectile;
		}

		@Override
		public void onHit(final CSimulation game, final CProjectile projectile, final AbilityTarget target) {
			this.projectile = null;
			if (this.attached) {
				this.hovering = true;
				this.model = game.spawnSpellEffectOnPoint(this.tree.getX(), this.tree.getY(), 0,
						this.alias, CEffectType.MISSILE, 0);
				if (this.model != null) {
					this.model.setHeight(this.hoveringHeight);
				}
			}
		}

		@Override
		public void update(final CSimulation game, final CPlayer player, final PathingGrid pathingGrid,
				final CPlayerFogOfWar fogOfWar) {
			if (this.projectile != null) {
				fogOfWar.setFogStateRadius(pathingGrid, this.projectile.getX(), this.projectile.getY(),
						this.flightSight, CFogState.VISIBLE, TRUE_SIGHT);
			}
			else if (this.hovering) {
				fogOfWar.setFogStateRadius(pathingGrid, this.tree.getX(), this.tree.getY(),
						this.hoveringSight, CFogState.VISIBLE, TRUE_SIGHT);
			}
		}

		@Override
		public void onRemove(final CSimulation game, final CDestructable tree) {
			this.attached = false;
			this.hovering = false;
			this.owner.removeFogModifer(game, this);
			if (this.model != null) {
				this.model.remove();
			}
		}

		@Override
		public void onDamage(final CSimulation game, final CDestructable tree) {
			tree.remove(game, this);
		}

		@Override
		public boolean isDispellable() {
			return this.hovering;
		}

		@Override
		public void onDeath(final CSimulation game, final CDestructable tree) {
			tree.remove(game, this);
		}

		@Override
		public int getHandleId() {
			return this.handleId;
		}

		@Override
		public War3ID getAlias() {
			return this.alias;
		}

		@Override
		public int getLevel() {
			return this.level;
		}

		@Override
		public void setLevel(final int level) {
			this.level = level;
		}
	}
}

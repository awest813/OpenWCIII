package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CTargetType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.AbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CommandStringErrorKeys;

class DestructableTargetingTest {

	private static final class TestTargetReceiver implements AbilityTargetCheckReceiver<CWidget> {
		private boolean success = false;
		private String failureKey = null;

		@Override
		public void targetCheckFailed(final String commandStringErrorKey) {
			this.success = false;
			this.failureKey = commandStringErrorKey;
		}

		@Override
		public void targetOk(final CWidget target) {
			this.success = true;
			this.failureKey = null;
		}

		@Override
		public void orderIdNotAccepted() {
			this.success = false;
		}

		@Override
		public void notAnActiveAbility() {
			this.success = false;
		}
	}

	@Test
	void testDecorativeDestructableCannotBeTargetedByNormalAttack() {
		// Spikes or decorative rocks: selectable=false, canAttack=false, targetedAs=[GROUND]
		final CDestructableType spikesType = new CDestructableType(War3ID.fromString("DTsp"), "Spikes", 100f,
				EnumSet.of(CTargetType.GROUND), "wood", 0, 0, 0, 0, 0f, null, null, false, false);
		final CDestructable spikes = new CDestructable(1, 0, 0, 100f, spikesType, null, null);

		final TestTargetReceiver receiver = new TestTargetReceiver();
		final boolean canTarget = spikes.canBeTargetedBy(null, null, EnumSet.of(CTargetType.GROUND), receiver);

		assertFalse(canTarget, "Unattackable decorative destructable must not be targetable");
		assertFalse(receiver.success);
	}

	@Test
	void testTreeTargetableByHarvestEvenIfCanAttackIsFalse() {
		// Trees in DestructableData.slk have canAttack=false and targetedAs=[GROUND, TREE]
		final CDestructableType treeType = new CDestructableType(War3ID.fromString("ATtr"), "Tree", 50f,
				EnumSet.of(CTargetType.GROUND, CTargetType.TREE), "wood", 0, 0, 0, 0, 0f, null, null, false, false);
		final CDestructable tree = new CDestructable(2, 0, 0, 50f, treeType, null, null);

		// 1. Worker harvest attack (targets TREE): must succeed
		final TestTargetReceiver harvestReceiver = new TestTargetReceiver();
		final boolean harvestCanTarget = tree.canBeTargetedBy(null, null, EnumSet.of(CTargetType.TREE), harvestReceiver);
		assertTrue(harvestCanTarget, "Harvest attack targeting TREE must succeed on trees with canAttack=false");

		// 2. Normal unit attack (targets GROUND): must fail because canAttack=false and not a tree attack
		final TestTargetReceiver normalReceiver = new TestTargetReceiver();
		final boolean normalCanTarget = tree.canBeTargetedBy(null, null, EnumSet.of(CTargetType.GROUND), normalReceiver);
		assertFalse(normalCanTarget, "Normal attack targeting GROUND must fail on trees with canAttack=false");
	}

	@Test
	void testAttackableDestructableTargetedNormally() {
		// Gates or Barrels: selectable=true, canAttack=true, targetedAs=[GROUND]
		final CDestructableType gateType = new CDestructableType(War3ID.fromString("DTg1"), "Gate", 500f,
				EnumSet.of(CTargetType.GROUND), "metal", 0, 0, 0, 0, 0f, null, null, true, true);
		final CDestructable gate = new CDestructable(3, 0, 0, 500f, gateType, null, null);

		final TestTargetReceiver receiver = new TestTargetReceiver();
		final boolean canTarget = gate.canBeTargetedBy(null, null, EnumSet.of(CTargetType.GROUND), receiver);

		assertTrue(canTarget, "Selectable attackable destructables must be targetable by normal ground attack");
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.etheller.warsmash.viewer5.handlers.mdx.Sequence;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnitTypeData;
import com.hiveworkshop.rms.parsers.mdlx.MdlxSequence;

public class HumanTowerSequenceTest {

	private Sequence createSequence(final String name) {
		final MdlxSequence mdlxSeq = new MdlxSequence();
		mdlxSeq.name = name;
		return new Sequence(mdlxSeq);
	}

	private List<Sequence> createHumanTowerSequences() {
		final List<Sequence> sequences = new ArrayList<>();
		sequences.add(createSequence("Stand")); // 0: Scout Tower Stand
		sequences.add(createSequence("Stand Upgrade First")); // 1: Guard Tower Stand
		sequences.add(createSequence("Stand Upgrade Second")); // 2: Cannon Tower Stand
		sequences.add(createSequence("Stand Upgrade Third")); // 3: Arcane Tower Stand
		sequences.add(createSequence("Attack Upgrade First")); // 4: Guard Tower Attack
		sequences.add(createSequence("Attack Upgrade Second")); // 5: Cannon Tower Attack
		sequences.add(createSequence("Attack Upgrade Third")); // 6: Arcane Tower Attack
		sequences.add(createSequence("Death")); // 7: Base Death
		sequences.add(createSequence("Death Upgrade First")); // 8: Guard Tower Death
		sequences.add(createSequence("Death Upgrade Second")); // 9: Cannon Tower Death
		sequences.add(createSequence("Death Upgrade Third")); // 10: Arcane Tower Death
		sequences.add(createSequence("Birth")); // 11: Base Birth
		return sequences;
	}

	@Test
	public void testParseSecondaryTagsWithSpacesAndSpecialChars() {
		final EnumSet<SecondaryTag> guardTags = RenderUnitTypeData.parseSecondaryTags("upgrade,first");
		assertTrue(guardTags.contains(SecondaryTag.UPGRADE));
		assertTrue(guardTags.contains(SecondaryTag.FIRST));
		assertEquals(2, guardTags.size());

		final EnumSet<SecondaryTag> guardTagsWithSpace = RenderUnitTypeData.parseSecondaryTags("upgrade, first");
		assertTrue(guardTagsWithSpace.contains(SecondaryTag.UPGRADE));
		assertTrue(guardTagsWithSpace.contains(SecondaryTag.FIRST));
		assertEquals(2, guardTagsWithSpace.size());

		final EnumSet<SecondaryTag> cannonTags = RenderUnitTypeData.parseSecondaryTags("upgrade, second");
		assertTrue(cannonTags.contains(SecondaryTag.UPGRADE));
		assertTrue(cannonTags.contains(SecondaryTag.SECOND));
		assertEquals(2, cannonTags.size());

		final EnumSet<SecondaryTag> arcaneTags = RenderUnitTypeData.parseSecondaryTags("upgrade, third");
		assertTrue(arcaneTags.contains(SecondaryTag.UPGRADE));
		assertTrue(arcaneTags.contains(SecondaryTag.THIRD));
		assertEquals(2, arcaneTags.size());

		final EnumSet<SecondaryTag> nullTags = RenderUnitTypeData.parseSecondaryTags((String) null);
		assertTrue(nullTags.isEmpty());

		final EnumSet<SecondaryTag> emptyTags = RenderUnitTypeData.parseSecondaryTags("");
		assertTrue(emptyTags.isEmpty());

		final EnumSet<SecondaryTag> dashTags = RenderUnitTypeData.parseSecondaryTags("-");
		assertTrue(dashTags.isEmpty());

		final EnumSet<SecondaryTag> underscoreTags = RenderUnitTypeData.parseSecondaryTags("_");
		assertTrue(underscoreTags.isEmpty());
	}

	@Test
	public void testParseSecondaryTagsFromList() {
		// When UnitFunc.txt specifies Animprops=upgrade,first, DataTable.readTXT splits commas into a list:
		final EnumSet<SecondaryTag> guardTagsFromList = RenderUnitTypeData.parseSecondaryTags(
				Arrays.asList("upgrade", "first"));
		assertTrue(guardTagsFromList.contains(SecondaryTag.UPGRADE));
		assertTrue(guardTagsFromList.contains(SecondaryTag.FIRST));
		assertEquals(2, guardTagsFromList.size());

		// Cannon Tower Animprops=upgrade,second
		final EnumSet<SecondaryTag> cannonTagsFromList = RenderUnitTypeData.parseSecondaryTags(
				Arrays.asList("upgrade", "second"));
		assertTrue(cannonTagsFromList.contains(SecondaryTag.UPGRADE));
		assertTrue(cannonTagsFromList.contains(SecondaryTag.SECOND));
		assertEquals(2, cannonTagsFromList.size());

		// Arcane Tower Animprops=upgrade,third
		final EnumSet<SecondaryTag> arcaneTagsFromList = RenderUnitTypeData.parseSecondaryTags(
				Arrays.asList("upgrade", "third"));
		assertTrue(arcaneTagsFromList.contains(SecondaryTag.UPGRADE));
		assertTrue(arcaneTagsFromList.contains(SecondaryTag.THIRD));
		assertEquals(2, arcaneTagsFromList.size());

		// Empty and null handling
		assertTrue(RenderUnitTypeData.parseSecondaryTags((List<String>) null).isEmpty());
		assertTrue(RenderUnitTypeData.parseSecondaryTags(Arrays.asList("", "-", "_")).isEmpty());
	}

	@Test
	public void testIsCompatibleStructuralIntegrity() {
		final EnumSet<SecondaryTag> guardGoal = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST);
		final EnumSet<SecondaryTag> cannonGoal = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND);
		final EnumSet<SecondaryTag> arcaneGoal = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.THIRD);
		final EnumSet<SecondaryTag> scoutGoal = EnumSet.noneOf(SecondaryTag.class);

		// Guard tower is compatible with Guard sequences
		assertTrue(SequenceUtils.isCompatible(guardGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST)));
		// Guard tower with READY is compatible with Guard sequences
		assertTrue(SequenceUtils.isCompatible(EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST, SecondaryTag.READY),
				EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST)));
		// Guard tower is NOT compatible with Cannon or Arcane sequences
		assertFalse(SequenceUtils.isCompatible(guardGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND)));
		assertFalse(SequenceUtils.isCompatible(guardGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.THIRD)));

		// Cannon tower is NOT compatible with Guard or Arcane sequences
		assertFalse(SequenceUtils.isCompatible(cannonGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST)));
		assertFalse(SequenceUtils.isCompatible(cannonGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.THIRD)));
		assertTrue(SequenceUtils.isCompatible(cannonGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND)));

		// Scout tower (no UPGRADE) is NOT compatible with any UPGRADE sequences
		assertFalse(SequenceUtils.isCompatible(scoutGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST)));
		assertFalse(SequenceUtils.isCompatible(scoutGoal, EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND)));
		assertFalse(SequenceUtils.isCompatible(scoutGoal, EnumSet.of(SecondaryTag.UPGRADE)));
		// Scout tower is compatible with base (empty) sequences
		assertTrue(SequenceUtils.isCompatible(scoutGoal, EnumSet.noneOf(SecondaryTag.class)));
	}

	@Test
	public void testHumanTowerSequenceSelection() {
		final List<Sequence> sequences = createHumanTowerSequences();

		// Scout Tower Stand: should pick sequence 0 (Stand)
		final IndexedSequence scoutStand = SequenceUtils.selectSequence(PrimaryTag.STAND, SequenceUtils.EMPTY, sequences,
				false);
		assertNotNull(scoutStand);
		assertEquals(0, scoutStand.index);
		assertEquals("Stand", scoutStand.sequence.getName());

		// Scout Tower in READY stance: should fall back to Stand, NEVER pick an upgrade sequence
		final IndexedSequence scoutReady = SequenceUtils.selectSequence(PrimaryTag.STAND, SequenceUtils.READY, sequences,
				false);
		assertNotNull(scoutReady);
		assertEquals(0, scoutReady.index);
		assertEquals("Stand", scoutReady.sequence.getName());

		// Guard Tower Stand: should pick sequence 1 (Stand Upgrade First)
		final EnumSet<SecondaryTag> guardTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST);
		final IndexedSequence guardStand = SequenceUtils.selectSequence(PrimaryTag.STAND, guardTags, sequences, false);
		assertNotNull(guardStand);
		assertEquals(1, guardStand.index);
		assertEquals("Stand Upgrade First", guardStand.sequence.getName());

		// Guard Tower Ready: should fall back to Stand Upgrade First, NEVER Cannon or Scout
		final EnumSet<SecondaryTag> guardReadyTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.FIRST,
				SecondaryTag.READY);
		final IndexedSequence guardReady = SequenceUtils.selectSequence(PrimaryTag.STAND, guardReadyTags, sequences,
				false);
		assertNotNull(guardReady);
		assertEquals(1, guardReady.index);
		assertEquals("Stand Upgrade First", guardReady.sequence.getName());

		// Guard Tower Attack: should pick sequence 4 (Attack Upgrade First)
		final IndexedSequence guardAttack = SequenceUtils.selectSequence(PrimaryTag.ATTACK, guardTags, sequences, false);
		assertNotNull(guardAttack);
		assertEquals(4, guardAttack.index);
		assertEquals("Attack Upgrade First", guardAttack.sequence.getName());

		// Guard Tower Birth: should fall back to base Birth (11) because no Birth Upgrade First exists
		final IndexedSequence guardBirth = SequenceUtils.selectSequence(PrimaryTag.BIRTH, guardTags, sequences, false);
		assertNotNull(guardBirth);
		assertEquals(11, guardBirth.index);
		assertEquals("Birth", guardBirth.sequence.getName());

		// Cannon Tower Stand: should pick sequence 2 (Stand Upgrade Second)
		final EnumSet<SecondaryTag> cannonTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND);
		final IndexedSequence cannonStand = SequenceUtils.selectSequence(PrimaryTag.STAND, cannonTags, sequences,
				false);
		assertNotNull(cannonStand);
		assertEquals(2, cannonStand.index);
		assertEquals("Stand Upgrade Second", cannonStand.sequence.getName());

		// Cannon Tower Ready: should fall back to Stand Upgrade Second, NEVER Guard Tower
		final EnumSet<SecondaryTag> cannonReadyTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.SECOND,
				SecondaryTag.READY);
		final IndexedSequence cannonReady = SequenceUtils.selectSequence(PrimaryTag.STAND, cannonReadyTags, sequences,
				false);
		assertNotNull(cannonReady);
		assertEquals(2, cannonReady.index);
		assertEquals("Stand Upgrade Second", cannonReady.sequence.getName());

		// Cannon Tower Attack: should pick sequence 5 (Attack Upgrade Second)
		final IndexedSequence cannonAttack = SequenceUtils.selectSequence(PrimaryTag.ATTACK, cannonTags, sequences,
				false);
		assertNotNull(cannonAttack);
		assertEquals(5, cannonAttack.index);
		assertEquals("Attack Upgrade Second", cannonAttack.sequence.getName());

		// Arcane Tower Stand: should pick sequence 3 (Stand Upgrade Third)
		final EnumSet<SecondaryTag> arcaneTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.THIRD);
		final IndexedSequence arcaneStand = SequenceUtils.selectSequence(PrimaryTag.STAND, arcaneTags, sequences,
				false);
		assertNotNull(arcaneStand);
		assertEquals(3, arcaneStand.index);
		assertEquals("Stand Upgrade Third", arcaneStand.sequence.getName());

		// Arcane Tower Ready: should fall back to Stand Upgrade Third, NEVER Guard or Cannon
		final EnumSet<SecondaryTag> arcaneReadyTags = EnumSet.of(SecondaryTag.UPGRADE, SecondaryTag.THIRD,
				SecondaryTag.READY);
		final IndexedSequence arcaneReady = SequenceUtils.selectSequence(PrimaryTag.STAND, arcaneReadyTags, sequences,
				false);
		assertNotNull(arcaneReady);
		assertEquals(3, arcaneReady.index);
		assertEquals("Stand Upgrade Third", arcaneReady.sequence.getName());
	}
}

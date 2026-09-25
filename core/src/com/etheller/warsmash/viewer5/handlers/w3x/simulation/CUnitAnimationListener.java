package com.etheller.warsmash.viewer5.handlers.w3x.simulation;

import java.util.EnumSet;

import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.SecondaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;

public interface CUnitAnimationListener {
	void playAnimation(boolean force, final PrimaryTag animationName,
			final EnumSet<SecondaryTag> secondaryAnimationTags, float speedRatio, boolean allowRarityVariations);

	void playAnimationWithDuration(final boolean force, final PrimaryTag animationName,
			final EnumSet<SecondaryTag> secondaryAnimationTags, final float duration,
			final boolean allowRarityVariations);

	void playWalkAnimation(boolean force, float currentMovementSpeed, boolean allowRarityVariations);

	void playAnimation(boolean force, int sequenceIndex, float speedRatio, boolean allowRarityVariations);

	void queueAnimation(final PrimaryTag animationName, final EnumSet<SecondaryTag> secondaryAnimationTags,
			boolean allowRarityVariations);

	boolean addSecondaryTag(SecondaryTag secondaryTag);

	boolean removeSecondaryTag(SecondaryTag secondaryTag);

	void forceResetCurrentAnimation();

	EnumSet<SecondaryTag> getSecondaryTags();

	void lockTurretFacing(AbilityTarget target);

	void clearTurretFacing();

	void lockHeadFacing(AbilityTarget target);

	void clearHeadFacing();

	CUnitAnimationListener DO_NOTHING = new CUnitAnimationListener() {
		private final EnumSet<SecondaryTag> emptyTags = EnumSet.noneOf(SecondaryTag.class);

		@Override
		public void playAnimation(final boolean force, final PrimaryTag animationName,
				final EnumSet<SecondaryTag> secondaryAnimationTags, final float speedRatio,
				final boolean allowRarityVariations) {
		}

		@Override
		public void playAnimationWithDuration(final boolean force, final PrimaryTag animationName,
				final EnumSet<SecondaryTag> secondaryAnimationTags, final float duration,
				final boolean allowRarityVariations) {
		}

		@Override
		public void playWalkAnimation(final boolean force, final float currentMovementSpeed,
				final boolean allowRarityVariations) {
		}

		@Override
		public void playAnimation(final boolean force, final int sequenceIndex, final float speedRatio,
				final boolean allowRarityVariations) {
		}

		@Override
		public void queueAnimation(final PrimaryTag animationName, final EnumSet<SecondaryTag> secondaryAnimationTags,
				final boolean allowRarityVariations) {
		}

		@Override
		public boolean addSecondaryTag(final SecondaryTag secondaryTag) {
			return false;
		}

		@Override
		public boolean removeSecondaryTag(final SecondaryTag secondaryTag) {
			return false;
		}

		@Override
		public void forceResetCurrentAnimation() {
		}

		@Override
		public EnumSet<SecondaryTag> getSecondaryTags() {
			return this.emptyTags;
		}

		@Override
		public void lockTurretFacing(final AbilityTarget target) {
		}

		@Override
		public void clearTurretFacing() {
		}

		@Override
		public void lockHeadFacing(final AbilityTarget target) {
		}

		@Override
		public void clearHeadFacing() {
		}
	};
}

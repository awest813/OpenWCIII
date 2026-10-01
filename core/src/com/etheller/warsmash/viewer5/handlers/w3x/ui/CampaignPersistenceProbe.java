package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.Gdx;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;

/** Each opt-in phase runs in a separate desktop process with the same isolated home. */
public final class CampaignPersistenceProbe {
	private static final String PHASE = System.getProperty("warsmash.campaignPersistenceAudit", "");
	private static final String OTHER_PROFILE = "Campaign isolation audit";
	private static boolean verified;
	private static boolean profilePrepared;
	private static long started;

	private CampaignPersistenceProbe() { }

	public static void afterProfileLoad(final PlayerProfileManager manager) {
		if (PHASE.isEmpty() || profilePrepared) return;
		if (PHASE.equals("write-other")) {
			check(!manager.hasProfile(OTHER_PROFILE), "Other profile already exists in fresh audit");
			manager.addProfile(OTHER_PROFILE);
		}
		if (PHASE.equals("recreate-other")) {
			final PlayerProfile old = manager.getProfiles().stream()
					.filter(profile -> profile.getName().equals(OTHER_PROFILE)).findFirst().orElseThrow();
			manager.removeProfile(old);
			manager.addProfile(OTHER_PROFILE);
		}
		final String selected = PHASE.endsWith("original") ? "WorldEdit" : OTHER_PROFILE;
		check(manager.hasProfile(selected), "Expected profile missing: " + selected);
		manager.setCurrentProfile(selected);
		profilePrepared = true;
		System.out.println("[CampaignPersistenceProbe] " + PHASE + " selected " + selected);
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (PHASE.isEmpty() || PHASE.startsWith("write-") || verified) return;
		if (started == 0) started = System.nanoTime();
		check(System.nanoTime() - started < 120_000_000_000L, "Timed out waiting for restored hero");
		if (!initialized) return;
		final GlobalScope globals = viewer.simulation.getGlobalScope();
		final CUnit hero = globals.getGlobal("udg_Arthas").visit(ObjectJassValueVisitor.<CUnit>getInstance());
		if (hero == null) return;
		check(viewer.getCurrentMapPath().toLowerCase(java.util.Locale.ROOT).contains("human02"), "Expected Human02");
		final StoredUnitData actual = CGameSave.snapshotUnit(hero);
		final CampaignProgressStore progress = CampaignProgressStore.get();
		check(!progress.isMissionAvailable(1, 2), "Unfinished Human03 unlocked");
		if (PHASE.equals("recreate-other")) {
			check(!progress.isMissionAvailable(1, 1), "Recreated profile inherited unlocks");
			check(!actual.properName.equals("Other profile hero") && !actual.properName.equals("Carryover audit"),
					"Recreated profile inherited deleted hero");
			check(actual.items[4] == null || !actual.items[4].typeId.equals(War3ID.fromString("phea"))
					|| actual.items[4].charges != 3, "Recreated profile inherited fixture inventory");
		}
		else {
			final String expected = PHASE.equals("read-original") ? "Carryover audit" : "Other profile hero";
			check(progress.isMissionAvailable(1, 1), "Unlock missing after process relaunch");
			check(expected.equals(actual.properName), "Another profile's hero restored: " + actual.properName);
			check(hero.getHeroData().getHeroLevel() == 3, "Hero level missing after process relaunch");
			check(actual.items[4] != null && actual.items[4].typeId.equals(War3ID.fromString("phea"))
					&& actual.items[4].charges == 3, "Inventory slot/charges missing after process relaunch");
			check(java.util.Arrays.stream(actual.abilities).anyMatch(ability ->
					ability.abilityId.equals(War3ID.fromString("AHhb")) && ability.level >= 1),
					"Learned skill missing after process relaunch");
		}
		verified = true;
		System.out.println("[CampaignPersistenceProbe] " + PHASE + " hero=" + actual.properName
				+ " xp=" + actual.xp + " skillPoints=" + actual.skillPoints);
		ui.customVictory(false);
	}

	public static void afterMenuRender() {
		if (!PHASE.isEmpty() && verified) {
			System.out.println("[CampaignPersistenceProbe] " + PHASE + " complete");
			Gdx.app.exit();
		}
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Campaign persistence audit: " + message);
	}
}

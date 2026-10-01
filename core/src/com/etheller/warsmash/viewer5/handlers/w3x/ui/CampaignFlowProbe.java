package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuestItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;

/** Opt-in retail transition checks. World-event fixtures are not normal gameplay. */
public final class CampaignFlowProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.campaignFlowAudit");
	private static final boolean OBJECTIVE_EVENTS = Boolean.getBoolean("warsmash.campaignObjectiveEvents");
	private static int stage;
	private static int objectiveStage;
	private static int remainingCrewTick;
	private static CUnit remainingCrew;
	private static War3MapViewer previousViewer;
	private static StoredUnitData expectedHero;
	private static long started;
	private static GlobalScope introSkipRequested;
	private static boolean victorySkipRequested;

	private CampaignFlowProbe() { }

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED) return;
		if (started == 0) started = System.nanoTime();
		check(System.nanoTime() - started < 120_000_000_000L,
				"Timed out at stage " + stage + ", objective event " + objectiveStage);
		if (!initialized) return;
		final GlobalScope globals = viewer.simulation.getGlobalScope();
		final CScriptDialog dialog = ui.getVisibleScriptDialog();
		switch (stage) {
		case 0:
			if (hero(globals) == null) return;
			if (OBJECTIVE_EVENTS && !readyAfterIntro(globals, ui)) return;
			check(viewer.getCurrentMapPath().toLowerCase().contains("human01"), "Expected Human01");
			check(viewer.getLocalPlayerIndex() == 1, "Wrong local campaign player");
			check(!CampaignProgressStore.get().isMissionAvailable(1, 1), "Fresh profile exposes Human02");
			viewer.getMapConfig().setGameDifficulty(CMapDifficulty.HARD);
			viewer.getMapConfig().setDefaultGameDifficulty(CMapDifficulty.HARD);
			previousViewer = viewer;
			if (OBJECTIVE_EVENTS) {
				check(!flag(globals, "udg_GAMEOVER"), "Mission ended before defeat fixture");
				hero(globals).kill(viewer.simulation);
				advance("Arthas death dispatched to retail defeat trigger");
			}
			else {
				run(globals, "Trig_Defeat_Cheat_Actions");
				advance("retail defeat action");
			}
			break;
		case 1:
			if (dialog == null) return;
			check(!CampaignProgressStore.get().isMissionAvailable(1, 1), "Defeat unlocked Human02");
			if (OBJECTIVE_EVENTS) check(flag(globals, "udg_GAMEOVER"), "Death did not end the mission");
			check(dialog.getButtons().size() >= 2, "Retail defeat choices missing");
			// Retail CustomDefeatDialogBJ adds Restart, then Reduce Difficulty.
			dialog.getButtons().get(1).getButtonFrame().onClick(Input.Buttons.LEFT);
			advance("reduce difficulty and retry selected");
			break;
		case 2:
			if (dialog == null) return;
			check("Restart Mission".equals(ui.getScoreDialogTitle()),
					"Retry incorrectly presented as victory");
			check(viewer.getMapConfig().getGameDifficulty() == CMapDifficulty.NORMAL, "Retail reduction failed");
			advance("restart hotkey");
			ui.keyDown(Input.Keys.R);
			break;
		case 3:
			if (viewer == previousViewer || hero(globals) == null) return;
			if (OBJECTIVE_EVENTS && !readyAfterIntro(globals, ui)) return;
			check(viewer.getCurrentMapPath().toLowerCase().contains("human01"), "Retry changed mission");
			check(viewer.getMapConfig().getGameDifficulty() == CMapDifficulty.NORMAL, "Retry lost reduced difficulty");
			check(viewer.getMapConfig().getDefaultGameDifficulty() == CMapDifficulty.HARD, "Retry lost original difficulty");
			if (OBJECTIVE_EVENTS && objectiveStage > 0) {
				if (!completeObjectiveEvents(viewer, globals)) return;
				previousViewer = viewer;
				advance("final crew death dispatched to retail victory condition");
				break;
			}
			final CUnit arthas = hero(globals);
			check(!arthas.isDead(), "Retry restored a dead hero");
			check(!CampaignProgressStore.get().isMissionAvailable(1, 1), "Retry unlocked Human02");
			arthas.getHeroData().setHeroLevel(viewer.simulation, arthas, 3, false);
			arthas.getHeroData().setProperName(System.getProperty("warsmash.campaignFlowHeroName", "Carryover audit"));
			arthas.getHeroData().selectHeroSkill(viewer.simulation, arthas, War3ID.fromString("AHhb"));
			final CItem potion = viewer.simulation.createItem(War3ID.fromString("phea"), arthas.getX(), arthas.getY());
			potion.setCharges(3);
			check(arthas.getInventoryData().giveItem(viewer.simulation, arthas, potion, 4, false) == 4,
					"Carryover fixture inventory failed");
			expectedHero = CGameSave.snapshotUnit(arthas);
			if (OBJECTIVE_EVENTS) {
				check(!flag(globals, "udg_GAMEOVER"), "Retry retained defeat state");
				check(!objective(globals, "udg_RequirementVillageTravel").isCompleted(),
						"Retry retained completed objectives");
				// Enter the retail Warlord return region, which queues the village objective update.
				final Rectangle rect = object(globals, "gg_rct_Warlord_5_Run_Here");
				final CUnit warlord = object(globals, "udg_EndBoss");
				warlord.setPoint(rect.x + rect.width / 2, rect.y + rect.height / 2,
						viewer.simulation.getWorldCollision(), viewer.simulation.getRegionManager());
				objectiveStage = 1;
				System.out.println("[CampaignFlowProbe] objective: Warlord return region entered");
				break;
			}
			previousViewer = viewer;
			run(globals, "Trig_Next_Level_Prep_Actions");
			run(globals, "Trig_Next_Level_Run_Actions");
			advance("retail victory preparation and next-level actions");
			break;
		case 4:
			if (OBJECTIVE_EVENTS) {
				if (!victorySkipRequested && triggerEnabled(globals, "gg_trg_Victory_Cancel")) {
					victorySkipRequested = true;
					ui.keyDown(Input.Keys.ESCAPE);
				}
				if (!objective(globals, "udg_RequirementVillageSlay").isCompleted()) return;
				check(flag(globals, "udg_GAMEOVER"), "Crew deaths did not end the mission");
				check(objective(globals, "udg_RequirementVillageArthas").isCompleted(),
						"Victory did not complete the surviving-Arthas objective");
			}
			if (dialog == null) return;
			check(CampaignProgressStore.get().isMissionAvailable(1, 1), "Victory did not unlock Human02");
			check(dialog.getButtons().size() == 2, "Retail victory choices missing");
			dialog.getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
			advance("retail victory Continue selected");
			break;
		case 5:
			if (dialog == null || ui.getScoreDialogTitle() == null) return;
			check("Victory".equals(ui.getScoreDialogTitle()), "Missing score continuation");
			advance("score Continue hotkey");
			ui.keyDown(Input.Keys.C);
			break;
		case 6:
			if (viewer == previousViewer || hero(globals) == null) return;
			check(viewer.getCurrentMapPath().toLowerCase().contains("human02"), "Next chapter did not load");
			check(viewer.getMapConfig().getGameDifficulty() == CMapDifficulty.HARD, "Victory did not restore selected difficulty");
			final StoredUnitData restored = CGameSave.snapshotUnit(hero(globals));
			check(expectedHero.xp == restored.xp, "Hero experience changed");
			check(expectedHero.skillPoints == restored.skillPoints, "Hero skill points changed");
			check(expectedHero.properName.equals(restored.properName), "Hero replaced with fallback");
			check(restored.items[4] != null && restored.items[4].charges == 3
					&& restored.items[4].typeId.equals(War3ID.fromString("phea")), "Hero inventory changed");
			check(expectedHero.strengthBase == restored.strengthBase, "Hero stats changed");
			check(java.util.Arrays.stream(restored.abilities).anyMatch(ability ->
					ability.abilityId.equals(War3ID.fromString("AHhb")) && ability.level >= 1), "Learned skill lost");
			PlayerProfileManager.loadFromGdx();
			CampaignProgressStore.get().seedCampaignEntries(1, 9);
			check(CampaignProgressStore.get().isMissionAvailable(1, 1), "Unlock lost after profile reload");
			check(!CampaignProgressStore.get().isMissionAvailable(1, 2), "Unfinished chapter became available");
			advance("Human02 initialized with carried hero, selected difficulty, and persisted unlock");
			ui.customVictory(false);
			break;
		default:
			break;
		}
	}

	public static void afterMenuRender() {
		if (ENABLED && stage == 7) {
			advance("returned to menu");
			System.out.println("[CampaignFlowProbe] complete");
			if (OBJECTIVE_EVENTS) System.out.println("[CampaignFlowProbe] objective events complete");
			Gdx.app.exit();
		}
	}

	private static CUnit hero(final GlobalScope globals) {
		return globals.getGlobal("udg_Arthas").visit(ObjectJassValueVisitor.<CUnit>getInstance());
	}

	private static boolean readyAfterIntro(final GlobalScope globals, final MeleeUI ui) {
		if (flag(globals, "udg_HasArthasArrived")) return true;
		if (introSkipRequested != globals && triggerEnabled(globals, "gg_trg_Intro_Cancel")) {
			introSkipRequested = globals;
			ui.keyDown(Input.Keys.ESCAPE);
		}
		return false;
	}

	private static boolean completeObjectiveEvents(final War3MapViewer viewer, final GlobalScope globals) {
		if (objectiveStage == 1) {
			final CQuestItem travel = objective(globals, "udg_RequirementVillageTravel");
			final CQuestItem slay = objective(globals, "udg_RequirementVillageSlay");
			if (!travel.isCompleted() || slay == null) return false;
			check(!slay.isCompleted(), "Slay objective completed before crew deaths");
			final List<CUnit> crew = new ArrayList<>(CampaignFlowProbe.<List<CUnit>>object(
					globals, "udg_WarlordAndCrewGroup"));
			check(crew.size() > 1, "Retail victory group is missing");
			remainingCrew = crew.remove(crew.size() - 1);
			for (final CUnit unit : crew) unit.kill(viewer.simulation);
			check(!remainingCrew.isDead(), "Final crew member already dead");
			remainingCrewTick = viewer.simulation.getGameTurnTick();
			objectiveStage = 2;
			System.out.println("[CampaignFlowProbe] objective: travel completed; one enemy remains");
			return false;
		}
		if (viewer.simulation.getGameTurnTick() - remainingCrewTick < 8) return false;
		check(!flag(globals, "udg_GAMEOVER"), "Victory fired with an enemy still alive");
		check(!CampaignProgressStore.get().isMissionAvailable(1, 1), "Partial objective unlocked Human02");
		expectedHero = CGameSave.snapshotUnit(hero(globals));
		remainingCrew.kill(viewer.simulation);
		objectiveStage = 3;
		return true;
	}

	private static boolean flag(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(BooleanJassValueVisitor.getInstance());
	}

	private static boolean triggerEnabled(final GlobalScope globals, final String name) {
		return CampaignFlowProbe.<Trigger>object(globals, name).isEnabled();
	}

	private static CQuestItem objective(final GlobalScope globals, final String name) {
		return object(globals, name);
	}

	private static <T> T object(final GlobalScope globals, final String name) {
		return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance());
	}

	private static void run(final GlobalScope globals, final String function) {
		globals.queueThread(globals.createThread(function, Collections.emptyList(), TriggerExecutionScope.EMPTY));
	}

	private static void advance(final String description) {
		System.out.println("[CampaignFlowProbe] stage " + (++stage) + ": " + description);
	}

	private static void check(final boolean condition, final String message) {
		if (!condition) throw new IllegalStateException("Campaign flow audit: " + message);
	}
}

package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Properties;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.util.JassProgram;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbilityMove;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionCheckpoint;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionStateFingerprint;

/** Opt-in separate-process reconstruction and continuation check on the retail Human01 mission. */
public final class MissionResumeProbe {
	private static final String PHASE = System.getProperty("warsmash.missionResumeAudit", "");
	private static final boolean BROADER = "broader".equals(System.getProperty("warsmash.missionResumeScenario", ""));
	private static int stage, saveTick, futureTick;
	private static long started;
	private static boolean skipRequested;
	private static int reportTick;
	private static int captureFrames;
	private static MeleeUI sceneCaptureUI;
	private static War3MapViewer previousViewer;
	private static com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData expectedHero;
	private static final Properties reference = new Properties();
	private static File directory() { return com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore.currentDirectory(); }
	public static final String SCRIPT = """
globals
    unit cpUnit = null
    unit cpAlias = null
    group cpGroup = null
    timer cpTimer = null
    item cpItem = null
    integer cpPulseCount = 0
    integer cpLocalResult = 0
    dialog cpDialog = DialogCreate()
    button cpButton = null
    integer cpDialogClicks = 0
    integer cpSaveEvents = 0
    integer cpLoadEvents = 0
    unit cpBattleTarget = null
    unit cpArcher = null
    unit cpGuard = null
    integer cpBattleDeaths = 0
    integer array cpArray
endglobals
function CheckpointAudit_Pulse takes nothing returns nothing
    set cpPulseCount = cpPulseCount + 1
    set cpArray[cpPulseCount] = cpPulseCount * 17
    call SetPlayerState(Player(1), PLAYER_STATE_RESOURCE_GOLD, GetPlayerState(Player(1), PLAYER_STATE_RESOURCE_GOLD) + 1)
    if cpPulseCount == 2 then
        call SaveGame("NativeCheckpoint")
    endif
endfunction
function CheckpointAudit_DialogClick takes nothing returns nothing
    set cpDialogClicks = cpDialogClicks + 1
    call DialogDisplay(Player(1), cpDialog, false)
endfunction
function CheckpointAudit_SaveEvent takes nothing returns nothing
    set cpSaveEvents = cpSaveEvents + 1
endfunction
function CheckpointAudit_LoadEvent takes nothing returns nothing
    set cpLoadEvents = cpLoadEvents + 1
endfunction
function CheckpointAudit_BattleDeath takes nothing returns nothing
    set cpBattleDeaths = cpBattleDeaths + 1
endfunction
function CheckpointAudit_Broader takes nothing returns nothing
    local real x = GetUnitX(udg_Arthas) + 768
    local real y = GetUnitY(udg_Arthas)
    local trigger death = CreateTrigger()
    call SetPlayerAlliance(Player(2), Player(3), ALLIANCE_PASSIVE, false)
    call SetPlayerAlliance(Player(3), Player(2), ALLIANCE_PASSIVE, false)
    call SetPlayerAlliance(Player(1), Player(3), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(3), Player(1), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(1), Player(2), ALLIANCE_PASSIVE, true)
    call SetPlayerAlliance(Player(2), Player(1), ALLIANCE_PASSIVE, true)
    set cpBattleTarget = CreateUnit(Player(2), 'hfoo', x + 300, y, 180)
    call SetUnitState(cpBattleTarget, UNIT_STATE_LIFE, 120)
    set cpArcher = CreateUnit(Player(3), 'earc', x, y, 0)
    call CreateUnit(Player(3), 'earc', x, y + 96, 0)
    set cpGuard = CreateUnit(Player(3), 'hfoo', x - 200, y + 256, 0)
    call TriggerRegisterUnitEvent(death, cpBattleTarget, EVENT_UNIT_DEATH)
    call TriggerAddAction(death, function CheckpointAudit_BattleDeath)
    call DefineStartLocation(3, x, y)
    call SetPlayerState(Player(3), PLAYER_STATE_RESOURCE_GOLD, 5000)
    call SetPlayerState(Player(3), PLAYER_STATE_RESOURCE_LUMBER, 5000)
    call StartCampaignAI(Player(3), "CheckpointAudit.ai")
    call FogEnable(false)
    call FogMaskEnable(false)
    call EnableUserControl(false)
    call EnableUserUI(false)
    call PanCameraToTimed(x + 800, y + 400, 6.0)
    call SetCameraField(CAMERA_FIELD_ZOFFSET, 350, 6.0)
    call SetCineFilterTexture("Textures\\\\Black32.blp")
    call SetCineFilterBlendMode(BLEND_MODE_BLEND)
    call SetCineFilterStartUV(0, 0, 1, 1)
    call SetCineFilterEndUV(0, 0, 1, 1)
    call SetCineFilterStartColor(20, 50, 90, 40)
    call SetCineFilterEndColor(90, 20, 50, 120)
    call SetCineFilterDuration(6.0)
    call DisplayCineFilter(true)
    call SetCinematicScene('Hpal', PLAYER_COLOR_BLUE, "Checkpoint", "The battle continues during this scene.", 6.0, 6.0)
endfunction
function CheckpointAudit_EndBroader takes nothing returns nothing
    call EndCinematicScene()
    call DisplayCineFilter(false)
    call EnableUserUI(true)
    call EnableUserControl(true)
    call ResetToGameCamera(0)
endfunction
function CheckpointAudit_Start takes nothing returns nothing
    local unit remembered
    local integer localNumber = 731
    local trigger saved = CreateTrigger()
    local trigger loaded = CreateTrigger()
    local trigger choice = CreateTrigger()
    set cpButton = DialogAddButton(cpDialog, "Continue checkpoint audit", 67)
    call TriggerRegisterDialogButtonEvent(choice, cpButton)
    call TriggerAddAction(choice, function CheckpointAudit_DialogClick)
    call DialogDisplay(Player(1), cpDialog, true)
    call TriggerRegisterGameEvent(saved, EVENT_GAME_SAVE)
    call TriggerAddAction(saved, function CheckpointAudit_SaveEvent)
    call TriggerRegisterGameEvent(loaded, EVENT_GAME_LOADED)
    call TriggerAddAction(loaded, function CheckpointAudit_LoadEvent)
    set cpUnit = CreateUnit(Player(1), 'hfoo', GetUnitX(udg_Arthas) + 128, GetUnitY(udg_Arthas) + 128, 0)
    set cpAlias = cpUnit
    set cpGroup = CreateGroup()
    call GroupAddUnit(cpGroup, cpUnit)
    call SetHeroLevel(udg_Arthas, 3, false)
    set cpItem = CreateItem('phea', GetUnitX(udg_Arthas), GetUnitY(udg_Arthas))
    call UnitAddItem(udg_Arthas, cpItem)
    set cpTimer = CreateTimer()
    call TimerStart(cpTimer, 1.0, true, function CheckpointAudit_Pulse)
    // BROADER_START
    set remembered = cpUnit
    call TriggerSleepAction(8.0)
    set cpLocalResult = localNumber
    call SetUnitUserData(remembered, localNumber)
    // BROADER_END
endfunction
""";
	private MissionResumeProbe() { }
	public static void install(final JassProgram program, final War3MapViewer viewer) {
		if (PHASE.isEmpty()) return;
        if (MissionBattleProbe.ENABLED) MissionBattleProbe.install(program, viewer);
		try { new net.warsmash.parsers.jass.SmashJassParser(new StringReader((MissionEconomyProbe.ENABLED ? MissionEconomyProbe.SCRIPT : MissionBattleProbe.ENABLED ? MissionBattleProbe.SCRIPT : "") + SCRIPT.replace("// BROADER_START", BROADER ? "call CheckpointAudit_Broader()" : MissionBattleProbe.ENABLED ? "call CheckpointAudit_LargeBattle()" : MissionEconomyProbe.ENABLED ? "call CheckpointAudit_Economy()" : "").replace("// BROADER_END", BROADER ? "call CheckpointAudit_EndBroader()" : ""))).scanAndParse("checkpoint-audit.j", program); }
		catch (final Exception error) { throw new IllegalStateException(error); }
	}
	public static boolean installAI(final JassProgram program, final String path) {
		if (PHASE.isEmpty() || !(BROADER && path.equals("CheckpointAudit.ai")
				|| MissionEconomyProbe.ENABLED && path.equals("CheckpointEconomy.ai")
                || MissionBattleProbe.ENABLED && path.equals("CheckpointBattle.ai"))) return false;
		final String script = MissionEconomyProbe.ENABLED ? MissionEconomyProbe.AI_SCRIPT : MissionBattleProbe.ENABLED ? MissionBattleProbe.AI_SCRIPT : """
globals
    integer cpAiPulse = 0
    integer cpAiLocal = 0
    integer cpAiGroupSize = 0
endglobals
function CheckpointAI_Pulse takes nothing returns nothing
    loop
        call Sleep(1.0)
        set cpAiPulse = cpAiPulse + 1
    endloop
endfunction
function main takes nothing returns nothing
    local integer remembered = 913
    local real x = GetStartLocationX(3)
    local real y = GetStartLocationY(3)
    call SetCaptainHome(0, x, y)
    call AddGuardPost('hfoo', x - 200, y + 256)
    call FillGuardPosts()
    call AddAssault(2, 'earc')
    set cpAiGroupSize = CaptainGroupSize()
    call SetUpgrade('Rhra')
    call CaptainAttack(x + 300, y)
    call StartThread(function CheckpointAI_Pulse)
    call Sleep(10.0)
    set cpAiLocal = remembered
    call CaptainGoHome()
    call ReturnGuardPosts()
endfunction
""";
		try { new net.warsmash.parsers.jass.SmashJassParser(new StringReader(script)).scanAndParse(path, program); }
		catch (final Exception error) { throw new IllegalStateException(error); }
		return true;
	}

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final com.etheller.warsmash.parsers.jass.Jass2.CommonEnvironment environment) {
		if (PHASE.isEmpty()) return;
		if (PHASE.startsWith("reject-")) return;
		final boolean initialized = environment.isInitializationComplete();
		if (started == 0) started = System.nanoTime();
		check(System.nanoTime() - started < ((MissionEconomyProbe.ENABLED || MissionBattleProbe.ENABLED) ? 240_000_000_000L : 120_000_000_000L), "Timed out at stage " + stage);
		final MissionCheckpoint checkpoint = viewer.simulation.getMissionCheckpoint();
		if (stage == 0 && viewer.simulation.getGameTurnTick() >= reportTick) {
			reportTick += 200;
			final GlobalScope globals = viewer.simulation.getGlobalScope();
			System.out.println("[MissionResumeProbe] startup tick=" + viewer.simulation.getGameTurnTick() + " initialized=" + initialized
					+ " main=" + environment.initializationStatus() + " paused=" + viewer.simulation.isGamePaused() + " local=" + viewer.getLocalPlayerIndex()
					+ " arrived=" + globals.getGlobal("udg_HasArthasArrived") + " intro=" + (MissionResumeProbe.<Trigger>object(globals, "gg_trg_Intro_Cancel") != null && MissionResumeProbe.<Trigger>object(globals, "gg_trg_Intro_Cancel").isEnabled()));
		}
		check(checkpoint.getFailure() == null, "Load failed: " + checkpoint.getFailure());
		if (!initialized || checkpoint.isRestoring()) return;
		final GlobalScope globals = viewer.simulation.getGlobalScope();
		if (PHASE.equals("read")) {
			if (stage == 0) {
				try (java.io.InputStream stream = Files.newInputStream(new File(directory(), "reference.properties").toPath())) { reference.load(stream); }
				catch (final Exception error) { throw new IllegalStateException(error); }
				futureTick = Integer.parseInt(reference.getProperty("futureTick"));
				saveTick = checkpoint.getLog().targetTick;
				check(viewer.simulation.getGameTurnTick() < futureTick, "Load advanced beyond the continuation target");
				check(object(globals, "cpUnit") == object(globals, "cpAlias"), "Unit handle alias lost");
				if (BROADER) {
					try { check(java.util.Arrays.equals(checkpoint.getLog().presentation, ui.snapshotMissionPresentation()), "Cinematic checkpoint progress differs after load"); }
					catch (final java.io.IOException error) { throw new IllegalStateException(error); }
					check(ui.getCameraManager().getPanDestination() != null, "Loading cancelled the active pan");
					check(ui.getCinematicSceneRemaining() > 0 && ui.getCineFilterElapsed() > 0, "Cinematic timers were lost");
					check(viewer.simulation.getActiveProjectileCount() > 0, "Active projectile was lost");
					captureFrames = 1; sceneCaptureUI = ui;
					System.out.println("[MissionResumeProbe] broader checkpoint restored: active projectile, pan, filter, subtitles and AI");
				}
				if (MissionEconomyProbe.ENABLED) MissionEconomyProbe.assertCheckpoint(viewer);
                if (MissionBattleProbe.ENABLED) { MissionBattleProbe.assertCheckpoint(viewer); captureFrames = 1; sceneCaptureUI = ui; }
				stage = 3;
				System.out.println("[MissionResumeProbe] saved mission reconstructed; following live continuation");
			}
			else continueAfterResume(viewer, ui, checkpoint, globals);
			return;
		}
		if (MissionEconomyProbe.ENABLED && (stage == 2 || stage == 3) && viewer.simulation.getGameTurnTick() >= reportTick) {
			reportTick = viewer.simulation.getGameTurnTick() + 200;
			System.out.println("[MissionResumeProbe] economy " + MissionEconomyProbe.describe(viewer));
		}
		if (MissionBattleProbe.ENABLED && (stage == 2 || stage == 3) && viewer.simulation.getGameTurnTick() >= reportTick) {
            reportTick = viewer.simulation.getGameTurnTick() + 200;
            System.out.println("[MissionResumeProbe] large battle " + MissionBattleProbe.describe(viewer));
        }
		if (BROADER && stage == 2 && viewer.simulation.getGameTurnTick() >= reportTick) {
			reportTick = viewer.simulation.getGameTurnTick() + 100;
			final CUnit target = object(globals, "cpBattleTarget"), archer = object(globals, "cpArcher");
			System.out.println("[MissionResumeProbe] battle tick=" + viewer.simulation.getGameTurnTick() + " projectiles=" + viewer.simulation.getActiveProjectileCount()
					+ " target=" + target.getX() + "," + target.getY() + " life=" + target.getLife() + " archer=" + archer.getX() + "," + archer.getY()
					+ " behavior=" + archer.getCurrentBehavior() + " attacks=" + archer.getCurrentAttacks().size() + " paused=" + archer.isPaused() + " AIgroup=" + viewer.simulation.getAiEnvironment(3).getGlobalScope().getGlobal("cpAiGroupSize") + " AIpulse=" + viewer.simulation.getAiEnvironment(3).getGlobalScope().getGlobal("cpAiPulse"));
		}
		if (stage == 0) {
			if (!globals.getGlobal("udg_HasArthasArrived").visit(BooleanJassValueVisitor.getInstance())) {
				final Trigger trigger = object(globals, "gg_trg_Intro_Cancel");
				if (!skipRequested && trigger != null && trigger.isEnabled()) { skipRequested = true; ui.keyDown(Input.Keys.ESCAPE); }
				return;
			}
			checkpoint.runScript("CheckpointAudit_Start");
			stage = 1;
		}
		else if (stage == 1 && object(globals, "cpUnit") != null) {
			MissionResumeProbe.<com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton>object(globals, "cpButton").getButtonFrame().onClick(Input.Buttons.LEFT);
			final CUnit unit = object(globals, "cpUnit");
			final CAbilityMove move = unit.getAbilities().stream().filter(ability -> ability instanceof CAbilityMove)
					.map(ability -> (CAbilityMove) ability).findFirst().orElseThrow();
			final CPlayerUnitOrderExecutor orders = new CPlayerUnitOrderExecutor(viewer.simulation, viewer.getLocalPlayerIndex());
			orders.issuePointOrder(unit.getHandleId(), move.getHandleId(), OrderIds.move, unit.getX() + 1024, unit.getY() + 512, false);
			orders.issuePointOrder(unit.getHandleId(), move.getHandleId(), OrderIds.move, unit.getX(), unit.getY(), true);
			saveTick = viewer.simulation.getGameTurnTick() + Integer.getInteger("warsmash.missionResumeSaveOffset", 65);
			futureTick = saveTick + (MissionEconomyProbe.ENABLED ? MissionEconomyProbe.CONTINUATION_TICKS : MissionBattleProbe.ENABLED ? MissionBattleProbe.CONTINUATION_TICKS : 240);
			stage = 2;
		}
	}
	public static void afterTick(final War3MapViewer viewer, final MeleeUI ui) {
		if (PHASE.isEmpty() || viewer.simulation.getMissionCheckpoint().isRestoring()) return;
		final int tick = viewer.simulation.getGameTurnTick();
		if (PHASE.equals("write") && stage == 2 && (BROADER ? tick >= saveTick && viewer.simulation.getActiveProjectileCount() > 0
				: MissionEconomyProbe.ENABLED ? tick >= saveTick && MissionEconomyProbe.ready(viewer)
                : MissionBattleProbe.ENABLED ? tick >= saveTick && MissionBattleProbe.ready(viewer) : tick == saveTick)) {
			((com.etheller.warsmash.parsers.fdf.frames.SimpleButtonFrame) viewer.getGameUI().getFrameByName("UpperButtonBarMenuButton", 0)).onClick(Input.Buttons.LEFT);
            ((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame) viewer.getGameUI().getFrameByName("SaveGameButton", 0)).onClick(Input.Buttons.LEFT);
			if (BROADER) {
				saveTick = tick; futureTick = tick + 240;
				check(ui.getCameraManager().getPanDestination() != null, "Checkpoint must be taken during the pan");
				check(ui.getCinematicSceneRemaining() > 0, "Checkpoint must be taken during the transmission");
			}
			if (MissionEconomyProbe.ENABLED) {
				saveTick = tick; futureTick = tick + MissionEconomyProbe.CONTINUATION_TICKS;
				MissionEconomyProbe.assertCheckpoint(viewer);
			}
			if (MissionBattleProbe.ENABLED) {
                saveTick = tick; futureTick = tick + MissionBattleProbe.CONTINUATION_TICKS;
                MissionBattleProbe.assertCheckpoint(viewer);
            }
			final CGameSave saved = CGameSave.tryLoad(new File(directory(), "QuickSave.w3s"));
			check(saved != null && saved.checkpoint != null && saved.checkpoint.targetTick == saveTick, "Quick Save did not capture the completed tick");
			check(CGameSave.tryLoad(new File(directory(), "NativeCheckpoint.w3s")) != null, "Deferred native SaveGame failed");
			// The reference branch receives the same load notification as the resumed branch.
			viewer.simulation.fireGameEvent(com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3.EVENT_GAME_LOADED);
			stage = 3;
			if (BROADER || MissionBattleProbe.ENABLED) {
				((com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame) viewer.getGameUI().getFrameByName("ReturnButton", 0)).onClick(Input.Buttons.LEFT);
				captureFrames = 1; sceneCaptureUI = ui;
			}
			System.out.println("[MissionResumeProbe] saved tick=" + tick + " state=" + saved.checkpoint.stateFingerprint);
		}
		if (stage == 3 && (tick == saveTick + 1 || tick == saveTick + 2 || tick == saveTick + 10)) MissionStateFingerprint.of(viewer.simulation);
		if (stage == 3 && tick == futureTick) {
			final GlobalScope globals = viewer.simulation.getGlobalScope();
			check(globals.getGlobal("cpLocalResult").visit(IntegerJassValueVisitor.getInstance()) == 731, "Sleeping script lost its local variable");
			check(globals.getGlobal("cpDialogClicks").visit(IntegerJassValueVisitor.getInstance()) == 1, "Dialog input lost or duplicated");
			check(globals.getGlobal("cpSaveEvents").visit(IntegerJassValueVisitor.getInstance()) == 2, "Save notification lost or duplicated");
			check(globals.getGlobal("cpLoadEvents").visit(IntegerJassValueVisitor.getInstance()) == 1, "Load notification lost or duplicated");
			final CUnit unit = object(globals, "cpUnit");
			check(unit.getTriggerEditorCustomValue() == 731, "Sleeping local unit handle no longer identifies the live unit");
			check(viewer.simulation.getUnit(unit.getHandleId()) == unit, "Script handle detached from world");
			if (BROADER) {
				final GlobalScope ai = viewer.simulation.getAiEnvironment(3).getGlobalScope();
				check(ai.getGlobal("cpAiLocal").visit(IntegerJassValueVisitor.getInstance()) == 913, "Sleeping AI lost local variables");
				check(ai.getGlobal("cpAiPulse").visit(IntegerJassValueVisitor.getInstance()) >= 8, "AI worker thread did not continue");
				check(ai.getGlobal("cpAiGroupSize").visit(IntegerJassValueVisitor.getInstance()) == 2, "AI assault group missing");
				check(MissionResumeProbe.<CUnit>object(globals, "cpBattleTarget").isDead(), "Battle did not kill its target; remaining life=" + MissionResumeProbe.<CUnit>object(globals, "cpBattleTarget").getLife());
				check(globals.getGlobal("cpBattleDeaths").visit(IntegerJassValueVisitor.getInstance()) == 1, "Death event lost or duplicated");
				check(ui.getCameraManager().getPanDestination() == null && !ui.isCineFilterDisplayed() && ui.getCinematicSceneRemaining() == 0, "Cinematic did not finish");
				System.out.println("[MissionResumeProbe] broader continuation verified: combat death, AI sleeping locals/worker/captain/guards and cinematic completion");
			}
			if (MissionEconomyProbe.ENABLED) MissionEconomyProbe.assertContinuation(viewer);
            if (MissionBattleProbe.ENABLED) MissionBattleProbe.assertContinuation(viewer);
			final String fingerprint = MissionStateFingerprint.of(viewer.simulation);
			if (PHASE.equals("write")) {
				reference.setProperty("futureTick", Integer.toString(futureTick));
				reference.setProperty("futureState", fingerprint);
				try (java.io.OutputStream stream = Files.newOutputStream(new File(directory(), "reference.properties").toPath())) { reference.store(stream, "Uninterrupted mission continuation"); }
				catch (final Exception error) { throw new IllegalStateException(error); }
			}
			else check(fingerprint.equals(reference.getProperty("futureState")), "Resumed continuation differs from uninterrupted mission");
			stage = 4;
			System.out.println("[MissionResumeProbe] " + PHASE + " continuation matched tick=" + tick + " state=" + fingerprint);
			if (PHASE.equals("write")) {
				System.out.println("[MissionResumeProbe] write complete");
				Gdx.app.exit();
			}
			else {
				try { viewer.simulation.getMissionCheckpoint().save(new File(directory(), "Resumed.w3s")); }
				catch (final java.io.IOException error) { throw new IllegalStateException(error); }
				viewer.simulation.getMissionCheckpoint().runScript("Trig_Defeat_Cheat_Actions");
			}
		}
	}
	private static void continueAfterResume(final War3MapViewer viewer, final MeleeUI ui,
			final MissionCheckpoint checkpoint, final GlobalScope globals) {
		final com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog dialog = ui.getVisibleScriptDialog();
		switch (stage) {
		case 4:
			if (dialog == null) return;
			check(dialog.getButtons().size() == 4, "Retail defeat choices missing");
			check(!com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get().isMissionAvailable(1, 1), "Defeat unlocked next chapter");
			dialog.getButtons().get(2).getButtonFrame().onClick(Input.Buttons.LEFT);
			stage = 5;
			break;
		case 5:
			if (dialog == null || !dialog.getScriptDialogTextFrame().getText().startsWith("Load Game")) return;
			previousViewer = viewer;
			// Sorted slots are NativeCheckpoint, QuickSave, Resumed (then corrupt audit copies).
			check(dialog.getButtons().size() >= 3, "Resaved mission missing from picker");
			dialog.getButtons().get(2).getButtonFrame().onClick(Input.Buttons.LEFT);
			stage = 6;
			break;
		case 6:
			if (viewer == previousViewer) return;
			final CUnit hero = object(globals, "udg_Arthas");
			check(hero != null && !hero.isDead(), "Defeat-to-load did not restore the hero");
			check(checkpoint.getLog().targetTick == futureTick, "Picker did not load the resaved mission");
			check(globals.getGlobal("cpLocalResult").visit(IntegerJassValueVisitor.getInstance()) == 731, "Resaving lost resumed script state");
			check(!globals.getGlobal("udg_GAMEOVER").visit(BooleanJassValueVisitor.getInstance()), "Load retained post-checkpoint defeat state");
			check(object(globals, "cpUnit") == object(globals, "cpAlias"), "Reload lost handle alias");
			expectedHero = CGameSave.snapshotUnit(hero);
			previousViewer = viewer;
			checkpoint.runScript("Trig_Next_Level_Prep_Actions");
			checkpoint.runScript("Trig_Next_Level_Run_Actions");
			stage = 7;
			System.out.println("[MissionResumeProbe] defeat menu loaded checkpoint; retail victory queued");
			break;
		case 7:
			if (dialog == null) return;
			check(globals.getGlobal("cpLoadEvents").visit(IntegerJassValueVisitor.getInstance()) == 2, "Resave/reload lost load notifications");
			check(dialog.getButtons().size() == 2, "Retail victory choices missing");
			dialog.getButtons().get(0).getButtonFrame().onClick(Input.Buttons.LEFT);
			stage = 8;
			break;
		case 8:
			if (!"Victory".equals(ui.getScoreDialogTitle())) return;
			ui.keyDown(Input.Keys.C);
			stage = 9;
			break;
		case 9:
			if (viewer == previousViewer) return;
			check(viewer.getCurrentMapPath().toLowerCase().contains("human02"), "Victory did not load Human02");
			final CUnit nextHero = object(globals, "udg_Arthas");
			if (nextHero == null) return;
			final com.etheller.warsmash.viewer5.handlers.w3x.simulation.StoredUnitData restored = CGameSave.snapshotUnit(nextHero);
			check(expectedHero.xp == restored.xp && expectedHero.skillPoints == restored.skillPoints, "Carryover lost hero progress");
			check(expectedHero.properName.equals(restored.properName) && expectedHero.strengthBase == restored.strengthBase, "Carryover replaced restored hero");
			for (int i = 0; i < expectedHero.items.length; i++) {
				check((expectedHero.items[i] == null) == (restored.items[i] == null), "Carryover lost inventory slot " + i);
				if (expectedHero.items[i] != null) check(expectedHero.items[i].typeId.equals(restored.items[i].typeId)
						&& expectedHero.items[i].charges == restored.items[i].charges, "Carryover changed item " + i);
			}
			final com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore progress = com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CampaignProgressStore.get();
			check(progress.isMissionAvailable(1, 1) && !progress.isMissionAvailable(1, 2), "Restored victory changed chapter gating");
			System.out.println("[MissionResumeProbe] Human02 carryover verified after defeat-to-load and victory");
			stage = 10;
			ui.customVictory(false);
			break;
		default: break;
		}
	}
	public static void afterUIRender() {
		if (!(BROADER || MissionBattleProbe.ENABLED) || captureFrames == 0 || ++captureFrames < 3) return;
		captureFrames = 0;
		if (BROADER) check(sceneCaptureUI.getCinematicSceneRemaining() > 0, "Loading time expired the saved transmission before its first live frames");
		if (BROADER) check(sceneCaptureUI.getCameraManager().getPanDestination() != null, "Loading time completed the saved pan before its first live frames");
		final com.badlogic.gdx.graphics.Pixmap pixels = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(
				0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		final com.badlogic.gdx.graphics.PixmapIO.PNG writer = new com.badlogic.gdx.graphics.PixmapIO.PNG();
		try {
			writer.setFlipY(true);
			writer.write(Gdx.files.absolute(new File(directory(), "scene-" + PHASE + ".png").getAbsolutePath()), pixels);
		}
		catch (final java.io.IOException error) { throw new IllegalStateException(error); }
		finally { writer.dispose(); pixels.dispose(); }
	}

	public static void afterMenuRender() {
		if (PHASE.startsWith("reject-") && stage == 12) {
			stage = 13;
			System.out.println("[MissionResumeProbe] " + PHASE + " complete; returned to menu");
			Gdx.app.exit();
		}
		if (PHASE.equals("read") && stage == 10) {
			stage = 11;
			System.out.println("[MissionResumeProbe] read complete");
			Gdx.app.exit();
		}
	}
	public static void afterLoadFailure(final String error) {
		if (!PHASE.startsWith("reject-")) return;
		check(error != null && error.contains(PHASE.equals("reject-engine") ? "different scripts or engine binaries" : "Mission reconstruction diverged"), "Wrong load failure: " + error);
		stage = 12;
		System.out.println("[MissionResumeProbe] rejected checkpoint: " + error);
	}
	private static <T> T object(final GlobalScope globals, final String name) { return globals.getGlobal(name).visit(ObjectJassValueVisitor.<T>getInstance()); }
	private static void check(final boolean value, final String message) { if (!value) throw new IllegalStateException("Mission resume audit: " + message); }
}

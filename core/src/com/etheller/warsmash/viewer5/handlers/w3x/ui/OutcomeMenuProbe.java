package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collections;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.CheckBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ScrollBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.TextAreaFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerGameResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameSpeed;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;

/** Bounded desktop audit for result choices, keyboard modality, and rendered layout. */
public final class OutcomeMenuProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.outcomeMenuAudit");
	private static final String MOVIE = System.getProperty("warsmash.outcomeMenuMovie", "");
	private static final boolean COMPLETE_MOVIE = Boolean.getBoolean("warsmash.outcomeMenuCompleteMovie");
	private static int stage;
	private static long started;
	private static War3MapViewer defeatedViewer;
	private static String pendingCapture;
	private static String lastCapture;
	private static int capturedFrames;
	private static int sizeIndex;
	private static int loadStage;
	private static boolean introSkipRequested;
	private static Vector2 restartCameraTarget;
	private static int pauseStage;
	private static int pausedTick;
	private static int pausedFrames;
	private static CUnit firstWorker;
	private static CUnit secondWorker;
	private static boolean idleWorkerVerified;
	private static CUnit firstTownHall;
	private static CUnit secondTownHall;
	private static CUnit secondHero;
	private static boolean cameraHotkeysVerified;
	private static boolean groupHoldIssued;
	private static boolean subgroupStopIssued;
	private static boolean inventoryHotkeyIssued;
	private static float potionStartingLife;
	private static int healthBarStage;
	private static int healthBarBaseline;
	private static int friendlyHealthBars;
	private static int hostileHealthBars;
	private static int heldHealthBarKey;
	private static CUnit healthBarEnemy;
	private static Input desktopInput;
	private static int helpStage;
	private static int optionsStage;
	private static int saveStage;
	private static War3MapViewer saveBrowserViewer;
	private static int namedRestoreTick;
	private static int speedSampleStartTick;
	private static float speedSampleSeconds;
	private static int movieStage;
	private static float originalMovieDuration;
	private static float lastMovieFrame;
	private static float lastMovieAudio;
	private static boolean movieFrameCaptured;
	private static final int[][] SIZES = {{960, 540}, {800, 600}, {600, 800}};

	private OutcomeMenuProbe() { }

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED) return;
		if (started == 0) started = System.nanoTime();
		check(System.nanoTime() - started < (COMPLETE_MOVIE ? 390_000_000_000L : 150_000_000_000L),
				"Timeout at stage " + stage);
		if (!initialized) return;
		final GlobalScope globals = viewer.simulation.getGlobalScope();
		final CUnit hero = globals.getGlobal("udg_Arthas").visit(ObjectJassValueVisitor.<CUnit>getInstance());
		final CScriptDialog dialog = ui.getVisibleScriptDialog();
		switch (stage) {
		case 0:
			if (hero == null) return;
			if (!globals.getGlobal("udg_HasArthasArrived").visit(BooleanJassValueVisitor.getInstance())) {
				final Trigger skip = globals.getGlobal("gg_trg_Intro_Cancel").visit(ObjectJassValueVisitor.<Trigger>getInstance());
				if (!introSkipRequested && skip.isEnabled()) {
					introSkipRequested = true;
					ui.keyDown(Input.Keys.ESCAPE);
				}
				return;
			}
			check(result(viewer) == CPlayerGameResult.NEUTRAL.ordinal(), "Unfinished mission recorded as victory");
			if (!exerciseNamedSave(viewer, ui)) return;
			if (!exerciseIdleWorkers(viewer, ui, hero)) return;
			if (!exerciseCameraHotkeys(viewer, ui, hero)) return;
			if (!exerciseHealthBarHotkeys(viewer, ui, hero)) return;
			if (!exerciseMissionHelp(ui)) return;
			if (!exerciseMissionOptions(viewer, ui)) return;
			if (!exerciseMovie(ui)) return;
			if (!exercisePause(viewer, ui)) return;
			ui.keyDown(Input.Keys.LEFT);
			ui.requestRestartLevel(viewer.getCurrentMapPath(), true);
			restartCameraTarget = new Vector2(ui.getCameraManager().target.x, ui.getCameraManager().target.y);
			advance("restart confirmation opened");
			break;
		case 1:
			if (dialog == null || !capture("restart-confirmation", dialog)) return;
			check(dialog.getButtons().size() == 2, "Restart/Cancel choices missing");
			check(restartCameraTarget.epsilonEquals(ui.getCameraManager().target.x,
					ui.getCameraManager().target.y, 0.001f), "Held camera input moved beneath a modal");
			ui.keyUp(Input.Keys.LEFT);
			ui.keyDown(Input.Keys.ESCAPE);
			check(ui.getVisibleScriptDialog() == null && !viewer.simulation.isGamePaused(), "Cancel did not resume mission");
			globals.queueThread(globals.createThread("Trig_Defeat_Cheat_Actions", Collections.emptyList(), TriggerExecutionScope.EMPTY));
			advance("restart cancelled; retail defeat requested");
			break;
		case 2:
			if (dialog == null) return;
			if (loadStage == 1) {
				if (!capture("load-unavailable", dialog)) return;
				check(dialog.getScriptDialogTextFrame().getText().startsWith("Load Game"), "Load failed silently");
				ui.keyDown(Input.Keys.B);
				loadStage = 2;
				return;
			}
			check(dialog.getButtons().size() == 4, "Retail defeat choices missing");
			check(!dialog.getScriptDialogTextFrame().getText().startsWith("TRIGSTR_"), "Defeat message was not localized");
			if (sizeIndex < SIZES.length) {
				if (!capture("defeat-choices-" + sizeIndex, dialog)) return;
				if (++sizeIndex < SIZES.length) {
					check(Gdx.graphics.setWindowedMode(SIZES[sizeIndex][0], SIZES[sizeIndex][1]), "Resize failed");
					return;
				}
			}
			if (loadStage == 0) {
				click(ui, dialog.getButtons().get(2));
				loadStage = 1;
				return;
			}
			check(viewer.simulation.isGamePaused(), "Load Back did not restore paused defeat menu");
			check(result(viewer) == CPlayerGameResult.DEFEAT.ordinal(), "Retail defeat result was not recorded");
			ui.keyDown(Input.Keys.ESCAPE);
			check(dialog.isVisible(), "Escape dismissed the defeat menu");
			check(ui.touchDown(1, 1, 1, Input.Buttons.RIGHT), "Outside click leaked through modal");
			check(ui.touchUp(1, 1, 1, Input.Buttons.RIGHT), "Outside release leaked through modal");
			click(ui, dialog.getButtons().get(3));
			sizeIndex = 0;
			resize(sizeIndex);
			advance("retail defeat Quit selected");
			break;
		case 3:
			if (ui.getScoreDialogTitle() == null) return;
			check(ui.isResultBackdropFullscreen(), "Defeat backdrop leaves window edges uncovered");
			if (dialog == null || !captureResultSizes("defeat-result", dialog)) return;
			check("Defeat".equals(ui.getScoreDialogTitle()), "Defeat Quit showed victory");
			check(dialog.getButtons().size() == 2, "Defeat result missing retry/quit");
			check(!ui.hasVisibleResultOverlays(), "Gameplay overlays cover defeat result");
			defeatedViewer = viewer;
			ui.keyDown(Input.Keys.ENTER);
			advance("defeat result Enter selected Restart");
			break;
		case 4:
			if (viewer == defeatedViewer || hero == null) return;
			check(!hero.isDead(), "Restart hero is dead");
			check(result(viewer) == CPlayerGameResult.NEUTRAL.ordinal(), "Restart retained defeat outcome");
			globals.queueThread(globals.createThread("Trig_Victory_Cheat_Actions", Collections.emptyList(), TriggerExecutionScope.EMPTY));
			advance("restarted mission; retail victory requested");
			break;
		case 5:
			if (dialog == null || !capture("victory-choices", dialog)) return;
			check(result(viewer) == CPlayerGameResult.VICTORY.ordinal(), "Retail victory result was not recorded");
			check(dialog.getButtons().size() == 2, "Retail victory choices missing");
			click(ui, dialog.getButtons().get(1));
			sizeIndex = 0;
			resize(sizeIndex);
			advance("retail victory Quit selected");
			break;
		case 6:
			if (ui.getScoreDialogTitle() != null) {
				check(ui.isResultBackdropFullscreen(), "Victory backdrop leaves window edges uncovered");
				ui.showInterface(true, 0);
				check(!ui.isGameInterfaceVisible(), "Script restored HUD over result menu");
				check(!ui.isPortraitVisible(), "Script restored portrait over result menu");
				ui.displayTimedText(0, 0, 60, "This late quest message must stay hidden");
				ui.displayCineFilter(true);
				check(!ui.hasVisibleResultOverlays(), "Late script overlays cover result menu");
			}
			if (dialog == null || !captureResultSizes("victory-result", dialog)) return;
			check("Victory".equals(ui.getScoreDialogTitle()), "Victory Quit lost outcome");
			final String summary = dialog.getScriptDialogTextFrame().getText();
			check(!summary.startsWith("Human01"), "Result uses internal map identifier");
			check(summary.contains(ui.scoreText("COLON_DIFFICULTY", "Difficulty:") + " " + ui.scoreText("NORMAL", "Normal"))
					&& summary.contains(ui.scoreText("RESOURCES_COLUMN0", "Gold Mined"))
					&& summary.contains(ui.scoreText("RESOURCES_COLUMN1", "Lumber Harvested"))
					&& summary.contains(ui.scoreText("UNIT_COLUMN0", "Units Produced"))
					&& summary.contains(ui.scoreText("UNIT_COLUMN1", "Units Killed"))
					&& summary.contains(ui.scoreText("UNIT_COLUMN3", "Buildings Razed"))
					&& summary.contains(ui.scoreText("UNIT_COLUMN4", "Largest Army"))
					&& summary.contains(ui.scoreText("HEROES_COLUMN1", "Heroes Killed")), "Localized result snapshot missing");
			System.out.println("[OutcomeMenuProbe] summary: " + summary.replace('\n', ' '));
			ui.keyDown(Input.Keys.ENTER);
			advance("victory result Continue selected");
			break;
		default:
			break;
		}
	}

	private static int result(final War3MapViewer viewer) {
		return viewer.simulation.getPlayer(viewer.getLocalPlayerIndex()).getPlayerState(viewer.simulation, CPlayerState.GAME_RESULT);
	}

	private static void resize(final int index) {
		check(Gdx.graphics.setWindowedMode(SIZES[index][0], SIZES[index][1]), "Resize failed");
	}

	private static boolean captureResultSizes(final String name, final CScriptDialog dialog) {
		if (!capture(sizeIndex == SIZES.length - 1 ? name : name + "-" + sizeIndex, dialog)) return false;
		if (++sizeIndex < SIZES.length) {
			resize(sizeIndex);
			return false;
		}
		return true;
	}

	private static boolean exerciseIdleWorkers(final War3MapViewer viewer, final MeleeUI ui, final CUnit hero) {
		if (idleWorkerVerified) return true;
		if (!ui.isUserControlEnabled()) return false;
		final var game = viewer.simulation;
		if (firstWorker == null) {
			final War3ID peasant = War3ID.fromString("hpea");
			firstWorker = game.createUnitSimple(peasant, viewer.getLocalPlayerIndex(), hero.getX() + 100, hero.getY(), 0);
			secondWorker = game.createUnitSimple(peasant, viewer.getLocalPlayerIndex(), hero.getX() + 180, hero.getY(), 0);
			return false;
		}
		if (!ui.idleWorkerControl().isVisible()) return false;
		click(ui, ui.idleWorkerControl());
		check(ui.getSelectedUnit() != null && ui.getSelectedUnit().getSimulationUnit() == firstWorker,
				"Clicking Idle Workers did not select the first visible worker");
		ui.keyDown(Input.Keys.F8);
		check(ui.getSelectedUnit().getSimulationUnit() == secondWorker, "F8 did not select the next idle worker");
		ui.keyDown(Input.Keys.GRAVE);
		check(ui.getSelectedUnit().getSimulationUnit() == firstWorker, "Tilde did not wrap idle worker selection");
		check(ui.getScriptSelectedUnits().equals(java.util.List.of(firstWorker)), "The idle worker is not the active group");
		check(Math.abs(ui.getCameraManager().target.x - firstWorker.getX()) < 0.001f
				&& Math.abs(ui.getCameraManager().target.y - firstWorker.getY()) < 0.001f,
				"Idle Workers did not center the camera");
		ui.scriptClearSelection();
		game.removeUnit(firstWorker);
		game.removeUnit(secondWorker);
		idleWorkerVerified = true;
		System.out.println("[OutcomeMenuProbe] Idle Workers button, F8, and Tilde cycled the local workers");
		return true;
	}

	private static boolean exerciseCameraHotkeys(final War3MapViewer viewer, final MeleeUI ui, final CUnit hero) {
		if (cameraHotkeysVerified) return true;
		final var game = viewer.simulation;
		if (firstTownHall == null) {
			final int player = viewer.getLocalPlayerIndex();
			firstTownHall = game.createUnitSimple(War3ID.fromString("htow"), player, hero.getX() + 420, hero.getY(), 0);
			secondTownHall = game.createUnitSimple(War3ID.fromString("hkee"), player, hero.getX() + 780, hero.getY(), 0);
			secondHero = game.createUnitSimple(War3ID.fromString("Hamg"), player, hero.getX(), hero.getY() + 200, 0);
			return false;
		}
		if (!game.getUnits().contains(secondTownHall)) return false;
		if (inventoryHotkeyIssued) {
			if (secondHero.getInventoryData().getItemInSlot(0) != null) return false;
			check(secondHero.getLife() > potionStartingLife + 50, "Numpad 7 did not use the first-slot healing potion");
			ui.scriptClearSelection();
			game.removeUnit(firstTownHall);
			game.removeUnit(secondTownHall);
			game.removeUnit(secondHero);
			cameraHotkeysVerified = true;
			System.out.println("[OutcomeMenuProbe] hero and camera hotkeys, group recall and Hold Position, Ctrl subgroup orders, Shift-Tab, and Numpad 7 passed");
			return true;
		}
		if (groupHoldIssued) {
			if (!subgroupStopIssued) {
				if (!hero.isHoldingPosition() || !secondHero.isHoldingPosition()) return false;
				final Input original = Gdx.input;
				final var settings = OptionsSettingsStore.get();
				final boolean saved = settings.isSubgroupOrderModifier();
				try {
					settings.setSubgroupOrderModifier(true);
					Gdx.input = keyOverride(original, () -> Input.Keys.CONTROL_LEFT);
					check(ui.keyDown(Input.Keys.S), "Ctrl-S did not dispatch Stop to the active subgroup");
				}
				finally {
					Gdx.input = original;
					settings.setSubgroupOrderModifier(saved);
				}
				subgroupStopIssued = true;
				return false;
			}
			if (hero.isHoldingPosition()) return false;
			check(secondHero.isHoldingPosition(), "The unhighlighted subgroup received Ctrl-Stop");
			ui.keyDown(Input.Keys.F2);
			secondHero.setLife(game, secondHero.getMaximumLife() * 0.5f);
			potionStartingLife = secondHero.getLife();
			final var potion = game.createItem(War3ID.fromString("phea"), secondHero.getX(), secondHero.getY());
			check(secondHero.getInventoryData().giveItem(game, secondHero, potion, false) == 0,
					"The healing potion did not occupy the first inventory slot");
			check(ui.keyDown(Input.Keys.NUMPAD_7), "Numpad 7 did not dispatch the inventory command");
			inventoryHotkeyIssued = true;
			return false;
		}
		final Vector2 before = new Vector2(ui.getCameraManager().target.x, ui.getCameraManager().target.y);
		ui.keyDown(Input.Keys.F1);
		check(ui.getSelectedUnit() != null && ui.getSelectedUnit().getSimulationUnit() == hero, "F1 did not select Arthas");
		check(ui.getScriptSelectedUnits().equals(java.util.List.of(hero)), "The first hero is not the active group");
		check(before.epsilonEquals(ui.getCameraManager().target.x, ui.getCameraManager().target.y, 0.001f),
				"First F1 moved the camera");
		ui.keyDown(Input.Keys.F1);
		checkCameraAt(ui, hero, "F1");
		ui.keyDown(Input.Keys.F2);
		check(ui.getSelectedUnit().getSimulationUnit() == secondHero, "F2 did not select the second hero");
		checkCameraAt(ui, hero, "First F2");
		ui.keyDown(Input.Keys.F2);
		checkCameraAt(ui, secondHero, "F2");
		ui.keyDown(Input.Keys.BACKSPACE);
		checkCameraAt(ui, firstTownHall, "First Backspace");
		ui.keyDown(Input.Keys.BACKSPACE);
		checkCameraAt(ui, secondTownHall, "Second Backspace");
		ui.keyDown(Input.Keys.BACKSPACE);
		checkCameraAt(ui, firstTownHall, "Backspace wrap");
		check(ui.getSelectedUnit().getSimulationUnit() == secondHero, "Backspace changed unit selection");
		ui.pingMinimap(firstTownHall.getX(), firstTownHall.getY(), 1, 1, 0, 0);
		ui.pingMinimap(secondTownHall.getX(), secondTownHall.getY(), 1, 1, 0, 0);
		ui.keyDown(Input.Keys.SPACE);
		checkCameraAt(ui, secondTownHall, "Space on the latest alert");
		ui.keyDown(Input.Keys.SPACE);
		checkCameraAt(ui, firstTownHall, "Space on the previous alert");
		ui.scriptSelectGroup(java.util.List.of(hero, secondHero, firstTownHall));
		final Input original = Gdx.input;
		try {
			Gdx.input = keyOverride(original, () -> Input.Keys.CONTROL_LEFT);
			ui.keyDown(Input.Keys.NUM_1);
		}
		finally {
			Gdx.input = original;
		}
		ui.scriptClearSelection();
		ui.keyDown(Input.Keys.NUM_1);
		check(ui.getScriptSelectedUnits().equals(java.util.List.of(hero, secondHero, firstTownHall)),
				"Number 1 did not recall the assigned group");
		checkCameraAt(ui, firstTownHall, "First group recall");
		ui.keyDown(Input.Keys.NUM_1);
		final float centerX = (hero.getX() + secondHero.getX() + firstTownHall.getX()) / 3;
		final float centerY = (hero.getY() + secondHero.getY() + firstTownHall.getY()) / 3;
		check(ui.getCameraManager().target.epsilonEquals(centerX, centerY, ui.getCameraManager().target.z, 0.01f),
				"Second group recall did not center the camera on the group");
		ui.keyDown(Input.Keys.TAB);
		check(ui.getSelectedUnit().getSimulationUnit() == secondHero, "Tab did not advance the subgroup");
		try {
			Gdx.input = keyOverride(original, () -> Input.Keys.SHIFT_LEFT);
			ui.keyDown(Input.Keys.TAB);
		}
		finally {
			Gdx.input = original;
		}
		check(ui.getSelectedUnit().getSimulationUnit() == hero, "Shift-Tab did not go back a subgroup");
		check(ui.keyDown(Input.Keys.H), "H did not dispatch Hold Position");
		groupHoldIssued = true;
		return false;
	}

	private static Input keyOverride(final Input input, final java.util.function.IntSupplier heldKey) {
		return (Input) Proxy.newProxyInstance(Input.class.getClassLoader(), new Class<?>[] { Input.class },
				(proxy, method, args) -> "isKeyPressed".equals(method.getName())
						? heldKey.getAsInt() == (Integer) args[0] : method.invoke(input, args));
	}

	private static void checkCameraAt(final MeleeUI ui, final CUnit unit, final String input) {
		check(Math.abs(ui.getCameraManager().target.x - unit.getX()) < 0.001f
				&& Math.abs(ui.getCameraManager().target.y - unit.getY()) < 0.001f,
				input + " did not center the camera on " + unit.getTypeId());
	}

	private static boolean exerciseHealthBarHotkeys(final War3MapViewer viewer, final MeleeUI ui, final CUnit hero) {
		switch (healthBarStage) {
		case 0:
			ui.keyDown(Input.Keys.F1);
			healthBarEnemy = viewer.simulation.createUnitSimple(War3ID.fromString("hpea"),
					WarsmashConstants.MAX_PLAYERS - 4, hero.getX() + 180, hero.getY(), 0);
			healthBarEnemy.setPaused(true);
			return nextHealthBarStage();
		case 1:
			if (!viewer.simulation.getUnits().contains(healthBarEnemy)
					|| !healthBarEnemy.isVisible(viewer.simulation, viewer.getLocalPlayerIndex())) return false;
			healthBarBaseline = ui.visibleWorldStatusBarCount();
			desktopInput = Gdx.input;
			Gdx.input = keyOverride(desktopInput, () -> heldHealthBarKey);
			heldHealthBarKey = Input.Keys.LEFT_BRACKET;
			return nextHealthBarStage();
		case 2:
			friendlyHealthBars = ui.visibleWorldStatusBarCount() - healthBarBaseline;
			check(friendlyHealthBars > 0, "Left bracket did not show friendly health bars");
			heldHealthBarKey = Input.Keys.RIGHT_BRACKET;
			return nextHealthBarStage();
		case 3:
			hostileHealthBars = ui.visibleWorldStatusBarCount() - healthBarBaseline;
			check(hostileHealthBars > 0, "Right bracket did not show enemy health bars");
			heldHealthBarKey = Input.Keys.ALT_LEFT;
			return nextHealthBarStage();
		case 4:
			check(ui.visibleWorldStatusBarCount() == healthBarBaseline + friendlyHealthBars + hostileHealthBars,
					"Alt did not show both teams' health bars");
			ui.keyDown(Input.Keys.F10);
			return nextHealthBarStage();
		case 5:
			check(ui.visibleWorldStatusBarCount() <= healthBarBaseline, "Health bars appeared through the mission menu");
			ui.keyDown(Input.Keys.ESCAPE);
			Gdx.input = desktopInput;
			return nextHealthBarStage();
		case 6:
			check(ui.visibleWorldStatusBarCount() == healthBarBaseline, "Held health bars persisted after release");
			viewer.simulation.removeUnit(healthBarEnemy);
			healthBarStage++;
			System.out.println("[OutcomeMenuProbe] bracket keys and Alt showed the matching world health bars and cleared on release");
			return true;
		default:
			return true;
		}
	}

	private static boolean nextHealthBarStage() {
		healthBarStage++;
		return false;
	}

	private static boolean exerciseMissionHelp(final MeleeUI ui) {
		switch (helpStage) {
		case 0:
			ui.keyDown(Input.Keys.F10);
			click(ui, ui.missionMenuControl("HelpButton"));
			final var help = (TextAreaFrame) ui.missionMenuControl("HelpTextArea");
			check(ui.missionMenuControl("HelpPanel").isVisible() && help.getScrollBarFrame().isVisible(),
					"Original Help did not open with its scrollable text");
			helpStage++;
			return false;
		case 1:
			if (!capture("mission-help", null)) return false;
			click(ui, ui.missionMenuControl("HelpOKButton"));
			check(ui.missionMenuControl("MainPanel").isVisible(), "Help OK did not return to the mission menu");
			click(ui, ui.missionMenuControl("TipsButton"));
			check(ui.missionMenuControl("TipsPanel").isVisible(), "Original Tips did not open");
			helpStage++;
			return false;
		case 2:
			if (!capture("mission-tips-first", null)) return false;
			click(ui, ui.missionMenuControl("TipsNextButton"));
			helpStage++;
			return false;
		case 3:
			if (!capture("mission-tips-next", null)) return false;
			click(ui, ui.missionMenuControl("TipsBackButton"));
			click(ui, ui.missionMenuControl("TipsOKButton"));
			check(ui.missionMenuControl("MainPanel").isVisible(), "Tips OK did not return to the mission menu");
			click(ui, ui.missionMenuControl("ReturnButton"));
			helpStage++;
			System.out.println("[OutcomeMenuProbe] original Help and Tips opened, paged, and returned to the mission menu");
			return true;
		default:
			return true;
		}
	}

	private static boolean exerciseMissionOptions(final War3MapViewer viewer, final MeleeUI ui) {
		final var saved = OptionsSettingsStore.get();
		switch (optionsStage) {
		case 0:
			ui.keyDown(Input.Keys.F10);
			click(ui, ui.missionMenuControl("OptionsButton"));
			check(ui.missionMenuControl("OptionsPanel").isVisible(), "Original mission Options did not open");
			click(ui, ui.missionMenuControl("GameplayButton"));
			check(ui.missionMenuControl("GameplayPanel").isVisible(), "Gameplay Options did not open");
			optionsStage++;
			return false;
		case 1:
			if (!capture("mission-options-gameplay", null)) return false;
			final var tooltip = (CheckBoxFrame) ui.missionMenuControl("TooltipsCheckBox");
			final boolean before = saved.isTooltips();
			click(ui, tooltip);
			check(tooltip.isChecked() != before && saved.isTooltips() == before, "Options changed before OK");
			final var scroll = (ScrollBarFrame) ui.missionMenuControl("KeyScrollSlider");
			final var speed = (ScrollBarFrame) ui.missionMenuControl("GameSpeedSlider");
			check(speed.isVisible() && speed.getValue() == 1, "Local Game Speed did not start at Normal");
			dragSlider(ui, speed);
			check(speed.getValue() == 2 && viewer.getDisplayedGameSpeed() == CGameSpeed.NORMAL,
					"Game Speed changed before OK");
			final int savedScroll = saved.getKeyScrollSpeed();
			dragSlider(ui, scroll);
			check(scroll.getValue() != savedScroll && saved.getKeyScrollSpeed() == savedScroll,
					"Dragging a mission slider did not edit only the draft");
			check(ui.touchDown(1, 1, 1, Input.Buttons.RIGHT) && ui.touchUp(1, 1, 1, Input.Buttons.RIGHT),
					"Mission options let the world receive an outside click");
			ui.keyDown(Input.Keys.ESCAPE);
			check(ui.missionMenuControl("MainPanel").isVisible() && saved.isTooltips() == before
					&& saved.getGameSpeed() == null && viewer.getDisplayedGameSpeed() == CGameSpeed.NORMAL,
					"Escape did not discard the draft");
			click(ui, ui.missionMenuControl("OptionsButton"));
			click(ui, ui.missionMenuControl("SoundButton"));
			check(ui.missionMenuControl("SoundPanel").isVisible(), "Sound Options did not open");
			optionsStage++;
			return false;
		case 2:
			if (!capture("mission-options-sound", null)) return false;
			final var subtitles = (CheckBoxFrame) ui.missionMenuControl("SubtitlesCheckBox");
			final boolean previous = saved.isSubtitles();
			click(ui, subtitles);
			check(subtitles.isChecked() != previous, "Subtitles checkbox did not accept mouse input");
			click(ui, ui.missionMenuControl("OptionsOKButton"));
			check(ui.missionMenuControl("MainPanel").isVisible() && saved.isSubtitles() != previous,
					"OK did not apply mission options");
			final var reread = new OptionsSettingsStore();
			reread.load(OptionsSettingsStore.optionsFile());
			check(reread.isSubtitles() == saved.isSubtitles(), "Mission Options did not persist");
			click(ui, ui.missionMenuControl("OptionsButton"));
			click(ui, ui.missionMenuControl("GameplayButton"));
			dragSlider(ui, (ScrollBarFrame) ui.missionMenuControl("GameSpeedSlider"));
			final var subgroup = (CheckBoxFrame) ui.missionMenuControl("SubgroupCheckBox");
			check(!saved.isSubgroupOrderModifier() && !subgroup.isChecked(), "Subgroup modifier did not start unchecked");
			click(ui, subgroup);
			check(subgroup.isChecked() && !saved.isSubgroupOrderModifier(), "Subgroup modifier changed before OK");
			click(ui, ui.missionMenuControl("OptionsOKButton"));
			reread.load(OptionsSettingsStore.optionsFile());
			check(viewer.getDisplayedGameSpeed() == CGameSpeed.FAST && reread.getGameSpeed() == CGameSpeed.FAST,
					"Game Speed did not apply and persist");
			check(reread.isSubgroupOrderModifier() && saved.isSubgroupOrderModifier(),
					"Subgroup modifier did not apply and persist");
			click(ui, ui.missionMenuControl("ReturnButton"));
			speedSampleStartTick = viewer.simulation.getGameTurnTick();
			optionsStage++;
			return false;
		case 3:
			speedSampleSeconds += Gdx.graphics.getDeltaTime();
			if (speedSampleSeconds < 1) return false;
			final float simulatedSeconds = (viewer.simulation.getGameTurnTick() - speedSampleStartTick)
					* WarsmashConstants.SIMULATION_STEP_TIME;
			check(Math.abs(simulatedSeconds - speedSampleSeconds * CGameSpeed.FAST.getTimeScale()) < 0.11f,
					"Fast Game Speed did not control elapsed simulation time");
			optionsStage++;
			System.out.println("[OutcomeMenuProbe] original mission Options opened and verified cancel, apply, and persistence");
			return true;
		default:
			return true;
		}
	}

	private static boolean exerciseNamedSave(final War3MapViewer viewer, final MeleeUI ui) {
		final String slotName = "A named checkpoint";
		final var namedSlot = MissionSaveStore.namedSlot(slotName);
		switch (saveStage) {
		case 0:
			check(ui.keyDown(Input.Keys.F6), "F6 did not invoke Quick Save");
			final var quick = CGameSave.tryLoad(MissionSaveStore.namedSlot("QuickSave"));
			check(quick != null && quick.checkpoint.targetTick == viewer.simulation.getGameTurnTick(),
					"F6 did not save the current simulation tick");
			ui.keyDown(Input.Keys.F10);
			click(ui, ui.missionMenuControl("SaveGameButton"));
			check(ui.missionMenuControl("EscMenuSaveGamePanel").isVisible(), "Save Game did not open the named slots");
			for (final char character : slotName.toCharArray()) ui.keyTyped(character);
			saveStage++;
			return false;
		case 1:
			if (!capture("mission-save-game", null)) return false;
			click(ui, ui.missionMenuControl("SaveGameSaveButton"));
			final var named = CGameSave.tryLoad(namedSlot);
			check(named != null && named.checkpoint.targetTick == viewer.simulation.getGameTurnTick(),
					"Named slot did not record the current simulation tick");
			click(ui, ui.missionMenuControl("SaveGameButton"));
			for (final char character : slotName.toCharArray()) ui.keyTyped(character);
			ui.keyDown(Input.Keys.ENTER);
			check(ui.missionMenuControl("EscMenuOverwriteContainer").isVisible(),
					"An existing slot did not ask before overwriting");
			ui.keyDown(Input.Keys.ESCAPE);
			check(ui.missionMenuControl("EscMenuSaveLoadContainer").isVisible()
					&& CGameSave.tryLoad(namedSlot).checkpoint.targetTick == named.checkpoint.targetTick,
					"Cancel changed the existing named slot");
			final var list = ui.missionMenuControl("FileListFrame");
			final Vector2 location = new Vector2(
					list.getFramePointX(FramePoint.LEFT) + GameUI.convertX(ui.getUiViewport(), 0.04f),
					list.getFramePointY(FramePoint.TOP) - GameUI.convertY(ui.getUiViewport(), 0.015f));
			clickAt(ui, location);
			click(ui, ui.missionMenuControl("SaveGameDeleteButton"));
			check(ui.missionMenuControl("EscMenuDeleteContainer").isVisible() && namedSlot.isFile(),
					"Deleting a slot did not wait for confirmation");
			click(ui, ui.missionMenuControl("DeleteDeleteButton"));
			check(!namedSlot.exists(), "The confirmed named slot was not deleted");
			click(ui, ui.missionMenuControl("SaveGameSaveButton"));
			final var replacement = CGameSave.tryLoad(namedSlot);
			check(replacement != null, "The named slot could not be saved again after deletion");
			click(ui, ui.missionMenuControl("LoadGameButton"));
			check(ui.missionMenuControl("LoadOnly").isVisible(), "Load Game did not show the saved slots");
			clickAt(ui, location);
			check(((GlueTextButtonFrame) ui.missionMenuControl("LoadGameLoadButton")).isEnabled(),
					"The named checkpoint could not be selected in the load list");
			namedRestoreTick = replacement.checkpoint.targetTick;
			saveBrowserViewer = viewer;
			click(ui, ui.missionMenuControl("LoadGameLoadButton"));
			System.out.println("[OutcomeMenuProbe] requested the named mission save at tick " + namedRestoreTick);
			saveStage++;
			return false;
		case 2:
			if (viewer == saveBrowserViewer || viewer.simulation.getMissionCheckpoint().isRestoring()
					|| viewer.simulation.getGameTurnTick() < namedRestoreTick) return false;
			check(!ui.missionMenuControl("EscMenuSaveGamePanel").isVisible(), "The load menu stayed open after resume");
			saveBrowserViewer = null;
			saveStage++;
			System.out.println("[OutcomeMenuProbe] F6, named saves, overwrite cancellation, deletion and resuming from the named slot passed");
			return true;
		default:
			return true;
		}
	}

	private static boolean exerciseMovie(final MeleeUI ui) {
		if (MOVIE.isEmpty() || movieStage == 4) return true;
		if (movieStage == 0 || movieStage == 2) {
			ui.setCinematicSkipButtonVisible(true);
			ui.playCinematic(MOVIE);
			check(ui.getMovieDurationSeconds() > 100 && !ui.isUserControlEnabled(), "Original movie did not begin playback");
			if (movieStage == 0) originalMovieDuration = ui.getMovieDurationSeconds();
			movieStage++;
			return false;
		}
		if (COMPLETE_MOVIE && movieStage == 1 && ui.getMovieDurationSeconds() == 0) {
			check(movieFrameCaptured && lastMovieFrame >= originalMovieDuration - 1.5f
					&& Math.abs(lastMovieAudio - lastMovieFrame) < 1.5f && ui.isUserControlEnabled(),
					"Natural movie completion did not submit its last frames and restore mission controls");
			System.out.printf("[OutcomeMenuProbe] original movie reached its natural ending: %.2fs video with %.2fs audio submitted%n",
					lastMovieFrame, lastMovieAudio);
			movieStage++;
			return false;
		}
		final float video = ui.getUploadedMovieTime();
		final float audio = ui.getSubmittedMovieAudioTime();
		if (COMPLETE_MOVIE && movieStage == 1) {
			lastMovieFrame = video;
			lastMovieAudio = audio;
			if (!movieFrameCaptured && video >= 8) movieFrameCaptured = capture("mission-original-movie", null);
			return false;
		}
		if (video < (movieStage == 1 ? 8 : 3)) return false;
		check(audio > 0 && Math.abs(audio - video) < 1.5f,
				"Original movie audio and video drifted: " + audio + "s audio, " + video + "s video");
		if (movieStage == 1 && !capture("mission-original-movie", null)) return false;
		ui.keyDown(Input.Keys.ESCAPE);
		check(ui.getMovieDurationSeconds() == 0 && ui.getSubmittedMovieAudioTime() == 0 && ui.isUserControlEnabled(),
				"Esc did not return from the original movie");
		movieStage++;
		System.out.printf("[OutcomeMenuProbe] original movie pass %d: %.2fs video with %.2fs audio submitted, Escape restored controls%n",
				movieStage / 2, video, audio);
		return movieStage == 4;
	}

	private static boolean exercisePause(final War3MapViewer viewer, final MeleeUI ui) {
		if (pauseStage == 0) {
			if (!ui.isUserControlEnabled()) return false;
			ui.keyDown(Input.Keys.F9);
			check(ui.getVisibleScriptDialog() != null, "F9 did not open the Quest Log");
			ui.keyDown(Input.Keys.F9);
			check(ui.getVisibleScriptDialog() == null, "F9 did not close the Quest Log");
			ui.keyDown(Input.Keys.F10);
			check(ui.missionMenuControl("MainPanel").isVisible(), "F10 did not open the mission menu");
			pausedTick = viewer.simulation.getGameTurnTick();
			pauseStage = 1;
			return false;
		}
		if (pauseStage == 1) {
			check(viewer.simulation.getGameTurnTick() == pausedTick, "The simulation advanced while the local menu was open");
			if (++pausedFrames < 12) return false;
			ui.keyDown(Input.Keys.ESCAPE);
			check(!ui.missionMenuControl("MainPanel").isVisible(), "Escape did not close the mission menu");
			click(ui, ui.missionMenuControl("UpperButtonBarMenuButton"));
			final var pause = (GlueTextButtonFrame) ui.missionMenuControl("PauseButton");
			check(pause.isEnabled(), "Pause Game is disabled during a local mission");
			click(ui, pause);
			check(viewer.isLocalGamePaused(), "Pause Game did not freeze the local mission");
			check(((StringFrame) pause.getButtonText()).getText().startsWith("Resume "), "Missing Resume Game label");
			click(ui, ui.missionMenuControl("ReturnButton"));
			pausedFrames = 0;
			pauseStage = 2;
			return false;
		}
		if (pauseStage == 2) {
			check(viewer.simulation.getGameTurnTick() == pausedTick, "The simulation advanced during a user pause");
			if (++pausedFrames < 12) return false;
			ui.keyDown(Input.Keys.F10);
			click(ui, ui.missionMenuControl("PauseButton"));
			check(!viewer.isLocalGamePaused(), "Resume Game did not unpause the local mission");
			click(ui, ui.missionMenuControl("ReturnButton"));
			pauseStage = 3;
			return false;
		}
		if (viewer.simulation.getGameTurnTick() == pausedTick) return false;
		System.out.println("[OutcomeMenuProbe] F10 froze local ticks and manual Pause stayed active after closing the menu");
		return true;
	}

	private static void click(final MeleeUI ui, final CScriptDialogButton button) {
		click(ui, button.getButtonFrame());
	}

	private static void dragSlider(final MeleeUI ui, final ScrollBarFrame slider) {
		final float left = slider.getFramePointX(FramePoint.LEFT);
		final float width = slider.getFramePointX(FramePoint.RIGHT) - left;
		final float center = slider.getFramePointY(FramePoint.CENTER);
		final Vector2 start = ui.getUiViewport().project(new Vector2(left + width * 0.25f, center));
		final Vector2 finish = ui.getUiViewport().project(new Vector2(left + width * 0.85f, center));
		final int y = Gdx.graphics.getHeight() - Math.round(start.y);
		ui.touchDown(Math.round(start.x), y, y, Input.Buttons.LEFT);
		ui.touchDragged(Math.round(finish.x), y, y, 0);
		ui.touchUp(Math.round(finish.x), y, y, Input.Buttons.LEFT);
	}

	private static void click(final MeleeUI ui, final UIFrame frame) {
		clickAt(ui, new Vector2(frame.getFramePointX(FramePoint.CENTER), frame.getFramePointY(FramePoint.CENTER)));
	}

	private static void clickAt(final MeleeUI ui, final Vector2 location) {
		final Vector2 screen = ui.getUiViewport().project(location.cpy());
		final int x = Math.round(screen.x);
		final int y = Gdx.graphics.getHeight() - Math.round(screen.y);
		final boolean modal = ui.getVisibleScriptDialog() != null;
		final boolean pressed = ui.touchDown(x, y, y, Input.Buttons.LEFT);
		final boolean released = ui.touchUp(x, y, y, Input.Buttons.LEFT);
		check(!modal || pressed, "Dialog mouse press was not consumed");
		check(!modal || released, "Dialog mouse release was not consumed");
	}

	private static boolean capture(final String name, final CScriptDialog dialog) {
		if (name.equals(lastCapture)) return true;
		if (dialog != null) validateBounds(dialog);
		if (!name.equals(pendingCapture)) {
			pendingCapture = name;
			capturedFrames = 0;
		}
		return false;
	}

	private static void validateBounds(final CScriptDialog dialog) {
		final UIFrame panel = dialog.getScriptDialogFrame();
		float previousBottom = dialog.getScriptDialogTextFrame().getFramePointY(FramePoint.BOTTOM);
		for (final CScriptDialogButton button : dialog.getButtons()) {
			final UIFrame frame = button.getButtonFrame();
			check(frame.getFramePointY(FramePoint.TOP) <= previousBottom, "Overlapping dialog content");
			check(frame.getFramePointY(FramePoint.BOTTOM) >= panel.getFramePointY(FramePoint.BOTTOM), "Button below panel");
			check(frame.getFramePointX(FramePoint.LEFT) >= panel.getFramePointX(FramePoint.LEFT)
					&& frame.getFramePointX(FramePoint.RIGHT) <= panel.getFramePointX(FramePoint.RIGHT), "Button outside panel");
			previousBottom = frame.getFramePointY(FramePoint.BOTTOM);
		}
	}

	public static void afterUIRender() {
		if (!ENABLED || pendingCapture == null || ++capturedFrames < 3) return;
		final String directory = System.getProperty("warsmash.outcomeMenuCaptureDirectory");
		check(directory != null, "Capture directory missing");
		Gdx.files.absolute(directory).mkdirs();
		final Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		final PixmapIO.PNG writer = new PixmapIO.PNG();
		try {
			if ("mission-original-movie".equals(pendingCapture)) {
				final int right = pixels.getWidth() - 20;
				check((pixels.getPixel(20, 20) & 0xffffff00) == 0 && (pixels.getPixel(right, 20) & 0xffffff00) == 0,
						"The original movie's letterbox exposed the mission viewport");
				boolean hasPicture = false;
				for (int x = pixels.getWidth() / 8; x < pixels.getWidth() * 7 / 8; x += pixels.getWidth() / 8) {
					if ((pixels.getPixel(x, pixels.getHeight() / 2) & 0xffffff00) != 0) hasPicture = true;
				}
				check(hasPicture, "No picture was visible inside the original movie frame");
			}
			writer.setFlipY(true);
			writer.write(Gdx.files.absolute(directory).child(pendingCapture + ".png"), pixels);
		}
		catch (final IOException error) { throw new RuntimeException(error); }
		finally { writer.dispose(); pixels.dispose(); }
		lastCapture = pendingCapture;
		pendingCapture = null;
		System.out.println("[OutcomeMenuProbe] capture " + lastCapture);
	}

	public static void afterMenuRender() {
		if (ENABLED && stage == 7) {
			System.out.println("[OutcomeMenuProbe] complete");
			Gdx.app.exit();
		}
	}

	private static void advance(final String text) {
		System.out.println("[OutcomeMenuProbe] stage " + (++stage) + ": " + text);
	}

	private static void check(final boolean value, final String message) {
		if (!value) throw new IllegalStateException("Outcome menu audit: " + message);
	}
}

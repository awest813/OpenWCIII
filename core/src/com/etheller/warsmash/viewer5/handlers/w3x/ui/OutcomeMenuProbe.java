package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.IOException;
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
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerGameResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;

/** Bounded desktop audit for result choices, keyboard modality, and rendered layout. */
public final class OutcomeMenuProbe {
	private static final boolean ENABLED = Boolean.getBoolean("warsmash.outcomeMenuAudit");
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
	private static final int[][] SIZES = {{960, 540}, {800, 600}, {600, 800}};

	private OutcomeMenuProbe() { }

	public static void afterMapRender(final War3MapViewer viewer, final MeleeUI ui, final boolean initialized) {
		if (!ENABLED) return;
		if (started == 0) started = System.nanoTime();
		check(System.nanoTime() - started < 150_000_000_000L, "Timeout at stage " + stage);
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
			check(summary.contains("Difficulty: Normal") && summary.contains("Resources remaining")
					&& summary.contains("Surviving forces"), "Result snapshot missing");
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

	private static void click(final MeleeUI ui, final CScriptDialogButton button) {
		final UIFrame frame = button.getButtonFrame();
		final Vector2 screen = ui.getUiViewport().project(new Vector2(frame.getFramePointX(FramePoint.CENTER),
				frame.getFramePointY(FramePoint.CENTER)));
		final int x = Math.round(screen.x);
		final int y = Gdx.graphics.getHeight() - Math.round(screen.y);
		check(ui.touchDown(x, y, y, Input.Buttons.LEFT), "Dialog mouse press was not consumed");
		check(ui.touchUp(x, y, y, Input.Buttons.LEFT), "Dialog mouse release was not consumed");
	}

	private static boolean capture(final String name, final CScriptDialog dialog) {
		if (name.equals(lastCapture)) return true;
		validateBounds(dialog);
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
		final Pixmap pixels = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		final PixmapIO.PNG writer = new PixmapIO.PNG();
		try {
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

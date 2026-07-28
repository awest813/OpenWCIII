package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import com.badlogic.gdx.audio.Music;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.w3x.camera.GameCameraManager;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerColor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.command.CommandErrorListener;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CTimerDialog;

import java.util.List;

public interface WarsmashUI extends CommandErrorListener, WarsmashBaseUI {

	CScriptDialog createScriptDialog(GlobalScope globalScope);

	void clearDialog(CScriptDialog dialog);

	void destroyDialog(CScriptDialog dialog);

	CScriptDialogButton createScriptDialogButton(CScriptDialog dialog, String buttonText, char hotkeyInt);

	GameCameraManager getCameraManager();

	Music playMusic(String musicField, boolean random, int index);

	Music setMapMusic(String musicField, boolean random, int index);

	void playMapMusic();

	/** Clears the map/default music playlist and stops playback. */
	void clearMapMusic();

	Music playMusicEx(String musicField, boolean random, int index, int fromMSecs, int fadeInMSecs);

	void stopMusic(boolean fadeOut);

	void resumeMusic();

	void setMusicVolume(int volume);

	void setMusicPlayPosition(int millisecs);

	Scene getUiScene();

	CTimerDialog createTimerDialog(CTimer timer);

	void removedUnit(CUnit whichUnit);

	void removedItem(CItem whichItem);

	void displayTimedText(float x, float y, float duration, String message);

	void clearTextMessages();

	void showInterface(boolean show, float fadeDuration);

	void setCinematicScene(int portraitUnitId, CPlayerColor color, String speakerTitle, String text,
			float sceneDuration, float voiceoverDuration);

	/**
	 * Like {@link #setCinematicScene(int, CPlayerColor, String, String, float, float)}
	 * with an optional portrait animation name (named transmission natives).
	 */
	default void setCinematicScene(int portraitUnitId, CPlayerColor color, String speakerTitle, String text,
			float sceneDuration, float voiceoverDuration, String animationName) {
		setCinematicScene(portraitUnitId, color, speakerTitle, text, sceneDuration, voiceoverDuration);
	}

	void enableUserControl(boolean value);

	void endCinematicScene();

	void forceCinematicSubtitles(boolean value);

	/**
	 * Trigger a custom victory for the local player (campaign mission complete).
	 * When {@code enableScoreScreen} is true the score/victory screen should be
	 * shown; when false the game silently transitions back to the menu.
	 */
	void customVictory(boolean enableScoreScreen);

	/**
	 * Trigger a custom defeat for the local player (campaign mission failed).
	 * When {@code enableScoreScreen} is true the score/defeat screen should be
	 * shown; when false the game silently transitions back to the menu.
	 */
	void customDefeat(boolean enableScoreScreen);

	/**
	 * Script-driven selection: add ({@code flag=true}) or remove ({@code flag=false})
	 * a unit from the local player's selection.
	 */
	void scriptSelectUnit(CUnit whichUnit, boolean flag);

	/** Clears the local player's unit selection. */
	void scriptClearSelection();

	/** Replaces the local player's selection with the units in {@code group}. */
	void scriptSelectGroup(List<CUnit> group);

	/** Returns the currently selected simulation units for the local player. */
	List<CUnit> getScriptSelectedUnits();

	/**
	 * Campaign progression: unload the current map and load {@code newLevel}.
	 * When {@code doScoreScreen} is true, show the score Continue dialog first.
	 */
	void requestChangeLevel(String newLevel, boolean doScoreScreen);

	/**
	 * Play a campaign movie / cinematic stub. Blocks the calling JASS thread for a
	 * short duration (or until skipped). Real video decode is not yet available.
	 */
	void playCinematic(String moviePath);

	/** Associates the sleeping JASS thread with the current movie overlay. */
	void bindMovieSleepThread(com.etheller.interpreter.ast.execution.JassThread thread);

	/** Controls whether ESC can skip the current {@link #playCinematic} overlay. */
	void setCinematicSkipButtonVisible(boolean visible);

	/** Ends an in-progress {@link #playCinematic} overlay early. */
	void endPlayCinematic();

	/** Registers a quest for the in-game quest dialog. */
	void registerQuest(com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest quest);

	/** Removes a quest from the in-game quest dialog. */
	void unregisterQuest(com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CQuest quest);

	/** Rebuilds quest dialog contents from the registered quest list. */
	void forceQuestDialogUpdate();

	/** Flashes the quests toolbar button to draw attention. */
	void flashQuestDialogButton();

	void trackMultiboard(com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui.CMultiboard board);

	void untrackMultiboard(com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui.CMultiboard board);

	com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CLeaderboard createLeaderboard();

	void destroyLeaderboard(com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CLeaderboard board);

	/** Plays a transmission voice-over label (UISounds / SoundLabels). */
	void playTransmissionSound(String soundLabel);

	/** Stops the last transmission VO and clears cinematic dialogue. */
	void clearTransmissionQueue();

	/** When false, transmission natives skip portrait/text/VO. */
	void setTransmissionEnabled(boolean enabled);

	boolean isTransmissionEnabled();

	void setCineFilterTexture(String filename);

	void setCineFilterBlendMode(com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CBlendMode mode);

	void setCineFilterTexMapFlags(
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CTexMapFlags flags);

	void setCineFilterStartUV(float minu, float minv, float maxu, float maxv);

	void setCineFilterEndUV(float minu, float minv, float maxu, float maxv);

	void setCineFilterStartColor(int red, int green, int blue, int alpha);

	void setCineFilterEndColor(int red, int green, int blue, int alpha);

	void setCineFilterDuration(float duration);

	void displayCineFilter(boolean flag);

	boolean isCineFilterDisplayed();

	/** Applies a volume-group scale (0–1). MUSIC maps to the music player. */
	void setVolumeGroupVolume(
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup group,
			float scale);

	void resetVolumeGroups();

	/**
	 * Duck map music/ambient when cinematic audio mode is enabled (MVP).
	 * Full cinematic volume-group remix is still TODO.
	 */
	void setCinematicAudio(boolean enabled);

	/** Hides the first N command-card slots from the local hero UI (script reserve). */
	void setReservedLocalHeroButtons(int reserved);

	void trackTrackable(com.etheller.warsmash.viewer5.handlers.w3x.simulation.ui.CTrackable trackable);

	void setSelectionEnabled(boolean enabled);

	void setDragSelectEnabled(boolean enabled);

	void setPreSelectEnabled(boolean enabled);

	void forceUIKey(String key);

	void forceUICancel();

	void registerDefeatCondition(
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CDefeatCondition condition);

	void unregisterDefeatCondition(
			com.etheller.warsmash.viewer5.handlers.w3x.simulation.quest.CDefeatCondition condition);

	/** Timed colored minimap ping (PingMinimap / PingMinimapEx). */
	void pingMinimap(float x, float y, float duration, float red, float green, float blue);

	/** Fire a local-player chat message into TriggerRegisterPlayerChatEvent listeners. */
	void submitPlayerChat(String message);

}

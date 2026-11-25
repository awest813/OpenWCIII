package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.MenuItem;
import com.etheller.warsmash.parsers.fdf.frames.CheckBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.MenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.PopupMenuFrame;
import com.etheller.warsmash.parsers.fdf.frames.ScrollBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.gl.Extensions;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameSpeed;

/** Binds the in-mission option frames to the same settings as the front end. */
final class MissionOptionsPanel {
	private static final String[] QUALITY = { "LOW", "MEDIUM", "HIGH" };
	private final GameUI root;
	private final Viewport viewport;
	private final UIFrame panel;
	private final UIFrame[] pages;
	private final UIFrame buttons;
	private final Runnable returnToMenu;
	private final IntConsumer previewMusicVolume;
	private final Consumer<Boolean> applySubtitles;
	private final Consumer<CGameSpeed> applyGameSpeed;
	private final Consumer<String> showError;
	private final ScrollBarFrame gameSpeedSlider;
	private final List<Runnable> refreshControls = new ArrayList<>();
	private final OptionsSettingsStore draft = new OptionsSettingsStore();

	MissionOptionsPanel(final GameUI root, final Viewport viewport, final UIFrame panel, final Runnable returnToMenu,
			final IntConsumer previewMusicVolume, final Consumer<Boolean> applySubtitles,
			final Consumer<CGameSpeed> applyGameSpeed, final Consumer<String> showError) {
		this.root = root;
		this.viewport = viewport;
		this.panel = panel;
		this.returnToMenu = returnToMenu;
		this.previewMusicVolume = previewMusicVolume;
		this.applySubtitles = applySubtitles;
		this.applyGameSpeed = applyGameSpeed;
		this.showError = showError;
		this.pages = new UIFrame[] { frame("OptionsPanel"), frame("GameplayPanel"), frame("VideoPanel"),
				frame("SoundPanel"), frame("NetworkPanel") };
		this.buttons = frame("BottomButtonPanel");
		button("GameplayButton").setOnClick(() -> showPage(1));
		button("VideoButton").setOnClick(() -> showPage(2));
		button("SoundButton").setOnClick(() -> showPage(3));
		button("NetworkButton").setEnabled(false);
		button("OptionsPreviousButton").setOnClick(this::cancel);
		button("OptionsOKButton").setOnClick(this::apply);
		button("OptionsCancelButton").setOnClick(this::cancel);
		this.gameSpeedSlider = (ScrollBarFrame) frame("GameSpeedSlider");
		this.gameSpeedSlider.setMinValue(0);
		this.gameSpeedSlider.setMaxValue(CGameSpeed.FAST.ordinal() - CGameSpeed.SLOW.ordinal());
		this.gameSpeedSlider.setStepSize(1);
		this.gameSpeedSlider.setChangeListener((gui, view, value) -> {
			final var speed = CGameSpeed.VALUES[CGameSpeed.SLOW.ordinal() + value];
			this.draft.setGameSpeed(speed);
			this.root.setDecoratedText((StringFrame) frame("GameSpeedValue"), speed.name());
		});
		((CheckBoxFrame) frame("InputSprocketCheckBox")).setEnabled(false);
		((CheckBoxFrame) frame("MovementCheckBox")).setEnabled(false);
		((CheckBoxFrame) frame("EnviroCheckBox")).setEnabled(false);

		bindSlider("MouseScrollSlider", this.draft::getMouseScrollSpeed, this.draft::setMouseScrollSpeed);
		bindSlider("KeyScrollSlider", this.draft::getKeyScrollSpeed, this.draft::setKeyScrollSpeed);
		bindCheckBox("MouseScrollDisable", this.draft::isMouseScrollDisabled, this.draft::setMouseScrollDisabled);
		bindCheckBox("SubgroupCheckBox", this.draft::isSubgroupOrderModifier, this.draft::setSubgroupOrderModifier);
		bindCheckBox("TooltipsCheckBox", this.draft::isTooltips, this.draft::setTooltips);
		bindSlider("GammaSlider", this.draft::getGamma, this.draft::setGamma);
		bindChoice("EscOptionsParticlesMenu", QUALITY,
				this.draft::getParticleQuality, this.draft::setParticleQuality);
		final String[] visibility = { "OFF", "ON" };
		bindChoice("EscOptionsLightsMenu", visibility, () -> this.draft.isLocalLights() ? 1 : 0,
				index -> this.draft.setLocalLights(index == 1));
		bindChoice("EscOptionsShadowsMenu", visibility, () -> this.draft.isShadows() ? 1 : 0,
				index -> this.draft.setShadows(index == 1));
		bindChoice("EscOptionsOcclusionMenu", visibility, () -> this.draft.isOcclusion() ? 1 : 0,
				index -> this.draft.setOcclusion(index == 1));
		bindSlider("SoundVolumeSlider", this.draft::getSoundVolume, this.draft::setSoundVolume);
		bindSlider("MusicVolumeSlider", this.draft::getMusicVolume, value -> {
			this.draft.setMusicVolume(value);
			previewMusic(this.draft);
		});
		bindCheckBox("SoundCheckBox", this.draft::isSoundEnabled, this.draft::setSoundEnabled);
		bindCheckBox("MusicCheckBox", this.draft::isMusicEnabled, enabled -> {
			this.draft.setMusicEnabled(enabled);
			previewMusic(this.draft);
		});
		bindCheckBox("AmbientCheckBox", this.draft::isAmbientSounds, this.draft::setAmbientSounds);
		bindCheckBox("SubtitlesCheckBox", this.draft::isSubtitles, this.draft::setSubtitles);
		bindCheckBox("UnitCheckBox", this.draft::isUnitSounds, this.draft::setUnitSounds);
		if (frame("MovementCheckBox") instanceof CheckBoxFrame) {
			bindCheckBox("MovementCheckBox", this.draft::isMovementSounds, this.draft::setMovementSounds);
		}
		bindCheckBox("PositionalCheckBox", this.draft::isPositionalAudio, this.draft::setPositionalAudio);
		this.panel.setVisible(false);
	}

	private UIFrame frame(final String name) {
		return this.root.getFrameByName(name, 0);
	}

	private GlueTextButtonFrame button(final String name) {
		return (GlueTextButtonFrame) frame(name);
	}

	private void bindSlider(final String name, final IntSupplier get, final IntConsumer set) {
		final ScrollBarFrame slider = (ScrollBarFrame) frame(name);
		slider.setChangeListener((gui, view, value) -> set.accept(value));
		this.refreshControls.add(() -> slider.setValue(this.root, this.viewport, get.getAsInt()));
	}

	private void bindCheckBox(final String name, final BooleanSupplier get, final Consumer<Boolean> set) {
		final CheckBoxFrame checkBox = (CheckBoxFrame) frame(name);
		checkBox.setOnClick(() -> set.accept(checkBox.isChecked()));
		this.refreshControls.add(() -> checkBox.setChecked(get.getAsBoolean()));
	}

	private void bindChoice(final String name, final String[] labels, final IntSupplier get, final IntConsumer set) {
		final PopupMenuFrame popup = (PopupMenuFrame) frame(name);
		final List<MenuItem> items = new ArrayList<>();
		for (final String label : labels) items.add(new MenuItem(this.root.getTemplates().getDecoratedString(label), -2));
		((MenuFrame) popup.getPopupMenuFrame()).setItems(this.viewport, items);
		final Runnable refresh = () -> this.root.setText((StringFrame) ((GlueTextButtonFrame) popup.getPopupTitleFrame())
				.getButtonText(), items.get(get.getAsInt()).getText());
		popup.setMenuClickListener((button, index) -> {
			if (index >= 0 && index < labels.length) {
				set.accept(index);
				refresh.run();
			}
		});
		this.refreshControls.add(refresh);
	}

	private void showPage(final int page) {
		for (int index = 0; index < this.pages.length; index++) this.pages[index].setVisible(index == page);
		this.buttons.setVisible(page != 0);
		this.panel.positionBounds(this.root, this.viewport);
	}

	void open(final CGameSpeed speed, final String difficulty, final boolean gameSpeedEditable) {
		final var settings = OptionsSettingsStore.get();
		this.draft.copyFrom(settings);
		this.gameSpeedSlider.setVisible(gameSpeedEditable);
		this.gameSpeedSlider.setValue(this.root, this.viewport,
				speed.ordinal() - CGameSpeed.SLOW.ordinal());
		this.draft.setGameSpeed(settings.getGameSpeed());
		this.panel.setVisible(true);
		showPage(0);
		this.refreshControls.forEach(Runnable::run);
		this.root.setText((StringFrame) frame("ResolutionValue"), Gdx.graphics.getWidth() + " x " + Gdx.graphics.getHeight());
		this.root.setDecoratedText((StringFrame) frame("GameSpeedValue"), speed.name());
		this.root.setDecoratedText((StringFrame) frame("DifficultyValue"), difficulty);
		this.root.setDecoratedText((StringFrame) frame("CustomKeysValue"), "OFF");
		this.root.setDecoratedText((StringFrame) frame("ProviderValue"), "DEFAULT");
		this.root.setDecoratedText((StringFrame) frame("ModelDetailValue"), QUALITY[this.draft.getModelDetail()]);
		this.root.setDecoratedText((StringFrame) frame("AnimQualityValue"), QUALITY[this.draft.getAnimationQuality()]);
		this.root.setDecoratedText((StringFrame) frame("TextureQualityValue"), QUALITY[this.draft.getTextureQuality()]);
		this.root.setDecoratedText((StringFrame) frame("SpellFilterValue"), "HIGH");
	}

	boolean isVisible() {
		return this.panel.isVisible();
	}

	void hide() {
		if (!isVisible()) return;
		previewMusic(OptionsSettingsStore.get());
		this.panel.setVisible(false);
	}

	void cancel() {
		hide();
		this.returnToMenu.run();
	}

	private void apply() {
		final var saved = OptionsSettingsStore.get();
		final String error = VideoOptions.applyAndSave(this.draft, saved, false, Gdx.graphics, Extensions.gamma,
				OptionsSettingsStore.optionsFile());
		if (error != null) {
			previewMusic(saved);
			this.showError.accept(error);
			return;
		}
		this.applySubtitles.accept(saved.isSubtitles());
		if (this.gameSpeedSlider.isVisible()) this.applyGameSpeed.accept(saved.getGameSpeed());
		cancel();
	}

	private void previewMusic(final OptionsSettingsStore values) {
		this.previewMusicVolume.accept(Math.round(values.getEffectiveMusicVolume() * 1.27f));
	}
}

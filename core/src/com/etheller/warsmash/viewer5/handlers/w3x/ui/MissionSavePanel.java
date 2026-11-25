package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.frames.EditBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.GlueTextButtonFrame;
import com.etheller.warsmash.parsers.fdf.frames.ListBoxFrame;
import com.etheller.warsmash.parsers.fdf.frames.ScrollBarFrame;
import com.etheller.warsmash.parsers.fdf.frames.SimpleFrame;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CGameSave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.save.MissionSaveStore;

/** In-mission save and load controls backed by the active profile's mission checkpoints. */
final class MissionSavePanel {
	@FunctionalInterface
	interface FileAction {
		void run(File file) throws IOException;
	}

	private final GameUI root;
	private final Viewport viewport;
	private final UIFrame panel;
	private final UIFrame browser;
	private final UIFrame overwrite;
	private final UIFrame delete;
	private final EditBoxFrame name;
	private final ListBoxFrame saves;
	private final ScrollBarFrame scrollbar;
	private final Runnable returnToMenu;
	private final FileAction save;
	private final FileAction load;
	private final Consumer<String> showError;
	private List<String> fileNames = List.of();
	private boolean saving;
	private File pendingFile;

	MissionSavePanel(final GameUI root, final Viewport viewport, final DataSource dataSource, final UIFrame panel,
			final Runnable returnToMenu, final FileAction save, final FileAction load, final Consumer<String> showError) {
		this.root = root;
		this.viewport = viewport;
		this.panel = panel;
		this.returnToMenu = returnToMenu;
		this.save = save;
		this.load = load;
		this.showError = showError;
		this.browser = frame("EscMenuSaveLoadContainer");
		this.overwrite = frame("EscMenuOverwriteContainer");
		this.delete = frame("EscMenuDeleteContainer");
		this.name = (EditBoxFrame) frame("SaveGameFileEditBox");
		this.name.setFilter(character -> !Character.isISOControl(character));
		final SimpleFrame listContainer = (SimpleFrame) frame("FileListFrame");
		this.saves = new ListBoxFrame("MissionSaveList", listContainer, viewport, dataSource);
		this.saves.setSetAllPoints(true);
		this.saves.setFrameFont(((StringFrame) frame("OverwriteMessageText")).getFrameFont());
		this.scrollbar = (ScrollBarFrame) root.createFrame("EscMenuScrollBarTemplate", this.saves, 0, 0);
		this.saves.setScrollBarFrame(this.scrollbar);
		listContainer.add(this.saves);
		this.saves.setSelectionListener((index, item) -> {
			if (this.saving && index >= 0) {
				final String fileName = this.fileNames.get(index);
				this.name.setText(fileName.substring(0, fileName.length() - 4), this.root, this.viewport);
			}
			updateSelection();
		});
		this.name.setOnEnter(this::requestSave);
		button("SaveGameSaveButton").setOnClick(this::requestSave);
		button("LoadGameLoadButton").setOnClick(this::loadSelected);
		button("SaveGameDeleteButton").setOnClick(this::requestDelete);
		button("SaveGameCancelButton").setOnClick(this::cancel);
		button("LoadGameCancelButton").setOnClick(this::cancel);
		button("OverwriteCancelButton").setOnClick(this::cancel);
		button("DeleteCancelButton").setOnClick(this::cancel);
		button("OverwriteOverwriteButton").setOnClick(() -> save(this.pendingFile));
		button("DeleteDeleteButton").setOnClick(this::deleteSelected);
		this.panel.setVisible(false);
	}

	private UIFrame frame(final String name) {
		return MenuUI.findFrameIn(this.panel, name);
	}

	private GlueTextButtonFrame button(final String name) {
		return (GlueTextButtonFrame) frame(name);
	}

	void open(final boolean saving) {
		this.saving = saving;
		this.name.setText("", this.root, this.viewport);
		frame("SaveOnly").setVisible(saving);
		frame("LoadOnly").setVisible(!saving);
		this.panel.setVisible(true);
		show(this.browser);
		refreshList();
	}

	boolean isVisible() {
		return this.panel.isVisible();
	}

	void hide() {
		this.name.onFocusLost();
		this.pendingFile = null;
		this.panel.setVisible(false);
	}

	void cancel() {
		if (this.browser.isVisible()) {
			hide();
			this.returnToMenu.run();
		}
		else {
			this.pendingFile = null;
			show(this.browser);
		}
	}

	boolean keyDown(final int keycode) {
		if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.F10) {
			cancel();
		}
		else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
			if (this.overwrite.isVisible()) save(this.pendingFile);
			else if (this.delete.isVisible()) deleteSelected();
			else if (this.saving) requestSave();
			else loadSelected();
		}
		else if (this.saving && this.browser.isVisible()) {
			this.name.keyDown(keycode);
		}
		return true;
	}

	boolean keyTyped(final char character) {
		if (!isVisible()) return false;
		if (this.saving && this.browser.isVisible()) this.name.keyTyped(character);
		return true;
	}

	void scroll(final float amount) {
		if (this.browser.isVisible()) this.scrollbar.setValue(this.root, this.viewport,
				this.scrollbar.getValue() - Math.round(amount * 10));
	}

	private void show(final UIFrame visible) {
		this.browser.setVisible(visible == this.browser);
		this.overwrite.setVisible(visible == this.overwrite);
		this.delete.setVisible(visible == this.delete);
		if (this.saving && visible == this.browser) this.name.onFocusGained();
		else this.name.onFocusLost();
		this.panel.positionBounds(this.root, this.viewport);
	}

	private void refreshList() {
		this.saves.removeAllItems();
		this.fileNames = CGameSave.listSaves(MissionSaveStore.currentDirectory());
		for (final String fileName : this.fileNames) {
			final String title = fileName.equalsIgnoreCase("QuickSave.w3s") ? "Quick Save"
					: fileName.substring(0, fileName.length() - 4);
			this.saves.addItem(title, this.root, this.viewport);
		}
		this.saves.setSelectedIndex(-1);
		this.scrollbar.setValue(this.root, this.viewport, 100);
		updateSelection();
	}

	private void updateSelection() {
		final boolean selected = this.saves.getSelectedItem() != null;
		button("SaveGameDeleteButton").setEnabled(selected);
		button("LoadGameLoadButton").setEnabled(selected);
	}

	private File selectedFile() {
		final int selected = this.saves.getSelectedIndex();
		return selected < 0 ? null : new File(MissionSaveStore.currentDirectory(), this.fileNames.get(selected));
	}

	private void requestSave() {
		try {
			final File slot = MissionSaveStore.namedSlot(this.name.getText());
			final String existing = CGameSave.listSaves(slot.getParentFile()).stream()
					.filter(file -> file.equalsIgnoreCase(slot.getName())).findFirst().orElse(null);
			if (existing == null) {
				save(slot);
			}
			else {
				this.pendingFile = new File(slot.getParentFile(), existing);
				this.root.setText((StringFrame) frame("OverwriteMessageText"), existing);
				show(this.overwrite);
			}
		}
		catch (final IllegalArgumentException error) {
			this.showError.accept(error.getMessage());
		}
	}

	private void save(final File slot) {
		try {
			this.save.run(slot);
			hide();
			this.returnToMenu.run();
		}
		catch (final IOException error) {
			this.showError.accept(error.getMessage());
		}
	}

	private void loadSelected() {
		final File slot = selectedFile();
		if (slot == null) return;
		try {
			this.load.run(slot);
		}
		catch (final IOException error) {
			this.showError.accept(error.getMessage());
		}
	}

	private void requestDelete() {
		this.pendingFile = selectedFile();
		if (this.pendingFile == null) return;
		this.root.setText((StringFrame) frame("DeleteMessageText"), this.pendingFile.getName());
		show(this.delete);
	}

	private void deleteSelected() {
		try {
			Files.deleteIfExists(this.pendingFile.toPath());
			this.pendingFile = null;
			refreshList();
			show(this.browser);
		}
		catch (final IOException error) {
			this.showError.accept(error.getMessage());
		}
	}
}

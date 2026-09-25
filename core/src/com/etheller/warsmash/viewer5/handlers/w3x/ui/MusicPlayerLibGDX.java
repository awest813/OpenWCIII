package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.DataSourceFileHandle;

public class MusicPlayerLibGDX implements MusicPlayer {
	private static final float GAME_MSECS_DIVISOR = 1000f;
	private Music[] currentMusics;
	private int currentMusicIndex;
	private boolean currentMusicRandomizeIndex;
	private boolean currentMusicActive;
	private final DataSource dataSource;
	private final DataTable musicSLK;
	private float volume = 1.0f;
	private String defaultMusicField;
	private boolean defaultMusicRandom;
	private int defaultMusicIndex;
	private float fadeDuration;
	private float fadeElapsed;

	public MusicPlayerLibGDX(DataSource dataSource, DataTable musicSLK) {
		this.dataSource = dataSource;
		this.musicSLK = musicSLK;
	}

	@Override
	public void update() {
		update(Gdx.graphics == null ? 0f : Gdx.graphics.getDeltaTime());
	}

	void update(final float deltaTime) {
		if (this.currentMusicActive) {
			if (this.fadeElapsed < this.fadeDuration) {
				this.fadeElapsed = Math.min(this.fadeDuration, this.fadeElapsed + Math.max(0f, deltaTime));
				applyVolume();
			}
			if (this.currentMusics == null) {
				playDefaultMusic();
			}
			if (this.currentMusics != null) {
				if ((this.currentMusics[this.currentMusicIndex] != null)
						&& !this.currentMusics[this.currentMusicIndex].isPlaying()) {
					if (this.currentMusicRandomizeIndex) {
						this.currentMusicIndex = (int) (Math.random() * this.currentMusics.length);
					}
					else {
						this.currentMusicIndex = (this.currentMusicIndex + 1) % this.currentMusics.length;
					}
					if (this.currentMusics[this.currentMusicIndex] != null) {
						this.currentMusics[this.currentMusicIndex].play();
					}
				}
			}
		}
	}

	@Override
	public void setMusicPosition(int positionMSecs) {
		if (this.currentMusics != null) {
			if (this.currentMusics[this.currentMusicIndex] != null) {
				this.currentMusics[this.currentMusicIndex].setPosition(positionMSecs / GAME_MSECS_DIVISOR);
			}
		}
	}

	@Override
	public void stopMusic() {
		this.currentMusicActive = false;
		if (this.currentMusics != null) {
			for (final Music music : this.currentMusics) {
				if (music != null) {
					music.pause();
				}
			}
		}
	}

	@Override
	public void setVolume(int volume) {
		this.volume = Math.max(0, Math.min(127, volume)) / 127f;
		applyVolume();
	}

	private void applyVolume() {
		final float volumeFloat = this.volume * (this.fadeDuration > 0f ? this.fadeElapsed / this.fadeDuration : 1f);
		if (this.currentMusics != null) {
			for (final Music music : this.currentMusics) {
				if (music != null) {
					music.setVolume(volumeFloat);
				}
			}
		}
	}

	@Override
	public void resumeMusic() {
		this.currentMusicActive = true;
		if (this.currentMusics != null) {
			this.currentMusics[this.currentMusicIndex].play();
		}
	}

	@Override
	public Music setDefaultMusic(String musicField, boolean random, int index) {
		this.defaultMusicField = musicField;
		this.defaultMusicRandom = random;
		this.defaultMusicIndex = index;
		return null;
	}

	@Override
	public Music playDefaultMusic() {
		return playMusicEx(this.defaultMusicField, this.defaultMusicRandom, this.defaultMusicIndex, 0, -1);
	}

	@Override
	public Music playMusicEx(String musicField, boolean random, int index, int fromMSecs, int fadeInMSecs) {
		stopMusic();
		if (this.currentMusics != null) {
			for (final Music music : this.currentMusics) {
				music.dispose();
			}
			this.currentMusics = null;
		}
		if ((musicField == null) || musicField.trim().isEmpty()) {
			return null;
		}
		this.fadeDuration = Math.max(0, fadeInMSecs) / GAME_MSECS_DIVISOR;
		this.fadeElapsed = 0f;

		final String[] semicolonMusics = musicField.split(";");
		final List<String> musicPaths = new ArrayList<>();
		for (String musicPath : semicolonMusics) {
			// dumb support for comma as well as semicolon, I wonder if we can
			// clean this up, simplify?
			if (this.musicSLK.get(musicPath) != null) {
				musicPath = this.musicSLK.get(musicPath).getField("FileNames");
			}
			final String[] moreSplitMusics = musicPath.split(",");
			for (final String finalSplitPath : moreSplitMusics) {
				if (!finalSplitPath.trim().isEmpty()) {
					musicPaths.add(finalSplitPath.trim());
				}
			}
		}
		final String[] musics = musicPaths.toArray(new String[musicPaths.size()]);

		this.currentMusics = new Music[musics.length];
		int validMusicCount = 0;
		for (int i = 0; i < musics.length; i++) {
			if (this.dataSource.has(musics[i])) {
				final Music newMusic = Gdx.audio.newMusic(new DataSourceFileHandle(this.dataSource, musics[i]));
				newMusic.setVolume(this.fadeDuration > 0f ? 0f : this.volume);
				this.currentMusics[i] = newMusic;
				validMusicCount++;
			}
		}
		if (this.currentMusics.length != validMusicCount) {
			final Music[] fixedList = new Music[validMusicCount];
			int fixedListIndex = 0;
			for (int i = 0; i < this.currentMusics.length; i++) {
				if (this.currentMusics[i] != null) {
					fixedList[fixedListIndex++] = this.currentMusics[i];
				}
			}
			this.currentMusics = fixedList;
		}
		if (validMusicCount == 0) {
			this.currentMusics = null;
			return null;
		}
		index = Math.floorMod(index, validMusicCount);
		if (random) {
			index = (int) (Math.random() * this.currentMusics.length);
		}
		this.currentMusicIndex = index;
		this.currentMusicRandomizeIndex = random;
		this.currentMusicActive = true;
		if (this.currentMusics[index] != null) {
			this.currentMusics[index].play();
			if (fromMSecs > 0) {
				this.currentMusics[index].setPosition(fromMSecs / GAME_MSECS_DIVISOR);
			}
			return this.currentMusics[index];
		}
		return null;
	}
}

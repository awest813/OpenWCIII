package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityEntangledMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityOverlayedMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.vision.CPlayerFogOfWar;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;

public class MeleeUIMinimap {
	private static final float HERO_STEP = 0.01f;
	private final Rectangle minimap;
	private final Rectangle minimapFilledArea;
	private final Texture minimapTexture;
	private final Rectangle playableMapArea;
	private final Texture[] teamColors;
	private final Texture[] specialIcons;
	private float heroAlpha = 0.90f;
	private byte polarity = -1;
	private boolean visible = true;
	private final List<MinimapPing> pings = new ArrayList<>();

	public MeleeUIMinimap(final Rectangle displayArea, final Rectangle playableMapArea, final Texture minimapTexture,
			final Texture[] teamColors, final Texture[] specialIcons) {
		this.playableMapArea = playableMapArea;
		this.minimapTexture = minimapTexture;
		this.teamColors = teamColors;
		this.minimap = displayArea;
		this.specialIcons = specialIcons;
		final float worldWidth = playableMapArea.getWidth();
		final float worldHeight = playableMapArea.getHeight();
		final float worldSize = Math.max(worldWidth, worldHeight);
		final float minimapFilledWidth = (worldWidth / worldSize) * this.minimap.width;
		final float minimapFilledHeight = (worldHeight / worldSize) * this.minimap.height;

		this.minimapFilledArea = new Rectangle(this.minimap.x + ((this.minimap.width - minimapFilledWidth) / 2),
				this.minimap.y + ((this.minimap.height - minimapFilledHeight) / 2), minimapFilledWidth,
				minimapFilledHeight);
	}

	public void ping(final float worldX, final float worldY, final float durationSeconds, final float r, final float g,
			final float b) {
		final float durationMs = Math.max(0.25f, durationSeconds) * 1000f;
		this.pings.add(new MinimapPing(worldX, worldY, durationMs, r, g, b));
	}

	public void update(final float dtSeconds) {
		final float dtMs = dtSeconds * 1000f;
		final Iterator<MinimapPing> it = this.pings.iterator();
		while (it.hasNext()) {
			final MinimapPing ping = it.next();
			ping.remainingMs -= dtMs;
			if (ping.remainingMs <= 0f) {
				it.remove();
			}
		}
	}

	public void render(final CSimulation game, final SpriteBatch batch, final Iterable<RenderUnit> units,
			final PathingGrid pathingGrid, final CPlayerFogOfWar fogOfWar, final CPlayer player) {
		if (!this.visible) {
			return;
		}
		batch.draw(this.minimapTexture, this.minimap.x, this.minimap.y, this.minimap.width, this.minimap.height);
		final Color og = batch.getColor();

		final int minX = pathingGrid.getFogOfWarIndexX(this.playableMapArea.getX());
		final int minY = pathingGrid.getFogOfWarIndexY(this.playableMapArea.getY());
		final int maxX = pathingGrid.getFogOfWarIndexX(this.playableMapArea.getX() + this.playableMapArea.getWidth());
		final int maxY = pathingGrid.getFogOfWarIndexY(this.playableMapArea.getY() + this.playableMapArea.getHeight());
		final float mapXMod = this.minimapFilledArea.width / (maxX - minX);
		final float mapYMod = this.minimapFilledArea.height / (maxY - minY);

		for (int y = 0; y < (maxY - minY); y++) {
			for (int x = 0; x < (maxX - minX); x++) {
				final CFogState state = fogOfWar.getFogState(game, x + minX, y + minY);
				if (CFogState.FOGGED.equals(state)) {
					batch.setColor(0f, 0f, 0f, 0.5f);
					batch.draw(this.teamColors[0], this.minimapFilledArea.x + (x * mapXMod),
							this.minimapFilledArea.y + (y * mapYMod), mapXMod, mapYMod);
				}
				else if (CFogState.MASKED.equals(state)) {
					batch.setColor(0f, 0f, 0f, 1f);
					batch.draw(this.teamColors[0], this.minimapFilledArea.x + (x * mapXMod),
							this.minimapFilledArea.y + (y * mapYMod), mapXMod, mapYMod);
				}
			}
		}

		this.heroAlpha += HERO_STEP * this.polarity;
		if ((this.heroAlpha <= 0.5) || (this.heroAlpha >= 0.95)) {
			this.polarity *= -1;
		}

		for (final RenderUnit unit : units) {
			final CUnit simUnit = unit.getSimulationUnit();
			int dimensions = 4;
			if (!simUnit.isHidden() && !simUnit.isDead() && simUnit.isVisible(game, player.getId())) {
				batch.setColor(1, 1, 1, 1);
				final Texture minimapIcon;
				if (simUnit.getGoldMineData() != null) {
					minimapIcon = this.specialIcons[0];
					dimensions = 21;
				}
				else if (simUnit.getOverlayedGoldMineData() != null) {
					final CAbilityOverlayedMine overlayedGoldMineData = simUnit.getOverlayedGoldMineData();
					if (overlayedGoldMineData instanceof CAbilityEntangledMine) {
						minimapIcon = this.specialIcons[3];
					}
					else {
						minimapIcon = this.specialIcons[4];
					}
					dimensions = 21;
				}
				else if (simUnit.getUnitType().isNeutralBuildingShowMinimapIcon()) {
					minimapIcon = this.specialIcons[1];
					dimensions = 21;
				}
				else if (simUnit.isHero()) {
					if (player.hasAlliance(simUnit.getPlayerIndex(), CAllianceType.PASSIVE)) {
						batch.setColor(1f, 1f, 1f, this.heroAlpha);
					}
					else {
						batch.setColor(1f, 0.2f, 0.2f, this.heroAlpha);
					}
					minimapIcon = this.specialIcons[2];
					dimensions = 28;
				}
				else {
					if (simUnit.isBuilding()) {
						dimensions = 10;
					}
					minimapIcon = this.teamColors[unit.getSimulationUnit().getPlayerIndex()];
				}
				final int offset = dimensions / 2;
				batch.draw(minimapIcon,
						(this.minimapFilledArea.x + (((unit.location[0] - this.playableMapArea.getX())
								/ (this.playableMapArea.getWidth())) * this.minimapFilledArea.width)) - offset,
						(this.minimapFilledArea.y + (((unit.location[1] - this.playableMapArea.getY())
								/ (this.playableMapArea.getHeight())) * this.minimapFilledArea.height)) - offset,
						dimensions, dimensions);
				batch.setColor(1, 1, 1, 1);
			}
		}

		for (final MinimapPing ping : this.pings) {
			final float pulse = 0.55f + (0.45f * (float) Math.sin(ping.remainingMs * 0.02f));
			batch.setColor(ping.r, ping.g, ping.b, pulse);
			final float size = 12f + (6f * pulse);
			final float px = this.minimapFilledArea.x + (((ping.worldX - this.playableMapArea.getX())
					/ this.playableMapArea.getWidth()) * this.minimapFilledArea.width) - (size * 0.5f);
			final float py = this.minimapFilledArea.y + (((ping.worldY - this.playableMapArea.getY())
					/ this.playableMapArea.getHeight()) * this.minimapFilledArea.height) - (size * 0.5f);
			batch.draw(this.teamColors[0], px, py, size, size);
		}
		batch.setColor(og);
	}

	public Vector2 getWorldPointFromScreen(final float screenX, final float screenY) {
		final Rectangle filledArea = this.minimapFilledArea;
		final float clickX = (screenX - filledArea.x) / filledArea.width;
		final float clickY = (screenY - filledArea.y) / filledArea.height;
		final float worldX = (clickX * this.playableMapArea.width) + this.playableMapArea.x;
		final float worldY = (clickY * this.playableMapArea.height) + this.playableMapArea.y;
		return new Vector2(worldX, worldY);

	}

	public boolean containsMouse(final float x, final float y) {
		return this.minimapFilledArea.contains(x, y);
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	private static final class MinimapPing {
		private final float worldX;
		private final float worldY;
		private float remainingMs;
		private final float r;
		private final float g;
		private final float b;

		private MinimapPing(final float worldX, final float worldY, final float remainingMs, final float r,
				final float g, final float b) {
			this.worldX = worldX;
			this.worldY = worldY;
			this.remainingMs = remainingMs;
			this.r = r;
			this.g = g;
			this.b = b;
		}
	}
}

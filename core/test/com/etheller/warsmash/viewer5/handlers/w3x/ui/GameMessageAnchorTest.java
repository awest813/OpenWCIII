package com.etheller.warsmash.viewer5.handlers.w3x.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.badlogic.gdx.utils.viewport.FitViewport;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;

class GameMessageAnchorTest {
	@Test
	void usesScriptedOffsetsInTheGameMessageCoordinateSpace() {
		final var viewport = new FitViewport(800, 600);
		final var anchor = MeleeUI.gameMessageAnchor(viewport, 0.2f, -0.1f);

		assertEquals(FramePoint.LEFT, anchor.getMyPoint());
		assertEquals(200, anchor.getX(), 0.001f);
		assertEquals(-100, anchor.getY(), 0.001f);
	}

	@Test
	void keepsTheExistingAnchorForOrdinaryMessagesAndIgnoresInvalidOffsets() {
		final var viewport = new FitViewport(800, 600);
		final var ordinary = MeleeUI.gameMessageAnchor(viewport, 0, 0);
		final var invalid = MeleeUI.gameMessageAnchor(viewport, Float.NaN, Float.POSITIVE_INFINITY);

		assertEquals(FramePoint.LEFT, ordinary.getMyPoint());
		assertEquals(0, ordinary.getX());
		assertEquals(0, ordinary.getY());
		assertEquals(ordinary.getX(), invalid.getX());
		assertEquals(ordinary.getY(), invalid.getY());
	}
}

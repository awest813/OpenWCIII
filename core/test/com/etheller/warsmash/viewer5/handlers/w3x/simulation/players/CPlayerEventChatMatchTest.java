package com.etheller.warsmash.viewer5.handlers.w3x.simulation.players;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CPlayerEventChatMatchTest {

	@Test
	void emptyMatchAcceptsAnyMessage() {
		assertTrue(CPlayerEvent.matchesChatMessage("anything", "", false));
		assertTrue(CPlayerEvent.matchesChatMessage("", "", false));
	}

	@Test
	void substringMatchIsCaseInsensitive() {
		assertTrue(CPlayerEvent.matchesChatMessage("-Gold 1000", "gold", false));
		assertFalse(CPlayerEvent.matchesChatMessage("-lumber", "gold", false));
	}

	@Test
	void exactMatchRequiresWholeString() {
		assertTrue(CPlayerEvent.matchesChatMessage("-gold", "-gold", true));
		assertTrue(CPlayerEvent.matchesChatMessage("-GOLD", "-gold", true));
		assertFalse(CPlayerEvent.matchesChatMessage("-gold 500", "-gold", true));
	}
}

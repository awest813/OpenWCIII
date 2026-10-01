package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CampaignPresentationEventsTest {
    @Test void retryAndQuitAreMutuallyExclusiveAndOldChoicesCannotAffectNewMenu() {
        final CampaignPresentationEvents events = new CampaignPresentationEvents();
        final List<String> transitions = new ArrayList<>();
        final Runnable[] defeat = events.replaceContinuations(() -> transitions.add("retry"), () -> transitions.add("quit"));
        defeat[0].run();
        defeat[1].run();
        defeat[0].run();
        assertEquals(List.of("retry"), transitions);
        final Runnable[] victory = events.replaceContinuations(() -> transitions.add("next"), () -> transitions.add("menu"));
        defeat[1].run();
        victory[1].run();
        victory[0].run();
        assertEquals(List.of("retry", "menu"), transitions);
    }

    @Test void otherPlayersAndNullPlayerDoNotShowLocalOutcome() {
        final List<String> screens = new ArrayList<>();
        CampaignPresentationEvents.presentForPlayer(1, 0, () -> screens.add("enemy defeat"));
        CampaignPresentationEvents.presentForPlayer(-1, 0, () -> screens.add("null player"));
        CampaignPresentationEvents.presentForPlayer(0, 0, () -> screens.add("local victory"));
        assertEquals(List.of("local victory"), screens);
    }

    @Test void replacingDialogInvalidatesOldContinueAndNewContinueRunsOnce() {
        final CampaignPresentationEvents events = new CampaignPresentationEvents();
        final List<String> transitions = new ArrayList<>();
        final Runnable old = events.replaceContinuation(() -> transitions.add("menu"));
        final Runnable next = events.replaceContinuation(() -> transitions.add("next chapter"));
        old.run();
        assertTrue(transitions.isEmpty());
        next.run();
        next.run();
        old.run();
        assertEquals(List.of("next chapter"), transitions);
    }

    @Test void leavingWithoutDialogCancelsOldContinue() {
        final CampaignPresentationEvents events = new CampaignPresentationEvents();
        final Runnable old = events.replaceContinuation(() -> fail("Stale dialog must not leave the new map"));
        events.cancelContinuation();
        old.run();
    }

    @Test void continuationCanReplaceItselfWithoutBeingErased() {
        final CampaignPresentationEvents events = new CampaignPresentationEvents();
        final int[] calls = {0};
        final Runnable[] second = {null};
        final Runnable first = events.replaceContinuation(() -> {
            calls[0]++;
            second[0] = events.replaceContinuation(() -> calls[0]++);
        });
        first.run();
        first.run();
        second[0].run();
        second[0].run();
        assertEquals(2, calls[0]);
    }
}

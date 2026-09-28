package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuDialogStateTest {
    @Test void responseHidesOldDialogBeforeOpeningFollowup() {
        final List<String> actions = new ArrayList<>();
        final MenuDialogState state = new MenuDialogState(() -> actions.add("hide"));
        state.open(() -> { actions.add("followup"); state.open(null, null); }, null);
        state.respond(true);
        assertEquals(List.of("hide", "followup"), actions);
        assertTrue(state.isOpen());
    }

    @Test void staleAndRepeatedResponsesCannotRunAnotherAction() {
        final int[] callbacks = {0};
        final MenuDialogState state = new MenuDialogState(() -> {});
        final long old = state.open(() -> fail("Old action"), null);
        final long current = state.open(() -> callbacks[0]++, () -> fail("Wrong answer"));
        state.respond(old, true);
        assertTrue(state.isOpen());
        state.respond(current, true);
        state.respond(current, true);
        state.respond(current, false);
        assertEquals(1, callbacks[0]);
        assertFalse(state.isOpen());
    }

    @Test void cancellationRunsOnlyTheCancelCallbackAndDismissesFirst() {
        final List<String> actions = new ArrayList<>();
        final MenuDialogState state = new MenuDialogState(() -> actions.add("hide"));
        state.open(() -> fail("Accepted cancellation"), () -> actions.add("cancel"));
        state.respond(false);
        assertEquals(List.of("hide", "cancel"), actions);
        assertFalse(state.isOpen());
    }
}

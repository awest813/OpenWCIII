package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

/** Local presentation routing and replaceable, single-use outcome continuations. */
public final class CampaignPresentationEvents {
    private long generation;
    private Runnable continuation;

    public static void presentForPlayer(int playerIndex, int localPlayerIndex, Runnable presentation) {
        if (playerIndex >= 0 && playerIndex == localPlayerIndex) presentation.run();
    }

    public Runnable replaceContinuation(Runnable action) {
        final long token = ++this.generation;
        this.continuation = action;
        return () -> {
            if (token != this.generation || this.continuation == null) return;
            final Runnable current = this.continuation;
            this.continuation = null;
            current.run();
        };
    }

    public void cancelContinuation() {
        ++this.generation;
        this.continuation = null;
    }
}

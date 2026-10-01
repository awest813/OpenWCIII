package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

/** Local presentation routing and replaceable, single-use outcome continuations. */
public final class CampaignPresentationEvents {
    private long generation;
    private boolean continuationPending;

    public static void presentForPlayer(int playerIndex, int localPlayerIndex, Runnable presentation) {
        if (playerIndex >= 0 && playerIndex == localPlayerIndex) presentation.run();
    }

    public Runnable replaceContinuation(Runnable action) {
        return replaceContinuations(action)[0];
    }

    /** Alternative buttons share one decision: choosing any invalidates all others. */
    public Runnable[] replaceContinuations(Runnable... actions) {
        final long token = ++this.generation;
        this.continuationPending = true;
        final Runnable[] guarded = new Runnable[actions.length];
        for (int i = 0; i < actions.length; i++) {
            final Runnable action = actions[i];
            guarded[i] = () -> {
                if (token != this.generation || !this.continuationPending) return;
                this.continuationPending = false;
                if (action != null) action.run();
            };
        }
        return guarded;
    }

    public void cancelContinuation() {
        ++this.generation;
        this.continuationPending = false;
    }
}

package com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog;

/** A dialog response closes the old dialog before running code that may open another. */
public final class MenuDialogState {
    private long generation;
    private boolean open;
    private Runnable accept;
    private Runnable cancel;
    private final Runnable dismiss;

    public MenuDialogState(Runnable dismiss) { this.dismiss = dismiss; }

    public long open(Runnable accept, Runnable cancel) {
        this.accept = accept;
        this.cancel = cancel;
        this.open = true;
        return ++this.generation;
    }

    public boolean isOpen() { return this.open; }

    public void respond(boolean accepted) { respond(this.generation, accepted); }

    public void respond(long token, boolean accepted) {
        if (!this.open || token != this.generation) return;
        final Runnable action = accepted ? this.accept : this.cancel;
        this.open = false;
        this.accept = null;
        this.cancel = null;
        this.dismiss.run();
        if (action != null) action.run();
    }
}

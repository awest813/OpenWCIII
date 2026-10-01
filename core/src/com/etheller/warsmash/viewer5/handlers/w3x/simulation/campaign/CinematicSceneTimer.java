package com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign;

/** Transmission lifetime, independent of portrait animation restarts. */
public final class CinematicSceneTimer {
    private double remaining;

    public void start(float seconds) {
        this.remaining = Float.isFinite(seconds) ? Math.max(0, seconds) : 0;
    }

    public void advance(float seconds) {
        if (Float.isFinite(seconds) && seconds > 0) this.remaining = Math.max(0, this.remaining - seconds);
    }

    public double getRemaining() { return this.remaining; }

    public void restore(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid cinematic timer");
        this.remaining = seconds;
    }

    public boolean isActive() { return this.remaining > 0; }

    public void end() { this.remaining = 0; }
}

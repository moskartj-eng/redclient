package de.redclient;

/** Weiches Scrollen: Mausrad setzt ein Ziel, die angezeigte Position gleitet framerate-unabhaengig dorthin. */
public final class SmoothScroll {
    private double target;
    private double current;
    private long last = System.nanoTime();

    /** Wie schnell die Animation ist (hoeher = knackiger). */
    private final double speed;

    public SmoothScroll() {
        this(14.0);
    }

    public SmoothScroll(double speed) {
        this.speed = speed;
    }

    public void add(double delta, double max) {
        target = clamp(target + delta, max);
    }

    public void reset() {
        target = 0;
        current = 0;
    }

    /** Einmal pro Frame aufrufen. */
    public void update(double max) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1_000_000_000.0);
        last = now;
        target = clamp(target, max);
        current = clamp(current, max + 40); // kurz ueber das Ende hinaus erlaubt, wird weich zurueckgezogen
        current += (target - current) * (1.0 - Math.exp(-dt * speed));
        if (Math.abs(target - current) < 0.01) current = target;
    }

    public double get() {
        return current;
    }

    public double target() {
        return target;
    }

    private static double clamp(double v, double max) {
        return Math.max(0, Math.min(max, v));
    }
}

package com.jack.bigphoneplayer;

/** Two continuous full-text loops; a third copy enters and settles at the origin. */
public final class MarqueeCycle {
    public static final long START_HOLD_MS=1000;
    private final long scrollMs;
    public MarqueeCycle(float periodPixels,float density) {
        if (!Float.isFinite(periodPixels) || periodPixels<=0 || !Float.isFinite(density) || density<=0)
            throw new IllegalArgumentException("Invalid scrolling dimensions");
        scrollMs=Math.max(1000,Math.round(periodPixels/(30d*density)*1000));
    }
    public long passMs() { return scrollMs; }
    public long totalMs() { return START_HOLD_MS+2*scrollMs; }
    public boolean finished(long elapsed) { return elapsed>=totalMs(); }
    /** Absolute travel in periods, never a discontinuous modulo/reset. */
    public float fraction(long elapsed) {
        double travel=Math.max(0,Math.min(2,(elapsed-START_HOLD_MS)/(double)scrollMs));
        // Ease gently to rest during the final 400 ms without changing the loop length.
        double ease=Math.min(.4,400d/scrollMs),start=2-ease;
        if (travel>start) {
            double t=(travel-start)/ease;
            travel=start+ease*(t+t*t-t*t*t);
        }
        return (float)travel;
    }
}

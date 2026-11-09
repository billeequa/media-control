package com.jack.bigphoneplayer;

/** Two full start-to-end passes, then a static ellipsized line. */
public final class MarqueeCycle {
    public static final long START_HOLD_MS=1000, END_HOLD_MS=800;
    private final long scrollMs;
    public MarqueeCycle(float overflowPixels,float density) {
        if (!Float.isFinite(overflowPixels) || overflowPixels<0 || !Float.isFinite(density) || density<=0)
            throw new IllegalArgumentException("Invalid scrolling dimensions");
        scrollMs=Math.max(1000,Math.round(overflowPixels/(30d*density)*1000));
    }
    public long passMs() { return START_HOLD_MS+scrollMs+END_HOLD_MS; }
    public long totalMs() { return 2*passMs(); }
    public boolean finished(long elapsed) { return elapsed>=totalMs(); }
    public float fraction(long elapsed) {
        if (elapsed<=0 || finished(elapsed)) return 0;
        long phase=elapsed%passMs();
        return Math.max(0,Math.min(1,(phase-START_HOLD_MS)/(float)scrollMs));
    }
}

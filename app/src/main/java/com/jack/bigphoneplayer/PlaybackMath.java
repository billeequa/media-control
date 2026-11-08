package com.jack.bigphoneplayer;

/** Pure position math: no polling or network dependency. */
public final class PlaybackMath {
    private PlaybackMath() {}
    public static long position(long reported, long updatedAt, long now, float speed,
                                boolean playing, long duration) {
        if (reported < 0) return -1;
        double value = reported;
        if (playing && updatedAt > 0 && Float.isFinite(speed)) {
            value += Math.max(0, now - updatedAt) * (double) speed;
        }
        long result = (long) Math.max(0, Math.min(Long.MAX_VALUE, value));
        return duration > 0 ? Math.min(result, duration) : result;
    }
    public static String time(long millis) {
        if (millis < 0) return "—";
        long seconds = millis / 1000;
        return seconds >= 3600
            ? String.format(java.util.Locale.ROOT, "%d:%02d:%02d", seconds/3600, seconds/60%60, seconds%60)
            : String.format(java.util.Locale.ROOT, "%d:%02d", seconds/60, seconds%60);
    }
}

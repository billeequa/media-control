package com.jack.bigphoneplayer;

import java.util.Objects;

/** Immediate restart, then previous on a second press of the same track. */
public final class PreviousPress {
    public enum Action { NONE, RESTART, PREVIOUS }
    public static final long DOUBLE_PRESS_MS = 3000;
    private boolean armed;
    private long lastPress;
    private Object session;
    private String track;

    public Action press(long now, Object currentSession, String currentTrack, boolean canSeek, boolean canPrevious) {
        if (!canSeek) { reset(); return canPrevious ? Action.PREVIOUS : Action.NONE; }
        long delta = now - lastPress;
        if (armed && delta >= 0 && delta <= DOUBLE_PRESS_MS && Objects.equals(session,currentSession)
                && Objects.equals(track,currentTrack) && canPrevious) {
            reset(); return Action.PREVIOUS;
        }
        armed = true; lastPress = now; session = currentSession; track = currentTrack;
        return Action.RESTART;
    }

    public void reset() { armed = false; session = null; track = null; }
}

package com.jack.bigphoneplayer;

import android.media.MediaDescription;
import android.media.session.MediaSession;
import java.util.List;

/** Keeps at most one upcoming queue description, using it only after identity is confirmed. */
final class QueuePreload {
    private long nextId = MediaSession.QueueItem.UNKNOWN_ID;
    private long currentId = MediaSession.QueueItem.UNKNOWN_ID;
    private MediaDescription next, current;

    void clear() {
        nextId = currentId = MediaSession.QueueItem.UNKNOWN_ID;
        next = current = null;
    }

    void update(List<MediaSession.QueueItem> queue, long activeId) {
        if (activeId == MediaSession.QueueItem.UNKNOWN_ID) { clear(); return; }
        if (currentId != activeId) {
            current = nextId == activeId ? next : null;
            currentId = current == null ? MediaSession.QueueItem.UNKNOWN_ID : activeId;
        }
        next = null;
        nextId = MediaSession.QueueItem.UNKNOWN_ID;
        if (queue == null) return;
        for (int i = 0; i + 1 < queue.size(); i++) {
            if (queue.get(i).getQueueId() == activeId) {
                MediaSession.QueueItem item = queue.get(i + 1);
                MediaDescription description = item.getDescription();
                if (item.getQueueId() != MediaSession.QueueItem.UNKNOWN_ID
                        && description != null && description.getMediaId() != null
                        && !description.getMediaId().isEmpty()) {
                    nextId = item.getQueueId();
                    next = description;
                }
                return;
            }
        }
    }

    MediaDescription match(long activeId, String mediaId) {
        if (current == null || activeId != currentId || mediaId == null || mediaId.isEmpty()) return null;
        return mediaId.equals(current.getMediaId()) ? current : null;
    }
}

package com.jack.bigphoneplayer;
import android.service.notification.NotificationListenerService;

/** System-bound service; never reads or stores notification text. */
public class MediaListener extends NotificationListenerService {
    @Override public void onListenerConnected() { MediaHub.get(this).connect(); }
    @Override public void onListenerDisconnected() { MediaHub.get(this).disconnect(); }
    @Override public void onDestroy() { MediaHub.get(this).disconnect(); super.onDestroy(); }
}

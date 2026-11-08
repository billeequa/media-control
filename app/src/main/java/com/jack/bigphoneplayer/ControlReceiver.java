package com.jack.bigphoneplayer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ControlReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        // Validate the session identity captured when the notification was built.
        // An old notification must never redirect a tap to a newly chosen app.
        MediaHub.get(context).command(intent.getAction(), intent.getStringExtra("source"));
    }
}

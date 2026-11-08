package com.jack.bigphoneplayer;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.session.*;
import android.media.session.MediaController;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements MediaHub.Observer {
    private static final int BG = Color.rgb(16,18,15), PANEL = Color.rgb(37,41,34), ACCENT = Color.rgb(212,246,107), WHITE = Color.rgb(245,247,239), MUTED = Color.rgb(163,172,155);
    private MediaHub hub;
    private TextView title, artist, timing, status;
    private Button source, access, notifications, restart, toggle, next;
    private ProgressBar progress;
    private ImageView artwork;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { @Override public void run() { updateProgress(); main.postDelayed(this, 500); } };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); hub = MediaHub.get(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(24),dp(18),dp(24),dp(24));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-1));
        // Keep touch targets away from system gestures when edge-to-edge is enforced.
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        TextView heading = text("BIG PHONE PLAYER",14,MUTED); heading.setLetterSpacing(.13f); body.addView(heading);
        source = button("Choose a music app",WHITE,PANEL); source.setTextSize(16); source.setOnClickListener(v -> chooseSource());
        body.addView(source,params(-1,56,16));
        access = button("Enable media access",BG,ACCENT); access.setOnClickListener(v -> explainAccess()); body.addView(access,params(-1,56,12));
        notifications = button("Enable notification controls",WHITE,PANEL); notifications.setTextSize(15); notifications.setOnClickListener(v -> enableNotifications()); body.addView(notifications,params(-1,52,8));
        artwork = new ImageView(this); artwork.setScaleType(ImageView.ScaleType.CENTER_CROP); artwork.setContentDescription("Album artwork");
        LinearLayout.LayoutParams artParams = params(100,100,28); artParams.gravity = Gravity.START; body.addView(artwork,artParams);
        title = text("Start your music",34,WHITE); title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); title.setMaxLines(4); body.addView(title,params(-1,-2,24));
        artist = text("",20,MUTED); body.addView(artist,params(-1,-2,8));
        status = text("",14,MUTED); body.addView(status,params(-1,-2,14));
        View spacer = new View(this); body.addView(spacer,new LinearLayout.LayoutParams(1,dp(22),1));
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(1000); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT)); progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        body.addView(progress,params(-1,8,24));
        timing = text("— / —",16,MUTED); timing.setTypeface(Typeface.MONOSPACE); body.addView(timing,params(-1,-2,12));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL);
        restart = button("↺\nRestart",WHITE,PANEL); restart.setContentDescription("Restart current song"); restart.setTextSize(20);
        toggle = button("▶\nPlay",BG,ACCENT); toggle.setContentDescription("Play"); toggle.setTextSize(25);
        next = button("▸|\nNext",WHITE,PANEL); next.setContentDescription("Next song"); next.setTextSize(20);
        for (Button b : new Button[]{restart,toggle,next}) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(152),1); lp.setMargins(dp(3),0,dp(3),0); controls.addView(b,lp); }
        restart.setOnClickListener(v -> hub.command(MediaHub.RESTART,hub.source())); toggle.setOnClickListener(v -> hub.command(MediaHub.PLAY,hub.source())); next.setOnClickListener(v -> hub.command(MediaHub.NEXT,hub.source()));
        body.addView(controls,params(-1,-2,24));
        TextView footer = text("Your music app plays. This screen controls.",12,MUTED); footer.setGravity(Gravity.CENTER); body.addView(footer,params(-1,-2,22));
        setContentView(scroll);
    }
    @Override protected void onResume() { super.onResume(); hub.observe(this); main.post(tick); }
    @Override protected void onPause() { main.removeCallbacks(tick); hub.unobserve(this); super.onPause(); }
    @Override public void changed() {
        title.setText(hub.title()); artist.setText(hub.artist());
        source.setText(hub.sourceLabel() + "  ▾");
        access.setVisibility(hub.hasAccess() ? View.GONE : View.VISIBLE);
        notifications.setVisibility(getSystemService(NotificationManager.class).areNotificationsEnabled() ? View.GONE : View.VISIBLE);
        Bitmap art = hub.art(); artwork.setImageBitmap(art); artwork.setVisibility(art == null ? View.GONE : View.VISIBLE);
        boolean playing = hub.playing();
        toggle.setText(playing ? "Ⅱ\nPause" : "▶\nPlay"); toggle.setContentDescription(playing ? "Pause" : "Play");
        enable(restart,hub.supports(PlaybackState.ACTION_SEEK_TO)); enable(toggle,hub.canToggle()); enable(next,hub.supports(PlaybackState.ACTION_SKIP_TO_NEXT));
        if (!hub.hasAccess()) status.setText("Enable media access to connect to your music app.");
        else if (!hub.available()) status.setText(hub.source() == null ? "Start a song in another app, then return here." : "Start playback in " + hub.sourceLabel() + ", or choose another source above.");
        else {
            PlaybackState state = hub.state(); int s = state == null ? PlaybackState.STATE_NONE : state.getState();
            String info = s == PlaybackState.STATE_BUFFERING || s == PlaybackState.STATE_CONNECTING ? "Buffering…" : s == PlaybackState.STATE_ERROR ? "The music app reported a playback error." : playing ? "Playing" : "Paused";
            if (!hub.supports(PlaybackState.ACTION_SEEK_TO)) info += " · Restart unavailable in this app";
            status.setText(info);
        }
        updateProgress();
    }
    private void updateProgress() {
        if (timing == null) return;
        long position = hub.position(), duration = hub.duration();
        timing.setText(PlaybackMath.time(position) + " / " + PlaybackMath.time(duration));
        progress.setProgress(duration > 0 && position >= 0 ? (int)(1000.0 * position / duration) : 0);
        progress.setContentDescription("Elapsed " + PlaybackMath.time(position) + " of " + PlaybackMath.time(duration));
    }
    private void chooseSource() {
        hub.refresh(); java.util.List<MediaController> list = hub.sources();
        if (list.isEmpty()) { new AlertDialog.Builder(this).setTitle("No music app available").setMessage("Start a song in foobar2000 or another music app, then return here. Media access must be enabled.").setPositiveButton("OK",null).show(); return; }
        String[] labels = new String[list.size()];
        for (int i=0;i<list.size();i++) labels[i] = hub.label(list.get(i).getPackageName());
        new AlertDialog.Builder(this).setTitle("Control which app?").setItems(labels,(d,i)->hub.select(list.get(i).getPackageName())).setNegativeButton("Cancel",null).show();
    }
    private void explainAccess() {
        new AlertDialog.Builder(this).setTitle("Connect to your music app")
            .setMessage("Android uses notification access to let this controller find other apps’ active media sessions. This permission can expose notifications. Big Phone Player does not read or save notification text and has no internet permission. Enable Big Phone Player on the next screen, then return here.")
            .setPositiveButton("Open settings",(d,i)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
            .setNegativeButton("Cancel",null).show();
    }
    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) { super.onRequestPermissionsResult(requestCode,permissions,results); hub.refresh(); }
    private void enable(Button b,boolean enabled) { b.setEnabled(enabled); b.setAlpha(enabled ? 1f : .35f); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(dp(3),1f); return v; }
    private Button button(String value,int color,int background) {
        Button b = new Button(this); b.setText(value); b.setTextColor(color); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setPadding(dp(8),dp(8),dp(8),dp(8));
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background); shape.setCornerRadius(dp(20));
        android.content.res.ColorStateList ripple = android.content.res.ColorStateList.valueOf(Color.argb(45,255,255,255));
        b.setBackground(new android.graphics.drawable.RippleDrawable(ripple,shape,null));
        b.setMinHeight(dp(48)); return b;
    }
    private LinearLayout.LayoutParams params(int width,int height,int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width < 0 ? width : dp(width),height < 0 ? height : dp(height)); lp.topMargin=dp(top); return lp; }
}

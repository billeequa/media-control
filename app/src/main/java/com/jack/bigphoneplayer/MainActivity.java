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
    private TextView title, artist, timing, totalTime, status;
    private Button source, access, notifications;
    private TransportButton previous, toggle, next;
    private ProgressBar progress;
    private ImageView artwork;
    private View playerPage, settingsPage;
    private Button playerTab, settingsTab;
    private boolean settingsVisible;
    private AppUpdater updater;
    private int displaySize;
    private int titleSize() { return new int[]{26,32,38}[displaySize]; }
    private int controlSize() { return new int[]{92,112,144}[displaySize]; }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() { @Override public void run() { updateProgress(); main.postDelayed(this, 500); } };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); hub = MediaHub.get(this);
        displaySize = Math.max(0, Math.min(2, getPreferences(MODE_PRIVATE).getInt("display_size", 1)));
        settingsVisible = state != null && state.getBoolean("settings_visible");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        FrameLayout pages = new FrameLayout(this);
        root.addView(pages, new LinearLayout.LayoutParams(-1,0,1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(10),dp(20),dp(10));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-1));
        // Keep touch targets away from system gestures when edge-to-edge is enforced.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text("BIG PHONE PLAYER",18,WHITE); heading.setLetterSpacing(.07f); heading.setMaxLines(2);
        heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1.3f));
        source = button("Choose app  ▾",WHITE,PANEL); source.setTextSize(14); source.setMaxLines(1); source.setEllipsize(android.text.TextUtils.TruncateAt.END);
        source.setMinWidth(0); source.setMinimumWidth(0); source.setContentDescription("Choose music app"); source.setOnClickListener(v -> chooseSource());
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(0,dp(48),1); sourceParams.leftMargin=dp(12); header.addView(source,sourceParams);
        body.addView(header,params(-1,-2,0));
        access = button("Enable media access",BG,ACCENT); access.setOnClickListener(v -> explainAccess()); body.addView(access,params(-1,56,12));
        notifications = button("Enable notification controls",WHITE,PANEL); notifications.setTextSize(15); notifications.setOnClickListener(v -> enableNotifications()); body.addView(notifications,params(-1,52,8));
        artwork = new CoverArtwork(this); artwork.setContentDescription("Album artwork");
        LinearLayout.LayoutParams artParams = params(-2,-2,16); artParams.gravity=Gravity.CENTER_HORIZONTAL; body.addView(artwork,artParams);
        title = text("Start your music",titleSize(),WHITE); title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); title.setMaxLines(3); body.addView(title,params(-1,-2,16));
        artist = text("",new int[]{17,19,23}[displaySize],MUTED); body.addView(artist,params(-1,-2,2));
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(1000); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT)); progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        body.addView(progress,params(-1,4,10));
        LinearLayout clock = new LinearLayout(this); clock.setGravity(Gravity.CENTER_VERTICAL);
        timing = text("—",new int[]{14,16,19}[displaySize],MUTED); timing.setTypeface(Typeface.MONOSPACE);
        totalTime = text("—",new int[]{14,16,19}[displaySize],MUTED); totalTime.setTypeface(Typeface.MONOSPACE); totalTime.setGravity(Gravity.END);
        clock.addView(timing,new LinearLayout.LayoutParams(0,-2,1)); clock.addView(totalTime,new LinearLayout.LayoutParams(0,-2,1)); body.addView(clock,params(-1,-2,5));
        status = text("",14,MUTED); status.setVisibility(View.GONE); body.addView(status,params(-1,-2,10));
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL);
        previous = new TransportButton(this,TransportButton.Symbol.PREVIOUS,false); previous.setContentDescription("Previous. Tap once to restart, twice for the previous track."); previous.setTooltipText("Previous");
        toggle = new TransportButton(this,TransportButton.Symbol.PLAY,true); toggle.setContentDescription("Play"); toggle.setTooltipText("Play");
        next = new TransportButton(this,TransportButton.Symbol.NEXT,false); next.setContentDescription("Next track"); next.setTooltipText("Next");
        for (Button control : new Button[]{previous,toggle,next}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(controlSize()),1); lp.setMargins(dp(4),0,dp(4),0); controls.addView(control,lp);
        }
        previous.setOnClickListener(v -> hub.command(MediaHub.PREVIOUS,hub.source())); toggle.setOnClickListener(v -> hub.command(MediaHub.PLAY,hub.source())); next.setOnClickListener(v -> hub.command(MediaHub.NEXT,hub.source()));
        body.addView(controls,params(-1,-2,16));
        playerPage = scroll; pages.addView(playerPage,new FrameLayout.LayoutParams(-1,-1));
        settingsPage = buildSettings(); pages.addView(settingsPage,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout tabs = new LinearLayout(this); tabs.setPadding(dp(20),dp(6),dp(20),dp(8));
        playerTab = button("Player", BG, ACCENT); settingsTab = button("Settings", WHITE, PANEL);
        for (Button tab : new Button[]{playerTab,settingsTab}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(48),1); lp.setMargins(dp(4),0,dp(4),0); tabs.addView(tab,lp);
        }
        playerTab.setOnClickListener(v -> showTab(false)); settingsTab.setOnClickListener(v -> showTab(true));
        root.addView(tabs); setContentView(root); showTab(settingsVisible);
    }
    private View buildSettings() {
        ScrollView page = new ScrollView(this); page.setFillViewport(true);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(24),dp(24),dp(24),dp(24)); page.addView(body);
        body.addView(text("Settings",32,WHITE));
        body.addView(text("Player size",21,WHITE),params(-1,-2,28));
        body.addView(text("Choose the size of the title, time and playback buttons.",16,MUTED),params(-1,-2,8));
        RadioGroup sizes = new RadioGroup(this);
        String[] labels = {"Compact", "Large", "Extra large"};
        for (int i=0;i<labels.length;i++) {
            RadioButton option = new RadioButton(this); option.setId(View.generateViewId()); option.setText(labels[i]); option.setTextColor(WHITE); option.setTextSize(19); option.setPadding(dp(8),dp(8),dp(8),dp(8)); option.setMinHeight(dp(56));
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT)); sizes.addView(option);
            if (i == displaySize) sizes.check(option.getId());
        }
        sizes.setOnCheckedChangeListener((group,id)-> {
            for (int i=0;i<group.getChildCount();i++) if (group.getChildAt(i).getId() == id && i != displaySize) {
                getPreferences(MODE_PRIVATE).edit().putInt("display_size",i).apply(); recreate(); break;
            }
        });
        body.addView(sizes,params(-1,-2,12));
        body.addView(text("Updates",21,WHITE),params(-1,-2,32));
        String version = "";
        try { version = getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch (PackageManager.NameNotFoundException ignored) { }
        body.addView(text("Installed version " + version,16,MUTED),params(-1,-2,8));
        body.addView(text("Download updates from GitHub, then approve installation in Android. Music controls work offline.",16,MUTED),params(-1,-2,12));
        Button check = button("Check for updates",BG,ACCENT); body.addView(check,params(-1,60,20));
        TextView updateStatus = text("",15,MUTED); updateStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(updateStatus,params(-1,-2,12));
        Button install = button("Install downloaded update",WHITE,PANEL); body.addView(install,params(-1,60,12));
        updater = new AppUpdater(this,check,install,updateStatus);
        body.addView(text("Media access",21,WHITE),params(-1,-2,32));
        Button media = button("Manage media access",WHITE,PANEL); media.setOnClickListener(v -> explainAccess()); body.addView(media,params(-1,56,12));
        Button alerts = button("Manage notification controls",WHITE,PANEL); alerts.setOnClickListener(v -> enableNotifications()); body.addView(alerts,params(-1,56,12));
        return page;
    }
    private void showTab(boolean settings) {
        settingsVisible = settings; playerPage.setVisibility(settings ? View.GONE : View.VISIBLE); settingsPage.setVisibility(settings ? View.VISIBLE : View.GONE);
        playerTab.setSelected(!settings); settingsTab.setSelected(settings);
        styleTab(playerTab,!settings); styleTab(settingsTab,settings);
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("settings_visible",settingsVisible); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { if (updater != null) updater.close(); super.onDestroy(); }
    @Override protected void onResume() { super.onResume(); hub.observe(this); main.post(tick); }
    @Override protected void onPause() { main.removeCallbacks(tick); hub.unobserve(this); super.onPause(); }
    @Override public void changed() {
        title.setText(hub.title()); artist.setText(hub.artist());
        source.setText(hub.sourceLabel() + "  ▾");
        access.setVisibility(hub.hasAccess() ? View.GONE : View.VISIBLE);
        notifications.setVisibility(getSystemService(NotificationManager.class).areNotificationsEnabled() ? View.GONE : View.VISIBLE);
        Bitmap art = hub.art();
        if (art == null) artwork.setImageResource(R.drawable.ic_album_placeholder); else artwork.setImageBitmap(art);
        artwork.setContentDescription(art == null ? "Album artwork unavailable" : "Album artwork");
        boolean playing = hub.playing();
        toggle.setSymbol(playing ? TransportButton.Symbol.PAUSE : TransportButton.Symbol.PLAY); toggle.setContentDescription(playing ? "Pause" : "Play"); toggle.setTooltipText(playing ? "Pause" : "Play");
        enable(previous,hub.canPrevious()); enable(toggle,hub.canToggle()); enable(next,hub.supports(PlaybackState.ACTION_SKIP_TO_NEXT));
        String message = "";
        if (!hub.hasAccess()) message = "Enable media access to connect to your music app.";
        else if (!hub.available()) message = hub.source() == null ? "Start a song in another app, then return here." : "Start playback in " + hub.sourceLabel() + ", or choose another app above.";
        else {
            PlaybackState state = hub.state(); int playback = state == null ? PlaybackState.STATE_NONE : state.getState();
            if (playback == PlaybackState.STATE_ERROR) message = "The music app reported a playback error.";
            else if (playback == PlaybackState.STATE_BUFFERING || playback == PlaybackState.STATE_CONNECTING) message = "Buffering…";
        }
        status.setText(message); status.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        updateProgress();
    }
    private void updateProgress() {
        if (timing == null) return;
        long position = hub.position(), duration = hub.duration();
        timing.setText(PlaybackMath.time(position)); totalTime.setText(PlaybackMath.time(duration));
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
            .setMessage("Android uses notification access to let this controller find other apps’ active media sessions. This permission can expose notifications. Big Phone Player does not read or save notification text. It only connects to GitHub when you check for an update. Enable Big Phone Player on the next screen, then return here.")
            .setPositiveButton("Open settings",(d,i)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
            .setNegativeButton("Cancel",null).show();
    }
    private void enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
        else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) { super.onRequestPermissionsResult(requestCode,permissions,results); hub.refresh(); }
    private void styleTab(Button tab,boolean selected) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(selected ? ACCENT : PANEL); shape.setCornerRadius(dp(24));
        tab.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(40,255,255,255)),shape,null));
        tab.setTextColor(selected ? BG : MUTED);
    }
    private void enable(Button b,boolean enabled) { b.setEnabled(enabled); b.setAlpha(enabled ? 1f : .35f); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(dp(3),1f); return v; }
    private Button button(String value,int color,int background) {
        Button b = new Button(this); b.setText(value); b.setTextColor(color); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setPadding(dp(8),dp(8),dp(8),dp(8));
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background); shape.setCornerRadius(dp(24));
        android.content.res.ColorStateList ripple = android.content.res.ColorStateList.valueOf(Color.argb(45,255,255,255));
        b.setBackground(new android.graphics.drawable.RippleDrawable(ripple,shape,null));
        b.setMinHeight(dp(48)); return b;
    }
    private LinearLayout.LayoutParams params(int width,int height,int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width < 0 ? width : dp(width),height < 0 ? height : dp(height)); lp.topMargin=dp(top); return lp; }
}

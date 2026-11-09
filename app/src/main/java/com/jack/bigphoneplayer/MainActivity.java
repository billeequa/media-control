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
    private AppPalette palette;
    private TransportDock controlsDock;
    private MediaHub hub;
    private TextView title, artist, timing, totalTime;
    private Button source;
    private TransportButton previous, toggle, next;
    private ProgressBar progress;
    private CoverArtwork artwork;
    private View metadataPanel;
    private TextClock wallClock;
    private boolean grayscaleArt,textOnlyArt,compactClock;
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
        palette=AppPalette.fromId(getSharedPreferences("appearance",MODE_PRIVATE).getInt("theme",0));
        SharedPreferences appearance=getSharedPreferences("appearance",MODE_PRIVATE);
        grayscaleArt=appearance.getBoolean("grayscale_art",false);
        textOnlyArt=appearance.getBoolean("text_only_art",false);
        compactClock=appearance.getBoolean("compact_clock",false);
        setTheme(palette.dark ? R.style.AppTheme : R.style.AppTheme_Light);
        super.onCreate(state); hub = MediaHub.get(this);
        getWindow().setStatusBarColor(palette.background); getWindow().setNavigationBarColor(palette.background);
        int barFlags=palette.dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT>=27 && !palette.dark) barFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(barFlags);
        displaySize = Math.max(0, Math.min(2, getPreferences(MODE_PRIVATE).getInt("display_size", 1)));
        settingsVisible = state != null && state.getBoolean("settings_visible");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(palette.background);
        FrameLayout pages = new FrameLayout(this);
        root.addView(pages, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(10),dp(20),dp(10));
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
        TextView heading = text("BIG PHONE PLAYER",18,palette.text); heading.setLetterSpacing(.07f); heading.setSingleLine(true); heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1.3f));
        source = button("Choose app  ▾",palette.text,palette.panel); source.setTextSize(19); source.setMaxLines(1); source.setEllipsize(android.text.TextUtils.TruncateAt.END);
        source.setMinWidth(0); source.setMinimumWidth(0); source.setContentDescription("Choose music app"); source.setOnClickListener(v -> chooseSource());
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(0,dp(48),1); sourceParams.leftMargin=dp(12); header.addView(source,sourceParams);
        body.addView(header,params(-1,-2,0));
        artwork = new CoverArtwork(this,palette); artwork.setContentDescription("Album artwork");
        artwork.setPresentation(grayscaleArt,textOnlyArt,compactClock);
        LinearLayout coverArea=new LinearLayout(this); coverArea.setOrientation(LinearLayout.VERTICAL); coverArea.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams artParams=new LinearLayout.LayoutParams(-1,compactClock ? dp(112) : 0,compactClock ? 0 : 1);
        artParams.gravity=Gravity.CENTER_HORIZONTAL; coverArea.addView(artwork,artParams);
        wallClock=new TextClock(this); wallClock.setFormat12Hour("h:mm"); wallClock.setFormat24Hour("HH:mm");
        wallClock.setTextColor(palette.text); wallClock.setGravity(Gravity.CENTER); wallClock.setSingleLine(true); wallClock.setHorizontallyScrolling(false);
        wallClock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        wallClock.setAutoSizeTextTypeUniformWithConfiguration(28,120,2,android.util.TypedValue.COMPLEX_UNIT_SP);
        wallClock.setVisibility(compactClock ? View.VISIBLE : View.GONE);
        coverArea.addView(wallClock,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(coverArea,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout metadata=new LinearLayout(this); metadata.setOrientation(LinearLayout.VERTICAL); metadata.setPadding(dp(20),0,dp(20),0);
        title = metadataLine("",titleSize(),palette.text); title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); metadata.addView(title,lineParams(title,16));
        artist = metadataLine("",new int[]{21,24,28}[displaySize],palette.muted); metadata.addView(artist,lineParams(artist,2));
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(1000); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(palette.accent)); progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.panel));
        metadata.addView(progress,params(-1,4,10));
        LinearLayout clock = new LinearLayout(this); clock.setGravity(Gravity.CENTER_VERTICAL);
        timing = text("—",new int[]{19,22,26}[displaySize],palette.muted); timing.setTypeface(Typeface.MONOSPACE);
        totalTime = text("—",new int[]{19,22,26}[displaySize],palette.muted); totalTime.setTypeface(Typeface.MONOSPACE); totalTime.setGravity(Gravity.END);
        clock.addView(timing,new LinearLayout.LayoutParams(0,-2,1)); clock.addView(totalTime,new LinearLayout.LayoutParams(0,-2,1)); metadata.addView(clock,params(-1,-2,5));
        controlsDock = new TransportDock(this,palette,dp(controlSize()));
        previous = new TransportButton(this,TransportButton.Symbol.PREVIOUS,false,palette); previous.setContentDescription("Previous. Tap once to restart, twice within three seconds for the previous track."); previous.setTooltipText("Previous");
        toggle = new TransportButton(this,TransportButton.Symbol.PLAY,true,palette); toggle.setContentDescription("Play"); toggle.setTooltipText("Play");
        next = new TransportButton(this,TransportButton.Symbol.NEXT,false,palette); next.setContentDescription("Next track"); next.setTooltipText("Next");
        for (Button control : new Button[]{previous,toggle,next}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(controlSize()),1); lp.setMargins(dp(4),0,dp(4),0); controlsDock.addControl(control,lp);
        }
        previous.setOnClickListener(v -> hub.command(MediaHub.PREVIOUS,hub.source())); toggle.setOnClickListener(v -> hub.command(MediaHub.PLAY,hub.source())); next.setOnClickListener(v -> hub.command(MediaHub.NEXT,hub.source()));
        FrameLayout player = new FrameLayout(this);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        metadataPanel=metadata; content.addView(metadata,new LinearLayout.LayoutParams(-1,-2));
        content.addView(new View(this),new LinearLayout.LayoutParams(-1,controlsDock.reservedHeight()));
        player.addView(content,new FrameLayout.LayoutParams(-1,-1));
        player.addView(controlsDock,new FrameLayout.LayoutParams(-1,-1));
        playerPage = player; pages.addView(playerPage,new FrameLayout.LayoutParams(-1,-1));
        settingsPage = buildSettings(); pages.addView(settingsPage,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout tabs = new LinearLayout(this); tabs.setPadding(dp(20),dp(6),dp(20),dp(8));
        playerTab = button("Player", palette.onAccent, palette.accent); settingsTab = button("Settings", palette.text, palette.panel);
        for (Button tab : new Button[]{playerTab,settingsTab}) {
            tab.setTextSize(19);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,dp(48),1); lp.setMargins(dp(4),0,dp(4),0); tabs.addView(tab,lp);
        }
        playerTab.setOnClickListener(v -> showTab(false)); settingsTab.setOnClickListener(v -> showTab(true));
        root.addView(tabs); setContentView(root); showTab(settingsVisible); changed();
        if(Build.VERSION.SDK_INT>=30 && getWindow().getInsetsController()!=null) {
            int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(palette.dark ? 0 : mask,mask);
        }
    }
    private Switch appearanceSwitch(String label,String key,boolean checked) {
        Switch option=new Switch(this); option.setText(label); option.setTextColor(palette.text); option.setTextSize(18);
        option.setMinHeight(dp(56)); option.setSwitchPadding(dp(12)); option.setChecked(checked);
        option.setOnCheckedChangeListener((button,value)->{
            artwork.setVisibility(View.INVISIBLE);
            getSharedPreferences("appearance",MODE_PRIVATE).edit().putBoolean(key,value).apply();
            hub.refresh(); recreate();
        });
        return option;
    }
    private View buildSettings() {
        ScrollView page = new ScrollView(this); page.setFillViewport(true);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(24),dp(24),dp(24),dp(24)); page.addView(body);
        body.addView(text("Settings",32,palette.text));
        body.addView(text("Player size",21,palette.text),params(-1,-2,28));
        body.addView(text("Choose the size of the title, time and playback buttons.",16,palette.muted),params(-1,-2,8));
        RadioGroup sizes = new RadioGroup(this);
        String[] labels = {"Compact", "Large", "Extra large"};
        for (int i=0;i<labels.length;i++) {
            RadioButton option = new RadioButton(this); option.setId(View.generateViewId()); option.setText(labels[i]); option.setTextColor(palette.text); option.setTextSize(19); option.setPadding(dp(8),dp(8),dp(8),dp(8)); option.setMinHeight(dp(56));
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent)); sizes.addView(option);
            if (i == displaySize) sizes.check(option.getId());
        }
        sizes.setOnCheckedChangeListener((group,id)-> {
            for (int i=0;i<group.getChildCount();i++) if (group.getChildAt(i).getId() == id && i != displaySize) {
                getPreferences(MODE_PRIVATE).edit().putInt("display_size",i).apply(); recreate(); break;
            }
        });
        body.addView(sizes,params(-1,-2,12));
        body.addView(text("Album display",21,palette.text),params(-1,-2,28));
        body.addView(text("Hide the cover with a plain artist, album and song label, or show it in grayscale.",16,palette.muted),params(-1,-2,8));
        body.addView(appearanceSwitch("Grayscale album art","grayscale_art",grayscaleArt),params(-1,-2,8));
        body.addView(appearanceSwitch("Text-only album block","text_only_art",textOnlyArt),params(-1,-2,8));
        body.addView(appearanceSwitch("Compact album label + big clock","compact_clock",compactClock),params(-1,-2,8));
        body.addView(text("Compact mode puts a short text label above the current time, using your phone’s 12/24-hour format. Text labels take priority over grayscale. These options also hide the cover in this app’s notification; your music app manages its own notification.",16,palette.muted),params(-1,-2,8));
        body.addView(text("Theme",21,palette.text),params(-1,-2,28));
        RadioGroup themes=new RadioGroup(this);
        int chosenTheme=getSharedPreferences("appearance",MODE_PRIVATE).getInt("theme",0);
        for (int i=0;i<AppPalette.THEMES.length;i++) {
            RadioButton option=new RadioButton(this); option.setId(View.generateViewId()); option.setText(AppPalette.THEMES[i].name);
            option.setTextColor(palette.text); option.setTextSize(18); option.setPadding(dp(8),dp(6),dp(8),dp(6)); option.setMinHeight(dp(52));
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent)); themes.addView(option);
            if (i==chosenTheme) themes.check(option.getId());
        }
        themes.setOnCheckedChangeListener((group,id)-> {
            for (int i=0;i<group.getChildCount();i++) if (group.getChildAt(i).getId()==id && i!=chosenTheme) {
                getSharedPreferences("appearance",MODE_PRIVATE).edit().putInt("theme",i).apply(); recreate(); break;
            }
        });
        body.addView(themes,params(-1,-2,8));
        body.addView(text("Updates",21,palette.text),params(-1,-2,32));
        String version = "";
        try { version = getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch (PackageManager.NameNotFoundException ignored) { }
        body.addView(text("Installed version " + version,16,palette.muted),params(-1,-2,8));
        body.addView(text("Download updates from GitHub, then approve installation in Android. Music controls work offline.",16,palette.muted),params(-1,-2,12));
        Button check = button("Check for updates",palette.onAccent,palette.accent); body.addView(check,params(-1,60,20));
        TextView updateStatus = text("",15,palette.muted); updateStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(updateStatus,params(-1,-2,12));
        Button install = button("Install downloaded update",palette.text,palette.panel); body.addView(install,params(-1,60,12));
        updater = new AppUpdater(this,check,install,updateStatus);
        body.addView(text("Media access",21,palette.text),params(-1,-2,32));
        Button media = button("Manage media access",palette.text,palette.panel); media.setOnClickListener(v -> explainAccess()); body.addView(media,params(-1,56,12));
        Button alerts = button("Manage notification controls",palette.text,palette.panel); alerts.setOnClickListener(v -> enableNotifications()); body.addView(alerts,params(-1,56,12));
        return page;
    }
    private void showTab(boolean settings) {
        settingsVisible = settings; playerPage.setVisibility(settings ? View.GONE : View.VISIBLE); settingsPage.setVisibility(settings ? View.VISIBLE : View.GONE);
        playerTab.setSelected(!settings); settingsTab.setSelected(settings);
        styleTab(playerTab,!settings); styleTab(settingsTab,settings);
        if (!settings) controlsDock.showTouchHints();
    }
    @Override protected void onSaveInstanceState(Bundle state) { state.putBoolean("settings_visible",settingsVisible); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { if (updater != null) updater.close(); super.onDestroy(); }
    @Override protected void onResume() { super.onResume(); hub.observe(this); main.post(tick); if (!settingsVisible) controlsDock.showTouchHints(); }
    @Override protected void onPause() { main.removeCallbacks(tick); hub.unobserve(this); super.onPause(); }
    @Override public void changed() {
        boolean available=hub.available();
        title.setText(available ? hub.title() : ""); artist.setText(available ? hub.artist() : "");
        metadataPanel.setVisibility(available ? View.VISIBLE : View.INVISIBLE);
        title.setVisibility(compactClock ? View.GONE : View.VISIBLE); artist.setVisibility(compactClock ? View.GONE : View.VISIBLE);
        source.setText((hub.source()==null ? "Choose app" : hub.sourceLabel()) + "  ▾");
        PlaybackState playbackState=hub.state();
        int playback=playbackState==null ? PlaybackState.STATE_NONE : playbackState.getState();
        artwork.showContent(hub.art(),playback==PlaybackState.STATE_BUFFERING || playback==PlaybackState.STATE_CONNECTING,available,hub.artist(),hub.album(),hub.title());
        boolean playing = hub.playing();
        toggle.setSymbol(playing ? TransportButton.Symbol.PAUSE : TransportButton.Symbol.PLAY); toggle.setContentDescription(playing ? "Pause" : "Play"); toggle.setTooltipText(playing ? "Pause" : "Play");
        enable(previous,hub.canPrevious()); enable(toggle,hub.canToggle()); enable(next,hub.supports(PlaybackState.ACTION_SKIP_TO_NEXT));
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
        if (!hub.hasAccess()) { explainAccess(); return; }
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
    private TextView metadataLine(String value,int size,int color) {
        TwoPassTextView line=new TwoPassTextView(this); line.setTextSize(size); line.setTextColor(color); line.setText(value); return line;
    }
    private LinearLayout.LayoutParams lineParams(TextView line,int top) {
        Paint.FontMetrics font=line.getPaint().getFontMetrics();
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,(int)Math.ceil(font.bottom-font.top)+dp(8)); lp.topMargin=dp(top); return lp;
    }
    private void styleTab(Button tab,boolean selected) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(selected ? palette.accent : palette.panel); shape.setCornerRadius(dp(24));
        tab.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(palette.dark ? Color.argb(40,255,255,255) : Color.argb(25,0,0,0)),shape,null));
        tab.setTextColor(selected ? palette.onAccent : palette.muted);
    }
    private void enable(Button b,boolean enabled) { b.setEnabled(enabled); b.setAlpha(enabled ? 1f : .35f); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView text(String value,int size,int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setLineSpacing(dp(3),1f); return v; }
    private Button button(String value,int color,int background) {
        Button b = new Button(this); b.setText(value); b.setTextColor(color); b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setPadding(dp(8),dp(8),dp(8),dp(8));
        GradientDrawable shape = new GradientDrawable(); shape.setColor(background); shape.setCornerRadius(dp(24));
        android.content.res.ColorStateList ripple = android.content.res.ColorStateList.valueOf(palette.dark ? Color.argb(45,255,255,255) : Color.argb(25,0,0,0));
        b.setBackground(new android.graphics.drawable.RippleDrawable(ripple,shape,null));
        b.setMinHeight(dp(48)); return b;
    }
    private LinearLayout.LayoutParams params(int width,int height,int top) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width < 0 ? width : dp(width),height < 0 ? height : dp(height)); lp.topMargin=dp(top); return lp; }
}

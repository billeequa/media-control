package com.jack.bigphoneplayer;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaDescription;
import android.media.session.*;
import android.os.*;
import android.provider.Settings;
import java.util.*;

final class MediaHub {
    static final String PLAY = "com.jack.bigphoneplayer.PLAY";
    static final String PREVIOUS = "com.jack.bigphoneplayer.PREVIOUS";
    // Keep old notifications from the previous APK functional during an update.
    private static final String LEGACY_RESTART = "com.jack.bigphoneplayer.RESTART";
    static final String NEXT = "com.jack.bigphoneplayer.NEXT";
    private static final int NOTICE = 42;
    private static MediaHub instance;
    static synchronized MediaHub get(Context context) {
        if (instance == null) instance = new MediaHub(context.getApplicationContext());
        return instance;
    }
    interface Observer { void changed(); }
    private final Context context;
    private final MediaSessionManager manager;
    private final NotificationManager notifications;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Observer> observers = new HashSet<>();
    private final ComponentName listener;
    private final android.content.SharedPreferences preferences;
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::setSessions;
    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { emit(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { emit(); }
        @Override public void onQueueChanged(List<MediaSession.QueueItem> queue) { emit(); }
        @Override public void onSessionDestroyed() { detach(); refresh(); }
    };
    private List<MediaController> sessions = new ArrayList<>();
    private MediaController controller;
    private String preferred;
    private boolean connected;
    private final PreviousPress previousPress = new PreviousPress();
    private final QueuePreload queuePreload = new QueuePreload();
    private final Runnable notificationTick = new Runnable() {
        @Override public void run() {
            if (!connected || controller == null) return;
            postNotification();
            if (playing()) main.postDelayed(this, 5000);
        }
    };
    private MediaHub(Context context) {
        this.context = context;
        manager = context.getSystemService(MediaSessionManager.class);
        notifications = context.getSystemService(NotificationManager.class);
        listener = new ComponentName(context, MediaListener.class);
        preferences = context.getSharedPreferences("controller", Context.MODE_PRIVATE);
        preferred = preferences.getString("source", null);
        NotificationChannel channel = new NotificationChannel("controls", "Playback controls", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Buttons for your selected music app");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notifications.createNotificationChannel(channel);
    }
    boolean hasAccess() {
        String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        for (String entry : enabled.split(":")) {
            if (listener.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }
    void connect() {
        if (!connected) {
            try {
                manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, main);
                connected = true;
            } catch (SecurityException exception) { disconnect(); return; }
        }
        refresh();
    }
    void disconnect() {
        if (connected) manager.removeOnActiveSessionsChangedListener(sessionsChanged);
        connected = false;
        detach(); sessions = new ArrayList<>();
        notifications.cancel(NOTICE);
        main.removeCallbacks(notificationTick);
        for (Observer observer : new ArrayList<>(observers)) observer.changed();
    }
    void observe(Observer observer) { observers.add(observer); refresh(); }
    void unobserve(Observer observer) { observers.remove(observer); }
    void refresh() {
        if (!hasAccess()) { disconnect(); return; }
        try { setSessions(manager.getActiveSessions(listener)); }
        catch (SecurityException exception) { disconnect(); }
    }
    List<MediaController> sources() { return new ArrayList<>(sessions); }
    String label(String packageName) {
        if (packageName == null) return "Choose a music app";
        try { return context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(packageName, 0)).toString(); }
        catch (PackageManager.NameNotFoundException exception) { return packageName; }
    }
    String source() { return preferred; }
    String sourceLabel() { return label(preferred); }
    void select(String packageName) {
        previousPress.reset();
        preferred = packageName;
        preferences.edit().putString("source", preferred).apply();
        refresh();
    }
    private void setSessions(List<MediaController> incoming) {
        sessions = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        MediaController selected = null;
        // Once chosen, stay on this source even if a video/browser starts playing.
        if (preferred != null) {
            for (MediaController item : sessions) if (preferred.equals(item.getPackageName())) {
                if (selected == null || playing(item)) selected = item;
            }
        } else if (!sessions.isEmpty()) {
            selected = sessions.get(0);
            for (MediaController item : sessions) if (playing(item)) { selected = item; break; }
            preferred = selected.getPackageName();
            preferences.edit().putString("source", preferred).apply();
        }
        if (controller == null || selected == null || !controller.getSessionToken().equals(selected.getSessionToken())) {
            detach(); controller = selected;
            if (controller != null) controller.registerCallback(callback, main);
        }
        emit();
    }
    private static boolean playing(MediaController item) {
        PlaybackState state = item.getPlaybackState();
        return state != null && state.getState() == PlaybackState.STATE_PLAYING;
    }
    private void detach() {
        previousPress.reset();
        queuePreload.clear();
        if (controller != null) controller.unregisterCallback(callback);
        controller = null;
    }
    boolean available() { return controller != null; }
    PlaybackState state() { return controller == null ? null : controller.getPlaybackState(); }
    MediaMetadata metadata() { return controller == null ? null : controller.getMetadata(); }
    boolean playing() { return controller != null && playing(controller); }
    boolean supports(long action) { PlaybackState s = state(); return s != null && (s.getActions() & action) != 0; }
    boolean canToggle() { return supports(PlaybackState.ACTION_PLAY_PAUSE) || supports(playing() ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY); }
    boolean canPrevious() { return supports(PlaybackState.ACTION_SEEK_TO) || supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS); }
    String title() {
        MediaMetadata m = metadata();
        if (m == null) return available() ? "Track information unavailable" : "Start your music";
        CharSequence title = m.getDescription().getTitle();
        if (title == null || title.length() == 0) {
            MediaDescription queued = preloaded();
            title = queued == null ? null : queued.getTitle();
        }
        return title == null || title.length() == 0 ? "Untitled track" : title.toString();
    }
    String artist() {
        MediaMetadata m = metadata();
        if (m == null) return "";
        String artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST);
        if (artist == null) { CharSequence subtitle = m.getDescription().getSubtitle(); artist = subtitle == null ? "" : subtitle.toString(); }
        if (artist == null || artist.isEmpty()) {
            MediaDescription queued = preloaded();
            CharSequence subtitle = queued == null ? null : queued.getSubtitle();
            artist = subtitle == null ? "" : subtitle.toString();
        }
        return artist;
    }
    String album() {
        MediaMetadata m=metadata();
        String value=m==null ? null : m.getString(MediaMetadata.METADATA_KEY_ALBUM);
        return value==null ? "" : value;
    }
    Bitmap art() {
        MediaMetadata m = metadata();
        if (m == null) return null;
        Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        MediaDescription queued = preloaded();
        if (b == null && queued != null) b = queued.getIconBitmap();
        return b;
    }
    private MediaDescription preloaded() {
        PlaybackState s = state();
        MediaMetadata m = metadata();
        if (s == null || m == null) return null;
        return queuePreload.match(s.getActiveQueueItemId(), m.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
    }
    boolean preloadedArtReady() {
        MediaDescription queued = preloaded();
        return queued != null && queued.getIconBitmap() != null;
    }
    long duration() { MediaMetadata m = metadata(); return m == null || !m.containsKey(MediaMetadata.METADATA_KEY_DURATION) ? -1 : m.getLong(MediaMetadata.METADATA_KEY_DURATION); }
    long position() {
        PlaybackState s = state();
        if (s == null) return -1;
        return PlaybackMath.position(s.getPosition(), s.getLastPositionUpdateTime(), SystemClock.elapsedRealtime(), s.getPlaybackSpeed(), playing(), duration());
    }
    void command(String action, String expectedSource) {
        refresh();
        if (controller == null || expectedSource == null || !expectedSource.equals(controller.getPackageName())) return;
        MediaController.TransportControls transport = controller.getTransportControls();
        try {
            if (PLAY.equals(action) && canToggle()) {
                if (supports(playing() ? PlaybackState.ACTION_PAUSE : PlaybackState.ACTION_PLAY)) {
                    if (playing()) transport.pause(); else transport.play();
                } else {
                    // Some apps advertise only the media-button toggle action.
                    controller.dispatchMediaButtonEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                    controller.dispatchMediaButtonEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                }
            } else if (PREVIOUS.equals(action)) {
                MediaMetadata metadata = metadata();
                String id = metadata == null ? "" : metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
                String track = (id == null ? "" : id) + "|" + title() + "|" + artist();
                PreviousPress.Action result = previousPress.press(SystemClock.elapsedRealtime(),controller.getSessionToken(),track,
                    supports(PlaybackState.ACTION_SEEK_TO),supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS));
                if (result == PreviousPress.Action.RESTART) transport.seekTo(0);
                else if (result == PreviousPress.Action.PREVIOUS) transport.skipToPrevious();
            } else if (LEGACY_RESTART.equals(action) && supports(PlaybackState.ACTION_SEEK_TO)) transport.seekTo(0);
            else if (NEXT.equals(action) && supports(PlaybackState.ACTION_SKIP_TO_NEXT)) {
                previousPress.reset(); transport.skipToNext();
            }
        } catch (RuntimeException exception) { refresh(); }
    }
    private void emit() {
        PlaybackState currentState = state();
        if (controller != null && currentState != null)
            queuePreload.update(controller.getQueue(), currentState.getActiveQueueItemId());
        for (Observer observer : new ArrayList<>(observers)) observer.changed();
        main.removeCallbacks(notificationTick);
        if (controller == null) notifications.cancel(NOTICE);
        else { postNotification(); if (connected && playing()) main.postDelayed(notificationTick, 5000); }
    }
    private PendingIntent action(String action, int code) {
        Intent intent = new Intent(context, ControlReceiver.class).setAction(action).setData(android.net.Uri.fromParts("media-control", preferred, null)).putExtra("source", preferred);
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private void postNotification() {
        if (controller == null || !notifications.areNotificationsEnabled()) return;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String timing = PlaybackMath.time(position()) + " / " + PlaybackMath.time(duration());
        Notification.Builder builder = new Notification.Builder(context, "controls")
            .setSmallIcon(R.drawable.ic_player).setContentTitle(title())
            .setContentText((artist().isEmpty() ? sourceLabel() : artist()) + "  ·  " + timing)
            .setSubText("Controls for " + sourceLabel()).setContentIntent(open)
            .setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true)
            .setShowWhen(false).setOngoing(playing()).setCategory(Notification.CATEGORY_TRANSPORT);
        android.content.SharedPreferences appearance=context.getSharedPreferences("appearance",Context.MODE_PRIVATE);
        boolean hideCover=appearance.getBoolean("text_only_art",false) || appearance.getBoolean("compact_clock",false);
        Bitmap artwork = hideCover ? null : art(); if (artwork != null) builder.setLargeIcon(artwork);
        if (canPrevious()) builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous, "Previous", action(PREVIOUS, 1)).build());
        if (canToggle()) builder.addAction(new Notification.Action.Builder(playing() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, playing() ? "Pause" : "Play", action(PLAY, 2)).build());
        if (supports(PlaybackState.ACTION_SKIP_TO_NEXT)) builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", action(NEXT, 3)).build());
        if (duration() > 0 && position() >= 0) builder.setProgress(1000, (int)(1000.0 * position() / duration()), false);
        try { notifications.notify(NOTICE, builder.build()); } catch (SecurityException ignored) { }
    }
}

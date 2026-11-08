# Big Phone Player

A simple Android remote for the music app already playing on your phone.

**Start a song in your usual app, then open Big Phone Player.** The controller
shows the title, artist, progress and optional artwork, with three large buttons:
restart the current song, play/pause, and next. It remembers your selected source
instead of switching to a browser or another app when that app makes sound.

## Setup

1. Install the debug APK on an Android 8.0+ phone.
2. Open Big Phone Player and tap **Enable media access**. Read the explanation,
   enable notification access for Big Phone Player in Android settings, then return.
3. On Android 13+, allow notifications if you want notification controls.
4. Start music in your source app and return. Use the source button to choose
   between active apps.

The app adds a notification with supported transport buttons. You can use it
from the notification shade and, where your phone permits, the lock screen.
Android controls the notification layout, which actions appear when collapsed,
and lock-screen visibility. Expand the notification if buttons are hidden.

## Compatibility

Designed for apps exposing standard Android media sessions, including the use
case of foobar2000, Spotify, Pandora and Bandcamp. **Those named apps have not
yet been verified on a device.** An app without an active media session will
not appear. Available commands and metadata depend on the source:

- Restart sends a seek to `0`, never a previous-track command. It is disabled
  when the source does not advertise seeking.
- Next and play/pause are disabled when not supported. Subscription restrictions,
  ads and source-app policies still apply.
- Unknown elapsed/total time is shown as `—`; live streams may have no duration.
- Closing or force-stopping the source can remove its session. Start it there again.
- Progress is read-only in this first version.

The app does not play audio, create a replacement media session, or take over
audio focus. Your music app retains its own playback, notification and system
media controls.

## Privacy

Android's notification-access permission is broad and can expose notifications.
This app uses it only to obtain active media sessions; it does not inspect,
save or forward notification contents. It has no internet permission, accounts,
analytics, streaming credentials or backend. Only the chosen source package is
stored locally. The control notification displays track information publicly
on the lock screen when permitted by your notification settings.

## Build

Use JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. Open the repository
in Android Studio, or run:

```sh
./scripts/test.sh
./gradlew assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions runs the same checks on pushes and pull requests, then uploads
an installable debug APK as the **big-phone-player-debug** artifact. Open a
successful run on the [Actions page](https://github.com/billeequa/media-control/actions)
and download that artifact while signed in. This is a debug build, not a store release;
different build machines can use different debug signing keys.

For an SDK-only build without Gradle plugin downloads:

```sh
ANDROID_HOME=/path/to/android-sdk python3 scripts/build_apk.py
```

This fallback builds and verifies an APK, but does not run Android Lint.
See [testing notes](docs/TESTING.md) for verification status and the device checklist.

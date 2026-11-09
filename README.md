# Big Phone Player

A simple Android remote for the music app already playing on your phone.

**Start a song in your usual app, then open Big Phone Player.** The controller
shows a centered album cover at 80% of the screen width, title, artist and
progress, with three large rounded, symbol-only buttons:
previous, play/pause, and next. One Previous press restarts the track; a second
press within three seconds goes to the previous track when supported. It remembers your selected source
instead of switching to a browser or another app when that app makes sound.

The **Settings** tab has **Compact**, **Large**, and **Extra large** player sizes.
Your choice is saved. Theme options are **Classic Green**, **AMOLED**, **Midnight**,
**Paper**, **Amber**, **Rose**, **Ocean**, **Violet**, **Monochrome**, and **Cloud**, and are also saved. It also has a **Check for updates** button that downloads a
newer APK from this repository's latest stable release and opens Android's installer.
Android asks you to approve installation.

The player screen does not scroll. The transport buttons and metadata stay
anchored above the tabs; the cover shrinks to fit a short viewport. The elapsed/total
time stays visible above the raised playback row. Title and artist stay on one line: long
values loop continuously twice; the next copy slides smoothly to the normal
starting position, slows to rest, and stays ellipsized. Equal metadata does not
restart scrolling, and disabled system animations show a static line. Buffering replaces
the cover with centered **Loading…** inside the same square. Each transport
button accepts taps up to one button height above and below its visible bounds;
horizontal bounds stay unchanged. Targets may cover the time bar but stop before
the Player and Settings tabs. The playback row sits higher, and artist, time,
source-picker and tab labels are larger. A brief fading outline shows these touch areas
when opening the player. The launcher icon is a green rounded play triangle on
black.

In **Settings → Album display**, choose independently:

- **Grayscale album art**: monochrome covers.
- **Text-only album block**: artist, album and song on a plain theme-colored block.
- **Compact album label + big clock**: a short text block above a large current-time
  clock in your phone’s 12/24-hour format. Track progress and elapsed/total time remain.

Text mode overrides grayscale; compact mode always uses a text label. These choices
are saved. All three options omit the cover from Big Phone Player’s own notification;
your music app controls its own notification. Without an active selected player,
the box says **No player found**, with no stale outside metadata or scrolling.

## Install or update

Download [the latest APK](https://github.com/billeequa/media-control/releases/latest/download/big-phone-player.apk),
open it on Android and approve installation. Existing v0.2.1+ installs can instead
use **Settings → Check for updates**. The signing identity is unchanged, so an
ordinary update preserves settings; do not uninstall first. See the
[changelog](CHANGELOG.md) for changes and [verification](docs/TESTING.md) for what
has actually been tested.

## Setup

1. Install the debug APK on an Android 8.0+ phone.
2. Open Big Phone Player and tap **Choose app**, or go to **Settings → Manage media
   access**. Read the explanation, enable notification access for Big Phone Player
   in Android settings, then return.
3. For notification controls, use **Settings → Manage notification controls** and
   allow notifications (Android 13+ may ask for permission).
4. If Android blocks notification access as a restricted setting after APK
   installation, open Settings → Apps → Big Phone Player → ⋮ → Allow restricted
   settings, authenticate, then return to enable media access.
5. Start music in your source app and return. Use the source button to choose
   between active apps.

The app adds a notification with supported transport buttons. You can use it
from the notification shade and, where your phone permits, the lock screen.
Android controls the notification layout, which actions appear when collapsed,
and lock-screen visibility. Expand the notification if buttons are hidden.

## Compatibility

Designed for apps exposing standard Android media sessions, including the use
case of foobar2000, Spotify, Pandora and Bandcamp. **The user confirmed the app works with foobar2000 on their phone. The other
named players have not yet been verified.** An app without an active media session will
not appear. Available commands and metadata depend on the source:

- Previous sends a seek to `0` immediately, then sends previous-track on a second
  press within three seconds for the same session/track. If seeking is unavailable,
  it sends previous-track directly when supported; if only seeking is supported,
  repeated presses restart. If neither is supported, the button is disabled.
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
save or forward notification contents. Internet access is used only when you
request a GitHub update check/download. There are no accounts, analytics,
streaming credentials or backend. The source package and display size are stored
locally, and a downloaded update is held in the app's private cache. The control notification displays track information publicly
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
different build machines can use different debug signing keys unless the stable
signing secret is configured. For in-place updates, use the APK from **Releases**
and keep its signing key fixed. The updater requires a **public repository** and
an APK attached to a published release. See [release setup](docs/RELEASING.md)
for the one-time signing secret and **Publish APK** workflow.

For an SDK-only build without Gradle plugin downloads:

```sh
ANDROID_HOME=/path/to/android-sdk python3 scripts/build_apk.py
```

This fallback builds and verifies an APK, but does not run Android Lint.
See [testing notes](docs/TESTING.md) for verification status and the device checklist.

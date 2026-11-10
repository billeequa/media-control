# Big Phone Player

Native Android controller for another app's already-started playback. Keep the
screen simple: large symbol-only previous, play/pause and next buttons, prominent
track title and elapsed/total time. Center a square album cover at up to 80% of the
window width, shrinking it when vertical space is limited. Compact clock mode
uses a short text-only album label above a large native TextClock instead. Keep the source picker beside the app heading, progress directly
below the artist, and normal playback-state labels/footer copy hidden. No catalog, streaming, login,
audio playback engine or proxy media session. Internet access is only for
user-initiated GitHub update checks and APK downloads.

Use the platform MediaSessionManager and an explicitly enabled notification
listener. Commands must respect the source's advertised capabilities. Previous immediately
seeks to zero on one press, then skips to the previous track on a second press
within three seconds of the same track/session. If seeking is unsupported, Previous
sends the previous-track command directly when supported. Respect capabilities
and reset the double-press state when changing source/session or tapping Next. Keep the selected source pinned until
the user changes it, and do not redirect stale notification actions to another
source. Notification access is broad: explain it before opening settings, and
do not inspect or store notification contents.

Build with JDK 17, Android SDK 35 and the checked-in Gradle wrapper. Run
`./scripts/test.sh` and `./gradlew assembleDebug lintDebug` after code changes.
The SDK-only fallback is `python3 scripts/build_apk.py`; it compiles, dexes,
aligns, signs and verifies the same sources without downloading Gradle plugins,
but does not replace Android Lint. Never commit signing keys or build output.

Maintain README.md and docs/TESTING.md. Distinguish compilation, automated
logic checks, emulator tests and real player/device checks. Never claim a named
music app is verified until actually tested. Notification and lock-screen
layout, visibility and available actions are controlled by Android and the
device's settings. Do not create a fake local media session to take over them.

Settings contains persisted Compact/Large/Extra large presets and a manual
GitHub release updater. Keep HTTPS origin checks, package identity, higher
version code and matching signing certificate validation before installation.
Never embed GitHub credentials. Keep the update provider private and limited
to its one APK; grant read access only to the Android installer intent. Updates
must use a stable signing key configured as an Actions secret, not a key in Git.

This repository is the source of truth. Publish authorized changes here and
check the GitHub Actions result. The build workflow produces an installable
debug APK artifact. The Publish APK workflow (manual or pushed version tag) creates stable-key GitHub
releases for the updater after the signing secret is configured. Bump both
versionCode and versionName for a new release; never replace an existing tag.

The persistent signing key is configured. Never generate a replacement key for
an ordinary release. Set UPDATE_KEYSTORE_PATH explicitly for Gradle and run
scripts/verify_signing.py before publishing; it pins the public certificate
fingerprint. A prior v0.2.0 publication used a runner key, so the stable update
series starts with v0.2.1. Keep old tags intact. See docs/RELEASING.md.

The main player page has no ScrollView. Player transport controls and fixed-height
metadata/clock are anchored below the bounded artwork area. Settings may scroll. Keep the clock visible above the raised controls. Never let track text, loading, missing artwork or error/setup messages move
them vertically. Title and artist reserve fixed single-line heights; overflow
scrolls continuously through two full loops, then brings a third copy smoothly to
the default starting position and ends ellipsized, without restarting for identical metadata.
Buffering/connecting uses Loading… inside the same square cover footprint, not a
separate state label. Respect disabled system animations.

Transport hotboxes extend up to one visible button height vertically above and
below each button, with unchanged horizontal bounds and gaps. They may cover
the time/progress area, but must be clamped to the player page before the Player
and Settings tabs. The transparent overlay must let other gestures reach the
remaining content outside its targets. Briefly outline these hotboxes on
opening and fade the outline; never make the controls themselves disappear.

Appearance settings persist Classic Green, AMOLED, Midnight, Paper, Amber, Rose,
Ocean, Violet, Monochrome and Cloud themes. Append new themes without reordering
existing IDs.
Theme all screen text, surfaces, controls and system bars, including a native
light dialog theme for Paper. The launcher uses a dark navy (#111C2E) background with one smaller green
play triangle with rounded corners. Do not tie launcher color to the selected in-app theme.

No selected active session: show only “No player found” inside the album box;
clear hidden cover drawables/labels and outside track metadata. Keep setup
instructions in Settings; Choose app opens the access explanation if required.
Paused sessions still count as an available player.

Appearance preferences `text_only_art` and `compact_clock` are independent saved
booleans. Remove the legacy `grayscale_art` preference; grayscale is no longer
supported. Double-tapping the album block cycles normal -> text-only -> normal,
without resizing the block or recreating the player. A single tap does nothing.
The Settings text-only switch must reflect gesture changes. Disable cycling and
that switch while compact mode is active, retaining the prior noncompact choice.
Compact mode always uses artist/album/song labels and a local current-time clock,
retains progress and elapsed/total time, and does not move transport controls.
Hidden cover modes must clear ImageView drawables, not merely cover them.
Loading/no-player messages take priority over labels/images. Omit the cover from
this app's notification whenever text-only/compact is enabled; source-app
notifications remain its concern. Reopening via launcher or resuming a stopped
activity must show Player. Settings changes may recreate the activity while
remaining on Settings; cold starts after process death must show Player.

Keep the screen awake with the Player view’s keepScreenOn property. Android must
release this behavior when that view is hidden by Settings or the app is in the
background. Do not use a wake lock or add a permission for this.

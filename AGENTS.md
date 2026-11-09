# Big Phone Player

Native Android controller for another app's already-started playback. Keep the
screen simple: large symbol-only previous, play/pause and next buttons, prominent
track title and elapsed/total time. Center a square album cover at 80% of the
window width. Keep the source picker beside the app heading, progress directly
below the artist, and normal playback-state labels/footer copy hidden. No catalog, streaming, login,
audio playback engine or proxy media session. Internet access is only for
user-initiated GitHub update checks and APK downloads.

Use the platform MediaSessionManager and an explicitly enabled notification
listener. Commands must respect the source's advertised capabilities. Previous immediately
seeks to zero on one press, then skips to the previous track on a second press
within one second of the same track/session. If seeking is unsupported, Previous
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
debug APK artifact. The manual Publish APK workflow creates stable-key GitHub
releases for the updater after the signing secret is configured. Bump both
versionCode and versionName for a new release; never replace an existing tag.

The persistent signing key is configured. Never generate a replacement key for
an ordinary release. Set UPDATE_KEYSTORE_PATH explicitly for Gradle and run
scripts/verify_signing.py before publishing; it pins the public certificate
fingerprint. A prior v0.2.0 publication used a runner key, so the stable update
series starts with v0.2.1. Keep old tags intact. See docs/RELEASING.md.

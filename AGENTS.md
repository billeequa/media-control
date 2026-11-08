# Big Phone Player

Native Android controller for another app's already-started playback. Keep the
screen simple: large restart, play/pause and next buttons, prominent track title
and elapsed/total time. Artwork is secondary. No catalog, streaming, login,
audio playback engine, internet permission, or proxy media session.

Use the platform MediaSessionManager and an explicitly enabled notification
listener. Commands must respect the source's advertised capabilities. Restart
means seek to zero, never previous track. Keep the selected source pinned until
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

This repository is the source of truth. Publish authorized changes here and
check the GitHub Actions result. The build workflow produces an installable
debug APK artifact; publishing a signed store release is a separate task.

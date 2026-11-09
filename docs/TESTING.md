# Verification

## Reproduce the checks

Use JDK 17, Android SDK Platform 35 and Build Tools 35.0.0:

```sh
bash scripts/test.sh
bash gradlew assembleDebug lintDebug --no-daemon
```

The first command runs standalone Java logic checks without Android or test
libraries. The second compiles/packages the app and runs Android Lint.
`scripts/build_apk.py` is an SDK-only fallback; it does not replace Lint or runtime
checks, and its default debug key must not be used to publish an ordinary update.
See [release instructions](RELEASING.md) for the stable signing key.

GitHub Actions **Android build** runs two jobs:

- **build**: logic checks, Gradle build, Android Lint and (when configured) the pinned
  signing-certificate check. Outputs `big-phone-player-debug` and lint artifacts.
- **ui**: installs that build and a matching-signed instrumentation APK on a
  disposable Android 35 Pixel 4 emulator. Outputs `player-ui-results`.
  Builds without signing secrets re-sign only the disposable test copy.

To run the UI checks on an already-running disposable emulator, build/install the
app with the same keystore as the instrumentation APK, then run:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 scripts/build_ui_checks.py
bash scripts/run_ui_checks.sh
```

Set `UPDATE_KEYSTORE_PATH` if the key is elsewhere. The script has a three-minute
instrumentation timeout and treats a missing success marker as failure, since
Android can return exit code zero for an instrumentation failure. Do not run these
checks on a personal phone: they change theme preferences and activity state.

## Version 0.5.0 status and coverage

Local: **613 logic checks passed**, and Android sources, resources and the UI
instrumentation compile against API 35. Local Gradle could not download its
wrapper because of network restrictions; Actions supplies full Gradle/Lint checks.
The [final Android build](https://github.com/billeequa/media-control/actions/runs/42564722874)
passed both jobs: **613 logic checks, Gradle build/Lint, persistent signing check,
and 44 Android 35 Pixel 4 emulator checks**. The
[v0.5.0 publication](https://github.com/billeequa/media-control/actions/runs/42565087334)
also passed. The published APK was downloaded independently and its digest,
cryptographic signature, pinned certificate, package, versionCode 6 and versionName
0.5.0 were verified. Its SHA-256 is:

```
d4538f1d189ba21d0859f1e92342e8f7f1649eb71ac93dfa2daa613d87870302
```

A successful build alone is not a device test. Emulator checks still do not
establish real source-player or updater installation behavior.

Logic coverage includes:

- Playback position, speed, clamping, missing/future timestamps and time formatting.
- Update version ordering, exact release asset path and HTTPS origin restrictions.
- Previous's immediate restart, second tap at/before three seconds, timeout after
  three seconds, track/session changes, resets and advertised capabilities.
- Continuous scrolling without a position reset at a loop boundary, final easing,
  two complete loops, density independence and invalid dimensions.
- Readable text, secondary text and accent contrast in all ten themes. Saved
  theme IDs remain stable.

The Android instrumentation checks visible clock above the raised row, fixed transport coordinates across long/empty
metadata and loading/artwork, reserved single-line heights, animation completion
and identical-metadata behavior, expanded tap routing through the actual view
hierarchy, unchanged horizontal gaps, page-boundary clamping before both tabs,
Settings tap routing, fading hints and persistence/backgrounds of all ten themes.
It does not simulate a real music app's media session or updater installation.

## Real-device acceptance

The user confirmed earlier versions work with foobar2000 on their phone. This
release still needs visual/gesture acceptance on that phone; emulator success
cannot establish Spotify, Pandora, Bandcamp or OEM lock-screen behavior.

1. Update in place from v0.2.1+; approve Android's installer and confirm saved
   source, size and theme survive. Reject wrong package/signature/older APKs.
2. Start music in each desired source; check metadata, timing, pause/resume, Next,
   and Previous: first tap restarts; second within three seconds skips when
   supported. Check a second tap after three seconds and source/track changes.
3. Use long title/artist text: read two loops, then watch the next copy settle at
   its starting position without snapping. Short/empty text must stay on one line.
   Check RTL, emoji, line breaks and disabled system animations.
4. Tap high/low around each playback button. The hint must match the effective
   target, gaps must remain inactive, and Player/Settings must always receive
   their own taps. Scroll the cover outside the targets. Verify loading, errors
   and source changes do not move playback controls.
5. Check Compact/Large/Extra large, landscape, narrow screens, enlarged system
   fonts and TalkBack. Artist, clock, source and tab labels should be readable;
   all controls and metadata should remain reachable.
6. Select each theme and reopen the app. Check controls, settings, dialogs, loading
   placeholder and system bars. Launcher stays black/green; notification icon is
   a separate white silhouette.
7. Test two active sources, pinned source choice, missing capabilities, revoked
   access and a stopped source. Stale notification actions must not control a
   newly selected source. Pause at least ten seconds: elapsed time must stay still.
8. Test notification controls with the app open/backgrounded/swiped away and the
   screen locked. Android/OEM policy controls visibility and collapsed actions;
   the source's own notification/headset controls should continue to work.
9. Test updater cancellation, no network, malformed/older releases, interrupted
   downloads and rotation/backgrounding. Update access is user initiated; media
   control needs no network. Installation must remain an Android confirmation.

The app uses the platform notification listener and media-session callbacks. It
neither reads notification contents nor creates a substitute media session or
playback foreground service. The visible clock refreshes every 500 ms; the
controller notification refreshes every five seconds while playing.

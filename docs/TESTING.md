# Verification

## Automated logic checks

`./scripts/test.sh` checks elapsed-position extrapolation, paused position,
playback speed, duration clamping, missing position/update time, future timestamps,
reverse playback and elapsed/total formatting. Update checks cover numeric version
ordering, equal/older releases, malformed versions, the exact repository asset
path, HTTPS origin restrictions and malicious/lookalike URLs (84 checks total).
Previous-button checks cover immediate restart, rapid second press, timeout,
track/session changes, reset, missing capabilities and backwards timestamps.
It needs Java 17 and no test libraries.

## Build checks

The normal build is `./gradlew assembleDebug lintDebug`. The SDK-only build
compiles all Android sources against API 35, converts them with D8 at minimum
API 26, packages resources, aligns the APK and verifies its debug signature.

Initial local verification: playback-math checks and SDK-only APK build.
The Gradle build could not download its Android plugin in the restricted local
environment. GitHub Actions runs the full build and Android Lint. Its initial
SDK setup tried to install the retired `tools` package; the workflow now explicitly
requests `platform-tools` instead.

The user confirmed v0.2.1 works with foobar2000 on their phone. The new v0.3.0
layout/previous behavior and other named music apps still need device checks. Do not confuse an APK build with successful device operation.

## Device acceptance checklist

Run on Android 8 and a recent Android version, ideally including the actual phone.

1. Fresh install: explain notification access, open settings and return after
   granting it. Declining access leaves controls disabled and does not crash.
2. Start playback in foobar2000, Spotify, Pandora and Bandcamp separately. Record
   app/OS versions, session discovery, metadata, time/duration, pause/resume, restart
   and next; mark unsupported actions separately from failures.
3. Previous seeks to zero in the **current** track immediately. A second press
   within one second goes to the previous track when advertised. After a timeout,
   source/session/track change or Next, the first press restarts again. For a source
   without seek support, Previous sends previous-track directly when supported.
   Next remains subject to the source's skip rules.
4. Pause for at least 10 seconds: elapsed position stays still. Resume: elapsed
   advances. Test a long track, missing metadata and a stream without duration.
5. Start two sources and choose one. Another app beginning playback must not
   silently redirect controls. Switch sources explicitly and check a queued tap
   from an old notification cannot operate the newly chosen source.
6. Expand the controller notification. Test its supported buttons with the activity
   visible, backgrounded and after swiping the activity away. Verify the source's
   own notification and headset controls continue to work.
7. Lock the phone: test visible notification controls under the phone's lock-screen
   policy. Hide notifications in system settings and confirm the app still works.
8. Revoke notification access, stop the source, kill/restart its process, then
   resume playback. Check stale controls clear and reconnecting works.
9. Rotate the device; use a narrow screen, landscape, large font and TalkBack.
   Buttons must remain readable/reachable, with clear labels and disabled states.

## Settings and updater acceptance checklist

1. In Settings choose Compact, Large, then Extra large. Verify the title, clock and
   all three buttons resize, persist across relaunch, and fit a narrow/landscape
   screen with large system fonts. The Settings tab remains selected after resizing.
2. While the repo is private or no stable release exists, check for updates. Show
   the public-release requirement; keep music controls usable.
3. Test current/older release tags (no update), a newer release without an APK,
   no network, request limits, malformed JSON and interrupted downloads.
4. Publish a higher version with the same signing key. Download, grant install
   permission, return to Settings and install. Check saved source/size survive.
5. Reject a different package, wrong signing key, non-increasing version code,
   truncated/mismatched-checksum APK, insecure URL or redirect off GitHub.
6. Rotate/background while checking/downloading. No stale activity dialogs or
   crashes; cancelled downloads must not become installable partial APKs.
7. With an already-downloaded valid update, reopen Settings and install without
   another network request. Cancelling Android's installer leaves that button usable.

Full updater installation has not been device-tested. Public releases and the
persistent signing secret are configured; the successful installer path still needs a device.

## Implementation notes

The app uses a system-bound NotificationListenerService and active media-session
callbacks; it does not start a mediaPlayback foreground service or create a dummy
media session. Control notifications are ordinary transport notifications, so
OEM lock-screen behavior may differ. The activity refreshes progress every 500 ms
while visible; the notification refreshes every five seconds while playing.

## Signing regression

Run `ANDROID_HOME=/path/to/sdk python3 scripts/verify_signing.py` after building
with the persistent key. It verifies the APK signature and checks its certificate
against the saved key and pinned public fingerprint. A downloaded v0.2.0 APK is
a negative regression fixture: its runner certificate must be rejected. The
v0.2.1 local build is a positive fixture. Verify the published v0.2.1 bytes again
after release, independent of workflow success.

## Player layout acceptance

On the user's phone and at narrow/landscape/large-font sizes, verify the app
picker sits beside the larger heading, artwork is a centered square at 80% of
window width, and progress follows the artist immediately. Confirm the footer,
normal Playing/Paused labels and visible transport text are absent. Previous
and Next use mirrored bar-and-triangle icons; Play/Pause uses large vector
symbols inside 36 dp corners. TalkBack and long-press labels remain available.
The app launcher uses the album-sleeve/play icon, while notification icons keep
their simple white silhouette.

## Version 0.4 appearance and geometry

The new checks cover exactly two scrolling passes, stop/hold timing, density
independence, invalid dimensions, and readable text/accent contrast for all four
themes. Additional Android runtime checks should verify:

- Long/short/empty title and artist have equal reserved line heights; two complete
  passes stop at an ellipsized line. Pause/state callbacks with identical metadata
  must not restart the animation. Check RTL, line breaks, emoji and reduced motion.
- Transport button top/bottom coordinates stay identical across metadata lengths,
  loading/cover/placeholder, setup/errors and scrolling the upper content.
- Loading hides the old cover image and centers Loading… in the same square.
- Taps up to 14 dp above/below each button work; side gaps and points outside the
  vertical extension do not. The opening hint fades without changing dimensions.
- All four themes persist through relaunch, preserve size/source choices, and
  apply to settings, controls, cover placeholders, dialogs and system bars.
- The black/green rounded triangle launcher is separate from the monochrome
  notification icon.

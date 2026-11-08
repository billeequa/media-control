# Verification

## Automated logic checks

`./scripts/test.sh` checks elapsed-position extrapolation, paused position,
playback speed, duration clamping, missing position/update time, future timestamps,
reverse playback and elapsed/total formatting. It needs Java 17 and no test libraries.

## Build checks

The normal build is `./gradlew assembleDebug lintDebug`. The SDK-only build
compiles all Android sources against API 35, converts them with D8 at minimum
API 26, packages resources, aligns the APK and verifies its debug signature.

Initial local verification: playback-math checks and SDK-only APK build.
The Gradle build could not download its Android plugin in the restricted local
environment. GitHub Actions is configured to run the full build and Android Lint.

No emulator/device playback or named music-app compatibility has been verified
yet. Do not confuse an APK build with successful device operation.

## Device acceptance checklist

Run on Android 8 and a recent Android version, ideally including the actual phone.

1. Fresh install: explain notification access, open settings and return after
   granting it. Declining access leaves controls disabled and does not crash.
2. Start playback in foobar2000, Spotify, Pandora and Bandcamp separately. Record
   app/OS versions, session discovery, metadata, time/duration, pause/resume, restart
   and next; mark unsupported actions separately from failures.
3. Restart seeks to zero in the **current** track. An unseekable stream must never
   jump to a previous song. Next remains subject to the source's skip rules.
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

## Implementation notes

The app uses a system-bound NotificationListenerService and active media-session
callbacks; it does not start a mediaPlayback foreground service or create a dummy
media session. Control notifications are ordinary transport notifications, so
OEM lock-screen behavior may differ. The activity refreshes progress every 500 ms
while visible; the notification refreshes every five seconds while playing.

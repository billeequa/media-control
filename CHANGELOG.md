# Changelog

## 0.7.1

- Keep the screen awake while Player is visible, including paused playback and
  compact clock mode. Settings and background retain the normal screen timeout.


## 0.7.0

- Double-tap the album block to cycle normal → text-only → normal, preserving
  its size and saving the choice. The Settings switch stays in sync.
- Compact clock mode disables cycling and the text-only switch while retaining
  the previous noncompact display choice.
- Remove grayscale and discard its legacy saved preference.
- Use a smaller, round-cornered green play triangle on dark navy for the launcher.
- Reopening returns to Player, including when last used on Settings.


## 0.6.0

- Removed scrolling from the main player screen. Without an active selected
  player, show “No player found” inside the album box and clear stale metadata.
- Added independent, saved Grayscale album art and Text-only album block options.
  The text block shows artist, album and song on a plain theme-colored background.
- Added Compact album label + big clock: a short text block above the current
  local time in the phone’s 12/24-hour format. Track progress/time remain available.
- Hide cover images from this app’s notification when any of these options is on.
  The source music app controls its own notification.
- Kept setup/access controls in Settings; Choose app opens the access explanation
  when permission is missing. Covers shrink to fit instead of scrolling.

## 0.5.0

- Raised the anchored playback row, reducing the gap below the time display.
- Anchored the title, artist and clock above the controls so the enlarged clock
  remains visible while the artwork scrolls separately.
- Enlarged artist and elapsed/total text, the source picker, and Player/Settings labels.
- Replaced jumping title/artist resets with continuous scrolling: two full loops,
  followed by a smooth arrival at the default, ellipsized position.
- Extended Previous's second-tap window from one to three seconds. First tap still
  restarts immediately; the second requests the previous track when supported.
- Expanded transport touch targets vertically by up to a full button height on
  each side. Targets keep their original widths and cannot cover navigation tabs.
- Added Amber, Rose, Ocean, Violet, Monochrome and Cloud themes; existing saved
  theme selections keep their meaning.
- Added Android 35 emulator checks to the normal build workflow and version-tag
  publishing to the existing stable-key release workflow.

## 0.4.0

Anchored transport controls, fixed single-line metadata with finite scrolling,
Loading… inside the square cover, four saved themes, fading touch-target hints,
and the black/green play launcher icon.

## 0.3.0

Artwork-centered layout and Previous/restart controls.

## 0.2.1

Start of the persistent signing-key update series. Earlier v0.2.0 used a different
runner key and is retained only for history.

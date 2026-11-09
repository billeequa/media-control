# Changelog

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

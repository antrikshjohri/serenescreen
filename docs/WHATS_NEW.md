# What's New in Serene Screen

Version: `v5.2.4`

This document summarizes the major changes completed for Serene Screen in the current release cycle, based on the project handoff summary and repository history.

## Highlights

- Serene Screen has been fully rebranded from Olauncher.
- The settings experience has been redesigned into a much cleaner, more polished layout.
- Home screen spacing and alignment were improved, especially on larger phones.
- Customization is easier with clearer selectors for theme, alignment, gestures, text size, and home screen app count.
- Default launcher prompts now behave more intelligently depending on whether Serene Screen is already the default launcher.
- Several safe-area, status bar, and layout issues were fixed across devices.

## Full Release Notes

### New Serene Screen identity

- Rebranded the launcher from `Olauncher` to `Serene Screen`.
- Updated package naming and project references to match the new app identity.
- Cleaned up unused manifest permissions conservatively.

### Home screen improvements

- Improved portrait spacing and alignment on larger phones.
- Refined the home layout to behave better on devices such as Pixel 6 and Pixel 8 Pro.
- Hid the `Set as default launcher` prompt on the home screen when Serene Screen is already the default launcher.

### Settings redesign

- Rebuilt the settings screen into a cleaner card-based layout.
- Made portrait mode the primary polished settings experience.
- Updated landscape settings so it remains visually and functionally aligned.
- Made whole rows tappable for easier interaction.
- Replaced boolean toggles with clearer visible switches.
- Reduced reliance on basic picker-style controls in favor of more intentional selectors.

### Better customization controls

- Added a richer `Theme` selector using a bottom sheet with live preview and theme chips.
- Added a new `App alignment` selector using a bottom sheet with live preview and bottom-align toggle.
- Redesigned `Swipe left`, `Swipe right`, and `Swipe down` settings using custom bottom-sheet selectors.
- Updated `Text size` to a slider-style selector.
- Updated `Apps on home screen` to a slider-style selector.

### Better theme behavior

- Fixed live theme switching directly inside settings.
- Ensured settings text and surfaces refresh correctly after theme changes.
- Improved separation between light and dark themes so they stay visually distinct.

### Smarter default launcher flow

- Updated the launcher change flow to use the modern launcher-role flow where supported.
- Aligned the home-screen and settings-screen launcher prompts to follow the same flow.
- Changed settings behavior based on launcher state.
- If Serene Screen is not the default launcher, a primary launcher card appears at the top of settings.
- If Serene Screen is already the default launcher, that top card is hidden and a quieter `Change default launcher` link appears near the bottom instead.

### Layout, status bar, and inset fixes

- Fixed settings content getting cut off near the bottom on tall devices.
- Fixed safe-area and bottom-navigation overlap issues.
- Fixed `Show status bar` clipping issues.
- Fixed the `Show status bar` switch visually animating from off to on when the settings screen opens.

### Build and platform updates

- Updated `compileSdk` to `35`.
- Updated `targetSdkVersion` to `35`.
- Kept the project building successfully with `./gradlew :app:assembleDebug`.

## Suggested Public Summary

Serene Screen now feels more polished, more consistent, and easier to customize. This release brings the new Serene Screen identity, a major settings redesign, improved large-screen home layout behavior, smarter default-launcher prompts, and a collection of device-specific layout and status bar fixes.

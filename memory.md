# Serene Screen Memory

Use this file as the handoff summary for future chats. New chats should read this file first.

## Project

- App name: `Serene Screen`
- Type: minimalist Android launcher, originally forked/rebranded from Olauncher
- Repo path: `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen`
- Primary branch: `master`

## Current Baseline

- `compileSdk = 36`
- Target SDK: 36 (Android 16 preview / Android 15+)
- `versionCode = 76` (`v5.3`)
- project rebrand from `Olauncher` to `Serene Screen` is already done
- build has been passing with `./gradlew :app:assembleDebug`

## Important Completed Work

### Rebrand / project cleanup

- renamed package path from `app/olauncher/...` to `app/serenescreen/...`
- updated project/app naming to `Serene Screen`
- removed clearly unused manifest permissions conservatively

### Home screen

- improved portrait spacing/alignment on larger phones
- fixed behavior across devices like Pixel 6 / Pixel 8 Pro
- `Set as default launcher` CTA on Home hides when Serene is already the default launcher

### Settings screen revamp

- settings screen was heavily redesigned into a cleaner card-based layout
- portrait settings is now the main polished experience
- landscape settings was also updated enough to stay functionally aligned
- whole rows are clickable
- boolean settings now use visible switches
- major selectors were redesigned away from plain/basic pickers

### New settings selector patterns

- `Theme`: bottom sheet with one live preview and theme chips
- `App alignment`: bottom sheet with one live preview, alignment choices, and bottom-align toggle
- `Swipe left/right/down`: custom bottom-sheet selectors
- `Text size`: slider-style selector
- `Apps on home screen`: slider-style selector

### Theme behavior & Eye-friendly themes
- Introduced curated aesthetic theme options:
  - `Charcoal Grey` (Default soft dark for new users, eye-friendly, `#161719` background with `#E6E6EA` soft text)
  - `Pitch Black` (AMOLED true black `#000000` with softened white text — preserved default for existing users)
  - `Midnight Navy` (Deep serene slate dark `#0D131F`)
  - `Clean Light` (Crisp minimalist light `#FFFFFF`)
  - `Warm Paper` (Eye-friendly warm sepia / parchment `#F6F2EB`)
  - `System Default` (Dynamically follows OS day/night mode)
- Theme bottom sheet features a live preview card and a compact horizontal scrollable selector with pill chips and color swatches
### Double tap to lock & Modern Dialogs
- Restored and integrated the `Double tap to lock` setting as a modern switch row in Settings under the **Gestures** section card
- Full support for Android Accessibility service (`GLOBAL_ACTION_LOCK_SCREEN` on Android 9+ to preserve biometric/fingerprint unlock) and Device Admin fallback on older versions
- Added `onResume()` in `SettingsFragment` to automatically sync switch state when returning from Android Accessibility Settings
- Enforced `prefs.lockModeOn` check in `HomeFragment.kt` `onDoubleClick()` so double tap gestures only trigger when enabled
- Redesigned in-app dialogs (`accessibilityLayout` & `messageLayout`) into modern 28dp Material 3 cards with theme-matching background (`?attr/customTileColor`), circular icon header badge, simplified 3-point bulleted copy, and styled pill action buttons

### Status bar / insets / layout fixes

- fixed settings bottom cut-off on tall devices
- fixed safe-area / bottom-nav overlap issues
- fixed `Show status bar` clipping issues
- fixed `Show status bar` switch animating visually from off to on when settings opens

### Default launcher flow

- modern launcher-role flow is used where supported
- Home CTA and Settings CTA were aligned to use the same launcher change flow
- settings launcher CTA behavior now depends on default-launcher state:
  - if Serene is **not** default: show primary launcher card at top of settings
  - if Serene **is** default: hide top launcher card and show a quieter bottom link labeled `Change default launcher`

## Current UX Decisions

- when Serene is already the default launcher, launcher change should not be the primary CTA in settings
- theme/alignment selector choices should be obvious but should not look noisy or gimmicky
- selected chips should be clearly readable without awkward decorations
- settings should feel polished on both light and dark themes

## Key Files Touched Most Recently

- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/java/app/serenescreen/ui/SettingsFragment.kt`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/java/app/serenescreen/ui/HomeFragment.kt`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/java/app/serenescreen/MainActivity.kt`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/java/app/serenescreen/data/Prefs.kt`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/res/layout/fragment_settings.xml`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/res/layout-land/fragment_settings.xml`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/res/layout/fragment_home.xml`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/res/values/strings.xml`
- `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/app/src/main/res/drawable/settings_card_bg.xml`

## Known Non-blocking Build Warnings

- Android Gradle Plugin `8.5.0` warns that `compileSdk 35` is newer than the tested support level
- project still has Java 8 source/target warnings under newer JDKs
- there are several existing Kotlin deprecation / nullability warnings

## Workflow For Future Chats

- Ask the new chat to read `/Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/memory.md` first.
- Treat this file as the running project memory.
- Update this file whenever a meaningful UX/behavior decision changes.

## Suggested Prompt For A New Chat

`Please read /Users/ajohri/Documents/Antriksh Personal/Serene Screen/serenescreen/memory.md first and use it as the project handoff summary before making changes.`

<div align="center">

<img src="docs/images/aether-mark.svg" width="112" height="112" alt="Aether Launcher icon" />

# Aether Launcher

**A calmer, faster home for Android.**

A lightweight native Android launcher with a clean home screen, searchable app drawer, floating dock and flexible pages.

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-native-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![CI](https://github.com/faizannazir/Aether-Launcher/actions/workflows/android-ci.yml/badge.svg)

</div>

## Visual preview

<p align="center">
  <img src="docs/images/aether-overview.svg" alt="Illustrative Aether home screen and app drawer preview" width="100%" />
</p>

> **Artwork note:** This is an illustrative interface preview, not a screenshot captured from a running device. Actual device screenshots should replace or supplement it after the current build is installed and verified.

## About

Aether is an independent Android Home app. Its large folders and service cards draw on Huawei's HarmonyOS desktop, while its system shortcuts open Android's native controls.

## Features

- **Home:** Android wallpaper, a neutral gradient or solid background, optional clock, compact search and a customizable dock. A fresh layout starts with a controls card, a Tools folder when supported apps are installed, and two populated pages when enough apps are available.
- **Layout:** choose the grid, icon size and labels. Hold a drawer app to lift it directly onto Home, then release anywhere to place it automatically. Drag Home apps to reorder, into the dock, or to Remove. Pause over an app or folder to group shortcuts.
- **Edit Home:** hold empty space for a menu beside your finger. Pinch in or hold a page dot to open Home options. Add pages, Android widgets and controls, or open launcher settings. Swipe sideways to change pages.
- **Gestures:** assign swipe up, swipe down, double tap, pinch and hold to launcher actions or supported native Android controls. Swipe up on a supported Home app to choose its Android widget. A small line marks apps with widgets. This behavior can be disabled in gesture settings.
- **Drawer:** swipe up from Home, including from the dock. Browse a continuous alphabetical grid or search installed apps. Pull down from the top of the list to return Home. Drawer columns and category chips are configurable. Search stays visible while the drawer scrolls. The keyboard search action opens the first matching app.
- **Android integration:** open Wallpaper & style, the Home app chooser, navigation mode, Internet, sound, display, notifications and accessibility. Android provides the system UI and applies changes made in these controls.
- **Google and Gemini:** use their buttons in the drawer or Home search, or assign either action in Launcher settings → Gestures. Tapping Google submits the current query if present. Gemini opens the app or website with no query.

Existing installations can apply the new default pages from Edit Home → Launcher settings → Home → Apply starter layout. Android shows an in-app confirmation before the pages, widgets and dock are replaced.

## Camera capsule

The compact camera capsule surrounds the physical camera cutout and expands for incoming notifications while the phone is unlocked. Enable **Display over other apps** and **Notification access** in Launcher settings → Android → Camera capsule. The setup includes a test notification and a switch for showing notification titles. Android asks separately for permission to send that test notification.

Notification content stays in memory while it is displayed. The capsule respects Do Not Disturb and clears its content when the phone locks or the source notification is removed. Appearance settings offer wallpaper colors, Graphite, Sage and Orchid palettes.

## Performance


The launcher queries app labels on a background dispatcher. It decodes icons on demand for visible items and caches the results in a 6 MiB memory cache. Home, the drawer and search compose one screen at a time. The release build uses R8 and resource shrinking.

## Getting started

### Requirements

- Android 8.0 (API 26) or later
- JDK 21
- Android SDK / Android Studio
- Gradle wrapper included in this repository

### Build from source

```bash
git clone https://github.com/faizannazir/Aether-Launcher.git
cd Aether-Launcher

./gradlew test
./gradlew assembleDebug
```

The debug APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

To build a release APK:

```bash
./gradlew assembleRelease
```

Release signing must use the established keystore. See [Install and update](#install-and-update).

## Set Aether as the default launcher

1. Install and open Aether Launcher.
2. Open **Settings** inside Aether.
3. Select **Set as default launcher**.
4. Confirm Aether in Android's system dialog.

Android requires the user to choose the default Home app; a third-party launcher cannot silently take over this role.

## Home screen gestures and actions

| Action | Result |
| --- | --- |
| Tap an app icon | Launch the app |
| Long-press an app in the app drawer | Pin it to the current home page |
| Tap a page indicator | Switch to that home page |
| Tap **+** beside the indicators | Add a new home page |
| Swipe up from the strip below the dock | Open the app drawer |
| Long-press an empty home area | Open home edit mode |

## Install and update

The Android application ID is `com.aether.launcher`. The development version on this branch is **1.2.0** (version code **3**).

Android permits an in-place update only when all of these conditions are met:

1. The new APK uses the **same signing certificate** as the installed APK.
2. Its version code is higher than the installed version.
3. The APK is compatible with the device's Android version and architecture.

The release workflow expects these GitHub Actions secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

**Keep the original release keystore safe. Do not create a new signing key to publish an update.** If the original key is unavailable, Android rejects an APK signed with a different certificate. Uninstalling the old app first may allow installation, but removes its local app data.

Debug builds use a debug signing key and cannot update a release-signed installation (or vice versa).

## Project boundaries

A launcher can provide the Home screen, app drawer, its own settings, widgets and in-app navigation. Android and the device manufacturer retain control of privileged interfaces such as global Settings, Quick Settings, the lock screen, notification shade, recents and system gesture navigation. Aether can open supported system settings screens; it cannot replace those operating-system interfaces.

Aether uses launcher-activity package visibility rather than requesting broad `QUERY_ALL_PACKAGES` access. It does not rely on root access, hidden installation, accessibility-based device control or device-admin takeover.

## Screenshots

The overview artwork above is a design illustration, not a device capture. Real screenshots should be captured from a verified build and added under `docs/screenshots/`. Suggested captures:

- `home-screen.png` — primary home screen and dock
- `app-drawer.png` — app list and search
- `home-pages.png` — page indicators and add-page control
- `settings.png` — launcher appearance and grid settings

When adding screenshots, use a consistent device resolution, hide personal notifications and accounts, and capture the actual running app rather than mockups.

## Contributing

Bug reports and focused improvements are welcome. Please include:

- Android version and device model
- Steps to reproduce the issue
- Expected and actual behaviour
- Relevant logs or screenshots (remove personal information)

For gesture, rendering and installation changes, test on a physical device as well as running the unit tests.

## License

See [LICENSE](LICENSE) for the project license.

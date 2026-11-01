# Aether Launcher

A lightweight, native Android home-screen replacement with an original interface inspired by the visual language and spatial layout of Huawei HarmonyOS. This is an independent Android launcher, not Huawei software and not a system UI replacement.

## Current launcher features

- Android HOME activity registration and an explicit in-app default Home setup flow.
- Live clock/date, compact installed-app grid, app drawer, search and floating dock.
- Real launchable app discovery through Android package visibility queries.
- Saved home layout, folders and Android AppWidgetHost support.
- Shared launcher preferences for dark appearance, translucency and grid columns.
- Native Android system Settings shortcut and coordinated status/navigation bar appearance while Aether is visible.

## Default Home app

Open Aether → Settings → Set as default launcher. Android displays its system confirmation. A third-party launcher cannot silently take over the Home role.

## Install and update

The application ID is `com.aether.launcher`. The version code has been incremented to 2.

Android accepts an update over an existing installation only when the new APK is signed with the same signing certificate as the installed APK and has a higher version code. The release workflow therefore requires the established signing key in GitHub Actions secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

**Do not generate a new signing key for an update.** If the original signing key is unavailable, Android cannot install a differently signed APK over the existing app. Uninstalling first removes the app's local data.

Debug APKs are signed with the debug key and are not interchangeable with a release-signed installation.

## Build

Requirements: JDK 21 and Android SDK.

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

## Android system boundaries

A launcher can replace the Home screen and provide its own app drawer, launcher settings, widgets and in-app navigation. Android and the device manufacturer retain ownership of global Settings, Quick Settings, lock screen, notification shade, recents and system gesture navigation. A launcher may open supported system settings screens and style its own status/navigation bars; it cannot replace the operating system's privileged interfaces.

## Google Play Protect

This project does not use a hidden installer, root access, accessibility-based control or device-admin takeover. Package visibility is scoped to launcher activities rather than requesting `QUERY_ALL_PACKAGES`. These choices reduce unnecessary permissions, but only Google can determine or clear a Play Protect warning. A false-positive report must be submitted to Google with the exact APK and detection details.

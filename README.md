# Aether Launcher

A lightweight native Android home-screen replacement inspired by the visual language of Huawei HarmonyOS 4. It is an independent Android launcher, not a Huawei system component and does not copy proprietary assets.

## What it does

- Registers as an Android HOME activity and can be selected as the device's default launcher.
- Discovers installed launchable apps and launches them directly.
- Presents a clean Harmony-inspired home screen with a large clock, date, app grid, search entry and floating dock.
- Includes an app drawer, app search, folders, customization and Android AppWidgetHost integration.
- Uses native Jetpack Compose surfaces and restrained translucency to keep scrolling responsive.

## Make it the default launcher

1. Install and open Aether Launcher.
2. When Android asks, select Aether Launcher as the Home app.
3. If Android does not show the chooser, open Settings → Apps → Default apps → Home app and select Aether Launcher.

The app cannot silently replace the current launcher. Android controls the HOME role and requires the user to approve the change.

## Build

Requirements: JDK 21 and Android SDK.

```bash
./gradlew test
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Design direction

The interface takes inspiration from HarmonyOS 4's spacious composition, rounded icon language, large typography and translucent controls. It uses original layouts and Android system app icons rather than Huawei branding or proprietary assets.

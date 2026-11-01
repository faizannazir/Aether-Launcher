# HarmonyOS 7 interaction research and Android implementation plan

This document records the interaction references reviewed before the launcher changes in this branch. The goal is to reproduce useful interaction patterns on Android, not to imitate screenshots or claim access to Huawei's proprietary system services.

## Sources reviewed

1. [Huawei HarmonyOS 7 product overview](https://consumer.huawei.com/cn/harmonyos-7/) — spatial visual language, light-following interactions, 3D spatial cards and system-level performance positioning. This is a product overview, not an Android launcher API specification.
2. [Huawei Basic Gestures and Shortcuts](https://consumer.huawei.com/en/support/content/en-us15876908/) — pinch-in to enter home editing, swipe down from the middle of the home screen for search, edge/back and bottom navigation gestures.
3. [Huawei Manage the Home Screen](https://consumer.huawei.com/en/support/content/en-us00801793/) — icon drag and drop, automatic alignment, layout lock, folder creation and home-page reordering.
4. [Huawei Home Screen Overview](https://consumer.huawei.com/en/support/content/en-us15994486/) — horizontal desktop navigation, large folders, service widgets and shortcut bar.
5. [Huawei Use Service Widgets](https://consumer.huawei.com/en/support/content/en-us15919069/) — swipe up on supported app icons to reveal service widgets, pin widgets, stack same-sized cards, swipe through a stack and combine cards.
6. [Huawei Home Screen Widgets](https://consumer.huawei.com/en/support/content/en-us00918364/) — widget selection, multiple widget sizes, placement and moving widgets between pages.
7. [Huawei App Drawer](https://consumer.huawei.com/en/support/content/en-us00784658/) — drawer mode, swipe-up access and dragging apps from the drawer to the desktop.

The public support pages describe HarmonyOS/Huawei home-screen behaviour across supported devices and versions; they are not all explicitly labelled HarmonyOS 7. The HarmonyOS 7 product page is used for its current spatial-design direction. Device and software-version differences must be expected.

## Interaction contract

| Interaction | Expected launcher behaviour | Android feasibility |
| --- | --- | --- |
| Pinch two fingers together on desktop | Enter a real home editor, not just theme settings | Implementable in Compose |
| Swipe down from the middle of desktop | Open launcher search | Implementable in Compose |
| Swipe left/right on desktop | Switch between persistent desktop pages | Implementable in Compose |
| Long-press and drag an app | Move app; drop on app to create folder | Implementable, needs robust drag/drop state |
| Large folder | Launch apps from the expanded folder surface | Implementable |
| Swipe up on an app icon | Show app-provided quick actions/cards when available | Android shortcut/widget APIs differ; no universal Huawei service-card equivalent |
| Widget catalogue and widget placement | Browse Android AppWidget providers and place/rescale widgets | Implementable with AppWidgetHost |
| Widget stacks | Stack compatible Android widgets and swipe between them | Custom launcher feature; not provided by AppWidgetHost |
| Back, Home, Recents and bottom-edge app switching | Respect Android's navigation mode and OEM handling | A third-party launcher cannot replace privileged system navigation |
| Notification shade and Quick Settings | Leave ownership to Android/system UI | Not available to a normal launcher |

## Android-specific constraints

- The launcher can request the Android Home role using RoleManager on supported Android versions. The system owns the final selection and may show an OEM-specific confirmation screen.
- Android AppWidgetHost can host widgets exposed by installed apps. It cannot host HarmonyOS service cards or invoke Huawei-only APIs.
- Android's LauncherApps/ShortcutManager APIs expose supported launcher shortcuts, but apps do not universally provide dynamic cards or swipe-up services.
- A standard third-party launcher cannot replace the system Recents screen, notification shade, Quick Settings, lock screen or OS-level edge navigation.
- Gesture recognizers must be scoped so horizontal desktop navigation does not consume vertical app-grid scrolling, and launcher gestures must not compete with Android's bottom system gesture region.

## Changes started in this branch

- Default Home selection uses the Android Home role request flow on Android 10+ and checks the selected role after returning.
- Home desktop now recognizes horizontal swipes to switch between existing pages.
- Pinch-in on the desktop opens the home editor panel.
- The editor panel now exposes page creation, Android widget selection and launcher settings.
- Pulling down on the clock/header opens launcher search.

These are the first interaction foundations, not a complete HarmonyOS-style launcher. Drag/drop editing, editable large folders, app-specific shortcut previews, widget stacks, page reordering/deletion, visual polish and measured startup improvements remain outstanding. Physical-device validation is required, especially for gesture navigation on Pixel and OEM Android builds.

## Validation requirements

Before treating this work as complete:

1. Run the repository's Android unit tests and debug APK build.
2. Test Home role selection on Android 10+ and verify the selected package after the system dialog returns.
3. Test horizontal page swipes, vertical grid scrolling, pinch editor entry and pull-down search independently and in combination.
4. Test with Android gesture navigation enabled and three-button navigation enabled.
5. Measure cold start to first interactive home frame and time to full app-drawer icons on a physical device. Record device, Android build, sample count and median/p95; do not claim a performance gain without measurements.
6. Capture real screenshots and short screen recordings from the verified build. Do not label concept artwork as device screenshots.

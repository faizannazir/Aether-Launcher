# Aether Launcher — Implementation Validation & QA Log

## Pixel 7 validation, 18 November 2026

The device runs Android 17 at 1080 × 2400 with an overridden density of 540 dpi. The optimized APK uses the local debug certificate for device testing.

Verified on the physical device

- Migrated the existing layout to two populated Home pages and swiped between them.
- Swiped up from the dock to open the indexed drawer, pulled down at its top to return Home, and jumped directly to the M section.
- Swiped up on Photos and Clock to open their Android widget providers.
- Bound and configured the native Analog Clock widget, restored it after a cold process restart, then removed the temporary widget and test page.
- Held a drawer app to enter Home placement and released it over unmarked header space. The shortcut was added automatically and persisted through restart.
- Removed a Home shortcut by dragging it to Remove.
- Opened a menu at the Home press position and inspected its light and dark appearance.
- Opened the Pixel Navigation mode screen and Wallpaper & style.
- Saved a double-tap mapping to Android's Internet controls and verified it after a cold restart.
- Enabled overlay and notification-listener access through Android's permission screens with the user's confirmation.
- Displayed Aether's own test notification in a compact, readable camera capsule. The idle overlay frame is [432,24]–[648,112], centered at x=540, with a size of 216 × 88 pixels.

Independent review covered placement correctness, privacy, notification access and foreground/background resources. The resulting fixes defer notification display until persisted preferences load, honor notification ranking and visual suppression, and route full-page drops to another page with available capacity. Custom color palettes pair their foreground and container colors. The static contrast check covered 28 role pairs with a minimum ratio of 6.26:1.

Final verification

- `./gradlew --offline :app:testDebugUnitTest :app:assembleRelease` completed successfully with 25 tests, zero failures, and zero errors. The capsule fallback geometry test also ran on the Android 8/API 26 Robolectric runtime.
- The final drawer-scroll sample rendered 381 frames with zero janky frames. Frame-time percentiles were 6 ms at p50, 9 ms at p90, and 10 ms at p95.
- Five cold first-frame samples were 119, 96, 103, 96, and 107 ms, with a median of 103 ms. These are ADB first-frame timings from a short device sample.
- Verified finger-following horizontal pages, the lighter drawer surface, and visible empty-search feedback on the installed release.
- The user selected Aether in Android's Home chooser. Launched Calculator from the drawer and returned to Aether using the actual bottom Home gesture.
- The user enabled Android overlay and notification-listener access. These choices remain active for the requested camera capsule.

Engineering notes

- Use the camera cutout path for alignment. On this Pixel, the broader inset rectangle was centered 12 pixels away from the physical camera path.
- Android 17 presents the Internet panel as a SystemUI dialog while the launcher can remain the resumed activity. UI tests should check the visible native controls.
- Clock widgets require both Android's binding permission and the provider's clock-face configuration before the hosted widget appears.
- Camera overlays are hidden by Android while sensitive permission dialogs are visible.

The records below describe earlier implementation stages.


## 1. Executive Summary
This document provides the actual implementation and validation log for **Aether Launcher** following codebase refactoring and visual polishing.

---

## 2. Core Defect Corrections Summary

| Area | Defect Description | Solution Implemented | Status |
| :--- | :--- | :--- | :--- |
| **Clock & Header** | Static date ("Tuesday, October 24"), fixed time ("10:08"), and fake weather ("72° Sunny · AQI 18"). | Created `GlanceClockHeader` in `HomeScreen.kt` using `java.time.LocalDateTime` and `LaunchedEffect` for real-time local clock and date updates. Removed fabricated weather string. | **VERIFIED** |
| **Glass Engine** | `RenderEffect.createBlurEffect` on `graphicsLayer` was blurring foreground text, icons, and buttons inside cards. | Re-architected `GlassEngine.kt` to apply crisp glass material backgrounds with 1px specular white/dark highlights and elevation shadows without blurring child composables. | **VERIFIED** |
| **App Icons** | `SquircleIcon.kt` forced dark/grey background containers behind every app icon, distorting original artwork. | Updated `SquircleIcon.kt` to convert drawables into high-res `ImageBitmap` (192x192) rendered cleanly without dark background boxes. | **VERIFIED** |
| **Service Widget** | Non-functional widget displaying fake meetings ("Design Sync · 10:30 AM"). | Redesigned `HarmonyServiceWidget.kt` into a functional System Dashboard with an interactive Focus mode toggle and direct System Settings launcher. | **VERIFIED** |
| **Floating Dock** | Generic capsule dock with non-distinct drawer trigger. | Updated `FloatingDock.kt` with balanced horizontal spacing, real installed app icons, a dedicated grid icon for opening the App Drawer, and `navigationBarsPadding()`. | **VERIFIED** |

---

## 3. Test Suite Execution & Benchmark Matrix

| Test Suite Class | Description | Result | Environment |
| :--- | :--- | :--- | :--- |
| `AppSearchEngineTest` | Sub-5ms fuzzy prefix app search & ranking | **PASSED** (3/3) | JUnit / Gradle |
| `LayoutPersistenceTest` | Serialization/deserialization of apps, folders, and widgets | **PASSED** (1/1) | Robolectric AVD |
| `LauncherViewModelTest` | Screen state, folder renaming, and drag-drop auto-folder logic | **PASSED** (3/3) | Robolectric AVD |
| `./gradlew test` | Complete unit test suite execution | **PASSED** | Gradle 8.8 / JDK 21 |
| `./gradlew assembleDebug` | Android debug APK compilation | **PASSED** | APK built in `app/build/outputs/apk/debug/` |

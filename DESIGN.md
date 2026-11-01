# Aether Launcher — HarmonyOS 4.0 / NEXT Design Specification

## 1. Executive Summary & Design Principles
Aether Launcher is a premium, lightweight native Android launcher optimized for Google Pixel flagships running Android 14+. It merges:
- **Huawei HarmonyOS 4.0 / NEXT:** Spatial depth, 2x2 System Service Widgets, 2x2 Smart Expanded Folders, 22% squircle geometry, physical glass materials, and specular edge highlights.
- **Google Pixel / Material You:** Wallpaper-first composition, readable geometry, dynamic color extraction, and zero-latency usability.
- **Real Device Telemetry:** Live updating local clock and date, real application discovery via Android PackageManager, and functional system shortcuts.

---

## 2. Verified Implementation & Architecture

| Component | Class Location | Key Architectural & Aesthetic Features |
| :--- | :--- | :--- |
| **Glance Clock Header** | `HomeScreen.kt` | Live device clock and date updating via `java.time.LocalDateTime` and `LaunchedEffect` in a lifecycle-aware manner. No hardcoded or fake weather data. |
| **Glass Material Engine** | `GlassEngine.kt` | Refactored `glassSurface` providing crisp translucent glass fills with 1px specular white/dark highlights and elevation shadows. Eliminates graphics layer blurs that previously distorted text and icons. |
| **Authentic Icon Pipeline** | `SquircleIcon.kt` | Converts `PackageManager` drawables to high-resolution `ImageBitmap` (192x192) rendered cleanly without artificial dark background boxes. Preserves original app artwork and accessibility labels. |
| **Service Widget** | `HarmonyServiceWidget.kt` | Functional System Dashboard featuring interactive Focus mode toggle and direct System Settings launcher. No fake meetings or appointments. |
| **Floating Dock** | `FloatingDock.kt` | Seats up to 4 favorite installed apps alongside a dedicated App Drawer button featuring a distinct grid icon and navigation bar inset clearance (`navigationBarsPadding()`). |

---

## 3. HarmonyOS Service Widgets & Smart 2x2 Folders

### A. 2x2 System Service Widget
- **Geometry:** 2x2 grid tile (`152dp x 152dp`), `26dp` continuous squircle corners.
- **Surface:** Level 2 Glass (`rgba(255, 255, 255, 0.35)` light / `rgba(30, 41, 59, 0.25)` dark) with 1px specular highlight.
- **Interiors:** Real title (*System Dashboard*), subtitle (*Quick Controls & Settings*), interactive Focus mode toggle, and direct System Settings launcher.

### B. 2x2 Smart Expanded Folder
- **Geometry:** 2x2 grid tile (`152dp x 152dp`), `26dp` continuous squircle corners.
- **Direct App Launching:** Houses up to 4 direct-launch app icons in a 2x2 inner grid (`38dp x 38dp`). Tapping an icon launches the app directly; tapping the folder header opens the expanded folder modal.

---

## 4. Color Palette & Semantic Tokens

### Base & Accent Palette
- **Obsidian Slate (Primary Text/Icons):** `#191C1E` (Light) / `#E1E2EB` (Dark).
- **Electric Cyan (Primary Spatial Accent):** `#00FBFF`.
- **Pixel Frost Blue (Secondary Accent):** `#8AB4F8`.
- **Radiant Iris / Orchid (Tertiary Accent):** `#C58AF9` / `#EC4899`.

---

## 5. Glass Material Engine & Fallbacks

- **Level 1 (Subtle):** `rgba(255, 255, 255, 0.25)` light / `rgba(30, 41, 59, 0.15)` dark — Used for At-a-Glance chips and micro controls.
- **Level 2 (Balanced):** `rgba(255, 255, 255, 0.35)` light / `rgba(30, 41, 59, 0.25)` dark with 1px specular stroke — Used for 2x2 Service Widgets, 2x2 Smart Folders, Floating Dock, and Search Field.
- **Level 3 (Strong / Overlay):** `rgba(255, 255, 255, 0.50)` light / `rgba(15, 23, 42, 0.40)` dark — Used for App Drawer, Folder Modals, and Settings Sheets.
- **Fallback Surface:** When blur is disabled in settings, engine substitutes crisp solid/semi-opaque surfaces (`rgba(248, 250, 252, 0.97)` light / `rgba(15, 23, 42, 0.97)` dark) while preserving specular borders.

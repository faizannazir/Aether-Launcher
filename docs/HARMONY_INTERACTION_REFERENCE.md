# HarmonyOS-inspired interaction reference

This document records the interaction behaviours Aether aims to reproduce on standard Android. It is based on public Huawei support documentation; it is not a claim that Aether can run Huawei's proprietary HarmonyOS framework.

## Primary references

- [Huawei — Manage the Home Screen](https://consumer.huawei.com/en/support/content/en-us00801793/): long-press and drag app icons, pinch to enter editor, auto-align, lock layout, change grid layout, create folders by dropping one app on another, and reorder desktop pages.
- [Huawei — Home Screen Overview](https://consumer.huawei.com/en/support/content/en-us15975200/): large folders, service widgets, page navigation indicators, shortcut bar.
- [Huawei — Basic Gestures and Shortcuts](https://consumer.huawei.com/en/support/content/en-us15802053/): pinch to edit, swipe down on desktop for search, swipe down from status bar for notifications, and system navigation gestures.
- [Huawei — Change the System Navigation Mode](https://consumer.huawei.com/en/support/content/en-us15802382/): edge-swipe Back, bottom-swipe Home, swipe-and-hold Recents, and bottom-edge app switching.
- [Huawei — Use Service Widgets](https://consumer.huawei.com/uk/support/content/en-gb15971050/): swipe up on supported app icons, add widgets, stack same-sized widgets, swipe through stacks, and manage widgets.
- [Huawei — App Drawer](https://consumer.huawei.com/en/support/content/en-us00784658/): swipe up to open drawer and long-press/drag an app onto the home screen.

## Behaviour mapping for Aether

| Reference behaviour | Aether implementation target |
| --- | --- |
| Pinch inward on desktop | Enter desktop edit mode |
| Long-press empty desktop | Enter desktop edit mode |
| Long-press app/widget/folder | Lift item into drag state and show placement targets |
| Drag app onto app | Create a folder; preserve both app shortcuts |
| Horizontal desktop swipe | Page with a real pager and animated page changes |
| Page indicators | Select a page; current page stays synchronized with pager |
| Page editor | Add, remove, reorder pages and choose primary page |
| Swipe down on desktop | Open launcher search |
| Swipe up from desktop content | Open app drawer without stealing Android's bottom system gesture |
| Large folder | Launch child apps directly; open full folder for overflow and management |
| Widget stack | Stack compatible Android AppWidgetHost views and swipe between them |
| App icon service-card gesture | Offer Android-compatible app shortcuts/widgets; do not claim compatibility with Huawei-only service-card APIs |

## Android platform boundaries

A normal third-party Android launcher cannot replace the operating system's privileged global Back, Home, Recents, notification shade, Quick Settings, lock-screen or system gesture handlers. Aether must leave those system-owned gestures to Android and provide launcher gestures in the remaining app content area. The default Home role must be requested through Android's supported Home role flow (API 29+) or the system Home-app settings/picker on older versions.

Huawei service widgets/cards are platform and app integrations. Aether can host standard Android widgets and app shortcuts, but cannot manufacture a third-party app's private HarmonyOS service-card implementation.

## Acceptance criteria

- Pinch on a free desktop area enters edit mode; normal one-finger scroll/tap/drag remains usable.
- Horizontal swipes change pages smoothly and page indicators remain synchronized.
- Swiping down from the desktop header opens search.
- The drawer can be opened from a launcher content gesture without intercepting Android's bottom-edge Home/Recents gesture.
- Page add/remove/reorder/primary selection survives process death.
- Dragging an app onto another app creates a folder; removing an app from a folder does not uninstall it.
- Widget stack operations are restricted to compatible Android widgets and persist their order.
- Default Home setting reports the actual system role state after returning from Android's confirmation UI.
- Cold-start performance is measured on physical hardware; do not claim a speed improvement from code inspection alone.

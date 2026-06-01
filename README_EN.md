# Luno

[中文](README.md)

Luno is an Android edge gesture, quick launcher, and system action enhancement tool. Configurable trigger areas let you bind actions like back, home, volume, app launch, virtual mouse, and more to swipe, long-swipe, tap, and long-press gestures.

Current version: `v1.6.0`

## Features

- **Gesture triggers** — Left, right, and bottom trigger areas; short-swipe, long-swipe, tap, and long-press gestures; per-button angle configuration.
- **Sub-gestures** — Each trigger button can configure 8-direction sub-gestures, executed by swiping anywhere on screen after trigger.
- **Action panel** — Expandable multi-action panel on long-swipe trigger, with arc and grid layouts.
- **Quick launcher** — Launch apps and shortcuts, hide apps, recent/frequent records.
- **System actions** — Back, home, recents, notification panel, quick settings, volume, mute, media controls, and more.
- **Tool actions** — Password generator, simulated click, virtual mouse (with continuous mode), screen scrolling, and more.
- **Mini window** — Configurable app mini-window position, margin, and offset.
- **Freeze management** — Shizuku-powered app freeze/unfreeze, one-click freeze, and protect list.
- **Personalization** — Dynamic colors, dark mode, customizable animation trail colors, icon scaling and background color.

## Requirements

- Android 13+ (API 33+)
- Accessibility Service must be enabled for gesture actions
- Shizuku environment required for app freeze/unfreeze

## Permissions

- **Accessibility Service**: executes back, home, recents, click, panel open, etc. No personal data collected.
- **Query app list**: app selection, shortcuts, blacklist, hide apps, freeze management.
- **Shizuku Provider**: freeze/unfreeze and other elevated operations.
- **Battery optimization & auto-start**: improves background service survival rate.
- Other permissions (notification, camera, Bluetooth, etc.) are requested on-demand for their respective system actions.

## Build

```bash
# Debug
./gradlew assembleDebug

# Release
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/{debug,release}/`

## Attribution

Based on [SideGesture](https://github.com/aaronzzx/SideGesture), retaining the original `LICENSE` and author attribution.

## License

See the [`LICENSE`](LICENSE) file in the repository root.

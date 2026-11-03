# InkLauncher

A lightweight Android launcher with a **Google Pixel** layout and a **monochrome e-ink** theme: paper (#F4F1EA), ink (#111111) and grey (#5A5A5A). No other colors, no shadows, no blur.

Package: `com.taro.inklauncher` · minSdk 26 · targetSdk 35 · Kotlin + Jetpack Compose · License **GPL-3.0**

> Built for the Redmi 10 2022 (MIUI 14, Android 13) and other phones with limited RAM. For daily use, install the **release** APK: Compose debug builds are much slower.

---

## Features

- **Pixel-style layout**
  - E-ink *At a Glance*: a large date ("Wednesday, Nov 4") plus one line for the next calendar event or the weather (Open-Meteo, no API key). Calendar and weather are **off by default**, so no permissions or internet are needed.
  - Grid of favorite icons (4×5 by default, configurable), **no labels**.
  - Dock with 4–5 icons and an app drawer button, then a capsule search bar with "G", mic and Lens.
  - Optional extra pages you swipe left/right (1 page by default).
- **Monochrome Material-style icons** without labels. Icon sources, in order:
  1. Official Android 13+ themed icon (`AdaptiveIconDrawable.getMonochrome()`).
  2. Package → **Material Symbols Outlined** map (phone, messages, camera, gallery, and so on). No brand logos.
  3. Grayscale fallback of the full icon (background + foreground + mask) with gentle contrast. The result is validated; if it is empty or flat, it falls back to the original icon in grayscale, then a Material Symbol, then the app's first letter in an outline circle.

  Icon background shape can be set to none, outline circle, or outline squircle.
- **App drawer**:
  - Opens with a swipe up or the dock button and follows your finger, with a short spring when you release.
  - Search bar at the top. The keyboard appears only when you tap it.
  - App names are shown only while searching.
  - Updates automatically when apps are installed or uninstalled.
- **Long-press an icon**:
  - App shortcuts
  - App info
  - Uninstall
  - Remove from home
  - Move (hold and drag, or choose *Move* and tap a destination, including the dock)
- **Long-press an empty area**: Wallpaper, Widgets, Custom widgets, Launcher settings.
- **System widgets** (including InkWidgets):
  - Widget picker grouped by app, with previews.
  - Bind and configure flow.
  - Resize with handles that snap to the grid, and remove.
- **Custom widget editor** (similar to KWGT, but lightweight): see below.
- **Launcher gestures**, each one can be turned off in Settings:
  - Swipe up: app drawer.
  - Swipe down: notifications.
  - Double tap: lock screen (through an optional accessibility service).
- **Backup & restore** of the layout, custom widgets and settings to a JSON file.

## Screenshots

| Home | Drawer | Widget editor |
|---|---|---|
| _(placeholder)_ | _(placeholder)_ | _(placeholder)_ |

## Download the APK

APKs are built by GitHub Actions on every push to `main`.

1. Open the **[Actions](https://github.com/Clarity69/ink-launcher/actions)** tab of this repo.
2. Open the most recent green (✓) **Build APK** run.
3. Under **Artifacts**, download:
   - **InkLauncher-release**: signed release APK (recommended), or
   - **InkLauncher-apk**: debug APK, built only when signing secrets are not set up.

   You need to be signed in to GitHub.
4. Unzip the downloaded file to get the APK.

To start a build manually: Actions tab → **Build APK** → **Run workflow**.

## Install

1. Copy the APK to your phone (for example with LocalSend) and open it from a file manager.
2. Allow *Install unknown apps* for that file manager when asked.
3. If Play Protect warns you, tap **More details → Install anyway**.
4. On MIUI, if a security warning appears, choose **Install anyway**.

> **Switching from the debug build to the release build:** the signatures differ, so Android will not install the release APK over the debug one. Before switching:
> 1. Make a backup in **Settings → Backup & restore**.
> 2. Uninstall the debug build once.
> 3. Install the release APK.
> 4. Restore the backup.

## Release signing

Release APKs are signed in GitHub Actions with your own keystore, which is stored as GitHub Secrets. The keystore and its passwords are **never** committed: `*.jks`, `*.keystore` and `keystore.properties` are in `.gitignore`. If the secrets are missing, the workflow still builds a debug APK and logs a warning.

Run these commands yourself in the **fish** shell. Every password is typed interactively; none of them go into the command line.

**1. Install keytool (if needed) and create the keystore** (RSA 4096, valid for 10000 days):

```fish
type -q keytool; or sudo pacman -S --needed jre-openjdk-headless
mkdir -p ~/keys; chmod 700 ~/keys
keytool -genkeypair -v -storetype PKCS12 -keystore ~/keys/inklauncher.jks -alias inklauncher -keyalg RSA -keysize 4096 -validity 10000
```

keytool asks for a password and your name/organization. With PKCS12 the key password is the same as the keystore password.

**2–3. Encode the keystore as single-line base64 and save the four secrets**:

```fish
base64 -w0 ~/keys/inklauncher.jks | gh secret set KEYSTORE_BASE64 -R Clarity69/ink-launcher
gh secret set KEYSTORE_PASSWORD -R Clarity69/ink-launcher
gh secret set KEY_PASSWORD -R Clarity69/ink-launcher
gh secret set KEY_ALIAS -R Clarity69/ink-launcher --body inklauncher
```

`KEYSTORE_PASSWORD` and `KEY_PASSWORD` prompt for the value; enter the same password both times. Then start a build:

```fish
gh workflow run build.yml -R Clarity69/ink-launcher
```

**4. Back up the keystore.** Copy `~/keys/inklauncher.jks` to at least one safe place outside this laptop (an encrypted USB drive or a password manager that stores files), and keep the password in your password manager.

> ⚠️ **If the keystore or its password is lost, updates can no longer be installed over the existing app.** You would have to uninstall it, which loses your layout and custom widgets.

## Set as default launcher on MIUI

**Settings → Apps → Manage apps → (⋮) Default apps → Home screen → InkLauncher**

You can also open the **InkLauncher Settings** icon from your old launcher and tap **Set as default launcher**.

## Custom widgets

Custom widgets are rendered directly by InkLauncher with Compose (they are not AppWidgets), so they stay lightweight and monochrome.

**Create a widget:**

1. Long-press an empty area → **Custom widgets** → **+**.
2. Set the widget size in grid cells (for example 4×2). A live preview is shown at the top.
3. Add layers:
   - **Text**
   - **Box**: rectangle, capsule or circle, filled or outline
   - **Icon** (Material Symbols)
   - **Dot-matrix clock** (5×7, 24/12 h)
   - **Divider**
   - **Dot progress** (battery, day or year)
4. Configure each layer:
   - Position and size (% or dp), alignment
   - Font (sans/serif/mono), font size
   - Color: Ink/Paper/Muted/transparent only
   - Tap action: open app, Google Search, Google Lens, clock or calendar
5. Changes are saved automatically. Back in the list, tap **⋮ → Add to home screen**.

**Text tokens** (replaced automatically; date formats follow the system locale):

`{time}` `{time12}` `{hour}` `{minute}` `{date}` `{day}` `{month}` `{year}` `{battery}` `{charging}` `{next_alarm}`

Time tokens refresh once a minute, only while the launcher is visible, and stop when it is not shown to save battery.

**Built-in presets:**

1. Dot clock + date
2. Search G + Lens
3. Serif clock + battery

**Sharing:**

- **Export**: **⋮ → Export** saves an `.inkw` (JSON) file.
- **Import**: use the import icon at the top right of the Custom widgets screen and pick an `.inkw` file.

## Project structure

```
app/src/main/java/com/taro/inklauncher/
├── MainActivity.kt, LauncherViewModel.kt, InkApp.kt
├── ui/   theme, home, drawer, glance, search, settings, widgeteditor, common
├── data/ AppRepository, IconProcessor, MaterialIconMap, Prefs, LayoutStore, GlanceProvider, Backup
├── widget/ WidgetHostManager, custom/ (JSON model, renderer, tokens, dot-matrix font, presets)
└── system/ LaunchHelper, StatusBarHelper, LockAccessibilityService
```

Builds run only on GitHub Actions (`.github/workflows/build.yml`). If `gradle-wrapper.jar` is missing, the workflow generates it with `gradle wrapper` first.

## License & attribution

- **InkLauncher** © Taro, released under the **GNU GPL v3.0** (see [LICENSE](LICENSE)).
- **[Lawnchair Launcher](https://github.com/LawnchairLauncher/lawnchair)** (GPL-3.0): some approaches are adapted and rewritten in a lightweight form with Compose:
  - themed/monochrome icons
  - Google Search/voice/Lens intents
  - the notification gesture
  - the widget host flow

  Files that adapt Lawnchair code carry a source comment at the top. No Launcher3/AOSP or Quickstep modules are used.
- **[Material Symbols](https://github.com/google/material-design-icons)** by Google, Apache License 2.0 ([licenses/Apache-2.0-MaterialSymbols.txt](licenses/Apache-2.0-MaterialSymbols.txt)).
- **Fonts**: [Figtree](https://github.com/erikdkennedy/figtree) and [Instrument Serif](https://github.com/Instrument/instrument-serif), SIL Open Font License 1.1 ([licenses/](licenses/)).
- Weather data from **[Open-Meteo](https://open-meteo.com/)** (CC BY 4.0).

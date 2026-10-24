# TinyWall+

**A simple view. Only what you need.**

📺 **A simple launcher for Android 9 — built for a projector.**
Five tiles, a clock, weather, 11 backgrounds, English and Russian — no ads, no
recommendations, no root, no firmware flashing. Developed and mostly tested on a
ThundeaL TD95MAX projector; likely to work on other Android 9 devices with a 16:9 screen.

**По-русски:** [полное описание на русском](README.ru.md) — простой лаунчер для Android 9,
разработанный и обкатанный на проекторе TD95MAX.

![TinyWall+ home screen](prototype/tinywall-home-en.png)

## ✨ What it is

Many Android 9 projectors and TV boxes ship with a vendor launcher full of
recommendations and services nobody asked for. That launcher is an ordinary Android app,
so it can be replaced — no root, no custom firmware, no risk of bricking the device.

TinyWall+ is that replacement: a small Java app with standard Android views, built to
stay light on 2 GB of RAM. It installs from a USB stick and is fully reversible — the
vendor launcher is never deleted, so you can switch back at any time.

There is nothing model-specific inside: it is a plain Android 9 launcher for a 16:9
screen and a D-pad remote. It was developed and primarily tested on the author's own
ThundeaL TD95MAX projector, and it will most likely work on other Android 9 projectors and
TV boxes that allow sideloading APKs and choosing a home app. Those devices are untested,
so treat the first run as an experiment: the vendor launcher stays in place, and switching
back takes a minute.

| | |
|---|---|
| Version | 0.10 |
| Android | 9 (API 28) and newer |
| Screen | 1920×1080, adapts to other 16:9 modes |
| APK size | 2.1 MB |
| Root | not required |
| Language | English (default), Russian |
| Tested on | ThundeaL TD95MAX (real unit) and the Android 9 emulator |

**Testing status:** the current release, 0.10, was developed and tested on the author's
own TD95MAX —
installation, assigning it as the home app, the remote, backgrounds, weather and
settings all work. It is also verified on the Android 9 emulator at 1920×1080. Earlier
builds 0.1–0.2 were the first ones tested on real hardware; 0.3–0.9 were mostly
emulator-only. If something misbehaves on your projector, please
[open an issue](../../issues) with your model and a photo of the screen.

## 🚀 Features

- 🧩 **Five empty tiles** on the first run — no preinstalled apps. A tile is assigned to a
  specific position (01–05); a long OK press replaces or clears it.
- 🗂️ **All apps grid** so the home screen stays clean.
- 🌤️ **Weather next to the clock**, with a city you type in by hand rather than pick from a
  list (Open-Meteo geocoding, wttr.in for current conditions). Needs internet.
- 🖼️ **11 backgrounds** with English and Russian names, plus a slow Ken Burns drift,
  adjustable dimming and blur (Off / Soft / Strong).
- ⚙️ **Modern grouped settings** — Background, Home screen, Appearance, Weather — with
  large buttons sized for a remote.
- 🔠 **Font size** Normal / Large / Very large, tile size S / M / L, dark and light theme.
- 📐 **Real screen resolution** shown in the status bar, and the layout rebuilds when the
  display mode changes.
- 🏠 **Make default** button that opens the system home-app chooser, so ADB is optional.

## 🖼️ Screenshots

| Home | Settings | Backgrounds |
|:---:|:---:|:---:|
| <img src="prototype/tinywall-home-en.png" width="320"> | <img src="prototype/tinywall-settings-en.png" width="320"> | <img src="prototype/tinywall-gallery-en.png" width="320"> |

More, including the earlier versions: [docs/screenshots.md](docs/screenshots.md).

## 📥 Install

1. Download `TinyWallPlus-0.10.apk` from [Releases](../../releases/latest) and check the
   SHA-256 from the release notes.
2. Copy it to a USB flash drive (FAT32 or NTFS).
3. Plug the drive into the projector and open the built-in file manager
   (Files / Media Center / Explorer).
4. Tap the APK and allow installation from unknown sources.
5. Open **TinyWall+** and press **Make default** → **Always**.

Detailed steps and how to roll back: [dist/README.md](dist/README.md) (Russian).

Over ADB:

```bash
adb install -r TinyWallPlus-0.10.apk
adb shell cmd package set-home-activity ru.tihiy.dom/.MainActivity
```

If nothing works on your firmware, [docs/plan.md](docs/plan.md) describes the whole
investigation: how the vendor launcher is built, which ADB commands reveal it, and what
can and cannot be replaced without flashing.

## 🌐 Web prototype

The interface started as an HTML prototype and still lives in the repository. Open
[`prototype/tinywall.html`](prototype/tinywall.html) in a browser — best at 1920×1080.

**Live demo:** <https://tatarovich.github.io/tinywall-plus/> — the same prototype, nothing
to install.
Arrow keys move focus, Enter opens, Esc goes back, `S` opens settings. No internet needed.

Useful URLs: `?lang=ru`, `?view=settings`, `?view=gallery`, `?view=tiles`,
`?view=apps&slot=0`, `?view=city&q=Moscow`, `?filled=1`.

An earlier iteration with four generated scenes is in
[`prototype/index.html`](prototype/index.html).

## 🔧 Build from source

Requirements: JDK 11+ (tested on JDK 25), Android SDK with `platforms;android-28` and
`build-tools;35.0.0`, plus `cwebp` from the `webp` package if you want to import your own
backgrounds. No Gradle — the build is a plain shell script around `aapt2`, `javac`, `d8`,
`zipalign` and `apksigner`.

```bash
SDK=/path/to/android-sdk bash android/build.sh
```

Without `KEYSTORE_PASS` the script produces an unsigned APK, which is enough to test
changes. Official releases are signed with a private key that is not in this repository
and is never published.

Details: [android/README.md](android/README.md) (Russian).

## 📁 Project layout

```
android/       Java sources, resources, build script
  src/         launcher, background controller, weather, app repository
  res/         strings, colors, styles, drawables
  assets/      backgrounds as WebP and scenes.txt (id|English|Russian)
backgrounds/   source images, 1920×1080 PNG
prototype/     HTML prototypes and screenshots
docs/          research notes and the full screenshot gallery
dist/          release notes for the APK
```

## ⚠️ Limitations

- The vendor **Settings**, autofocus and HDMI input switching stay vendor-owned. The
  launcher can open them, but cannot replace them.
- Without root, system apps cannot be uninstalled — only hidden from the home screen.
- A **firmware update will restore the stock launcher**; the home assignment has to be
  repeated afterwards.
- Assigning the launcher as home may require ADB on firmware that suppresses the system
  chooser.
- Weather needs `INTERNET` permission and degrades to a dash when offline.

## ⚖️ Disclaimer

This is an unofficial, community project. It is not affiliated with, endorsed by or
supported by ThundeaL. "TD95MAX" and "ThundeaL" are used only to describe the device the
launcher was developed and tested on. TinyWall+ has nothing to do with the TinyWall firewall for
Windows — the name is a coincidence of a working title.

Installing third-party software is done at your own risk. The launcher does not modify
the system partition, and the vendor launcher is left untouched, so the change is
reversible.

## 👥 Authors

**Tatarovich** and **DeepSeek** — a human and an AI model, listed here as two equal halves
of the project.

- **Tatarovich** — the projector, the idea and the requirements. Every decision about what
  the launcher should do, what to cut and how it should feel is his, plus testing on the
  real TD95MAX.
- **DeepSeek** — the AI co-author, working through **DeepSeek Harness**. Wrote the Java
  code, the HTML prototypes, the build scripts and these docs, and kept everything
  consistent between versions.

From Tatarovich: thank you for the code, the docs and the patience — without you this app
simply wouldn't exist.

## 🙏 Credits

- Backgrounds are AI-generated images made in **Google Flow** by Tatarovich.
- Weather data: [Open-Meteo](https://open-meteo.com/) geocoding and
  [wttr.in](https://wttr.in/).
- Some icons in the HTML prototype come from
  [Material Design Icons](https://github.com/google/material-design-icons)
  (Apache License 2.0).
- Built with the Android SDK build tools; no third-party runtime libraries.

## ☕ Support

TinyWall+ is free, MIT-licensed and stays that way: no pro version, no ads, no paywall.
If it saved you some time and you feel like buying the author a coffee, here is how.

**RUB — card transfer (Russian bank cards)**

```
5536 9140 3935 1840
```

A virtual card set aside for donations only. Card-to-card transfer. The author is
self-employed and declares this income.

**USDT — TRC-20 (TRON)**

```
TTMmuG68drdKfthzBo4t9rY5n8QCFe917n
```

**TON**

```
UQCI5KdNl1xOzpOnepB6eho-2SzTEoNd3VKUT1XkLCoFnOzm
```

**SOL — Solana**

```
9Ud1NujZTQTsANo5hK97AZVTbWrdkP3NnypGRjDkCLgK
```

**ETH — Ethereum**

```
0x0Cb745cBb0f86ec2D5F6E1F280Df6E383A7B4f5A
```

⚠️ **Rubles go to the card above, never to a crypto address.** The addresses below are
crypto only — each works only on its own network:

- USDT on TRC-20 → the TRON address;
- TON coins and TON jettons → the TON address;
- SOL and SPL tokens, including USDT-SPL → the Solana address;
- ETH and ERC-20 tokens, including USDT-ERC20 → the Ethereum address.

Anything sent on a different chain is lost for good: the networks are not compatible, and
there is no way to recover it.

Nothing is expected in return: no priority support, no features, no obligations.

## 📄 License

[MIT](LICENSE) © 2026 Tatarovich and the TinyWall+ authors.

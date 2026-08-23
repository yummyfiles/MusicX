<div align="center">

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./assets/MusicX_Banner.png">
  <img src="./assets/MusicX_Banner.png" alt="MusicX" width="100%" height="100%" />
</picture>

<p align="center">
  <a href="https://git.io/typing-svg">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://readme-typing-svg.demolab.com?font=Share+tech+mono&size=26&pause=1000&color=ffffff&center=true&vCenter=true&width=550&lines=MusicX+-+Offline+Music+Player;Completely+Ad-Free;No+subscriptions;No+paywalls+stalking+you+(%E2%89%96+-+%E2%89%96)">
      <img src="https://readme-typing-svg.demolab.com?font=Share+tech+mono&size=26&pause=1000&color=000000&center=true&vCenter=true&width=550&lines=MusicX+-+Offline+Music+Player;Completely+Ad-Free;No+subscriptions;No+paywalls+stalking+you+(%E2%89%96+-+%E2%89%96)" alt="Typing SVG" />
    </picture>
  </a>
</p>

<br/>

<a href="https://github.com/yummyfiles/MusicX/releases/latest">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://img.shields.io/badge/DOWNLOAD_APK-000000?style=for-the-badge&logo=android&logoColor=white&labelColor=000000&color=000000">
    <img src="https://img.shields.io/badge/DOWNLOAD_APK-ffffff?style=for-the-badge&logo=android&logoColor=000000&labelColor=ffffff&color=ffffff" alt="Download APK" width="220" />
  </picture>
</a>

<a href="https://yummyfiles.vercel.app/projects/musicx">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://img.shields.io/badge/PROJECT_HUB-000000?style=for-the-badge&logo=gitbook&logoColor=white&labelColor=000000&color=000000">
    <img src="https://img.shields.io/badge/PROJECT_HUB-ffffff?style=for-the-badge&logo=gitbook&logoColor=000000&labelColor=ffffff&color=ffffff" alt="Project Hub" width="220" />
  </picture>
</a>

<br/><br/>

<p align="center">
  <a href="https://ko-fi.com/Z5Z521S7ER">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi" width="200" />
  </a>
</p>

<br/>

**No ads • No subscriptions • No accounts**
No "Start Free Trial" button stalking you 😭

<p align="center">
  <a href="https://github.com/yummyfiles/MusicX/releases"><img src="https://img.shields.io/github/v/release/yummyfiles/MusicX?style=flat-square&label=Version&color=000000&labelColor=000000" alt="Release"></a>
  <a href="https://github.com/yummyfiles/MusicX/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-000000?style=flat-square" alt="License: GPL-3.0"></a>
  <a href="https://github.com/yummyfiles/MusicX/issues"><img src="https://img.shields.io/github/issues/yummyfiles/MusicX?style=flat-square&label=Issues&color=000000" alt="Issues"></a>
  <a href="https://github.com/yummyfiles/MusicX/pulls"><img src="https://img.shields.io/github/issues-pr/yummyfiles/MusicX?style=flat-square&label=PRs&color=000000" alt="Pull Requests"></a>
  <a href="https://github.com/yummyfiles/MusicX/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/yummyfiles/MusicX/release.yml?style=flat-square&label=Build&color=000000" alt="Build Status"></a>
  <a href="https://github.com/yummyfiles/MusicX/stargazers"><img src="https://img.shields.io/github/stars/yummyfiles/MusicX?style=flat-square&label=Stars&color=000000" alt="Stars"></a>
  <img src="https://img.shields.io/badge/minSdk-24-000000?style=flat-square" alt="minSdk 24">
  <img src="https://img.shields.io/badge/Kotlin-2.2-000000?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin 2.2">
</p>

<br/>

<strong>An offline-first music player for Android that respects your freedom, your library, and your sanity.</strong>

</div>

---

## ❯ Table of Contents

- [Why MusicX](#-why-musicx)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Download & Install](#-download--install)
- [Tech Stack](#-tech-stack)
- [Permissions](#-permissions)
- [Building From Source](#-building-from-source)
- [Project Structure](#-project-structure)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [FAQ](#-faq)
- [Support](#-support)
- [License](#-license)

---

## ❯ Why MusicX

I got tired of music apps stuffing everything behind subscriptions, ads, and features nobody asked for.

Most music apps are doing the absolute most — ads, premium popups, "Sign in to continue," and "Upgrade to unlock basic functionality" 💀. I just wanted to press play and hear music.

MusicX is built around one idea:

> **Your music should stay yours.**

Import your songs, organize your library, make playlists, and listen offline without getting nagged to upgrade. No streaming, no cloud, no tracking, no account creation side quest — zero nonsense.

It opens. It scans your songs. It plays them. Crazy concept, I know.

---

## ❯ Features

### 🎵 Playback

- **Offline-first playback** — plays audio stored locally on your device; no internet required.
- **Background playback** with a foreground media service.
- **Lock screen & notification controls** powered by Android Media3.
- **Gapless playback** — remove silence between consecutive tracks.
- **Shuffle & repeat** modes (off, one, all).
- **Seek bar** with elapsed/remaining time.
- **Pause on disconnect** — automatically pauses when headphones are removed.
- **Remember position** — resumes where you left off.
- **Fade on play/pause** transitions.
- **Autoplay next song** in the queue.

### 📚 Library & Organization

- **Local media scanning** through Android's `MediaStore`.
- **Songs list** sorted by title, with embedded album art.
- **Search** across titles, artists, and metadata.
- **Playlists** — create, rename, and curate your own collections.
- **Favorites** — mark tracks you love.
- **Music import** via the system file picker (`GetMultipleContents`).
- **Metadata editor** — edit song title, artist, and embedded lyrics.
- **File management** — delete tracks directly from the library (with scoped-storage consent).

### 🎤 Lyrics

- **Embedded lyrics** support (read from file metadata).
- **LRCLIB integration** — automatically fetches plain and synced lyrics online.
- **Synced (LRC) lyrics** with line-by-line highlighting as the song progresses.
- **Romanized lyrics** option for non-Latin scripts.
- Configurable **text size** and **centered alignment**.

### 🎨 Theming & Customization

- Deep, granular color theming — tweak backgrounds, surfaces, text, borders, accents, buttons, icons, sliders, toggles, and more.
- **Save & load custom themes**.
- Pure-black minimal or neon-science-experiment — your call.

### 🔊 Audio Engine

- **Master Equalizer**, **Bass Boost**, **Virtual Surround Sound**.
- **Loudness Normalization** and **Smart Gain**.

### ⚙️ Settings

Dedicated screens for **Playback**, **Library**, **Audio**, **Lyrics**, **Video**, **Appearance**, and **About**.

### 🛡️ Privacy

- No account. No sign-in. No tracking.
- Music and metadata stay on your device.
- The only network traffic is optional lyrics lookup via LRCLIB.

---

## ❯ Screenshots

<div align="center">
  <em>Screenshots coming soon.</em>
</div>

> Have great screenshots? Open a PR — they'd be a welcome addition!

---

## ❯ Download & Install

### Option 1 — Prebuilt APK (recommended)

1. Go to the [**latest release**](https://github.com/yummyfiles/MusicX/releases/latest).
2. Download the `app-release.apk` asset.
3. Open the APK on your Android device and enable **Install from unknown sources** if prompted.

> ℹ️ MusicX is not on Google Play. Sideloading the APK is the intended install method.

### Option 2 — Project Hub

Visit the [**MusicX Project Hub**](https://yummyfiles.vercel.app/projects/musicx) for documentation, wiki, and guides.

### Requirements

| | |
|---|---|
| **Android version** | Android 7.0 (API 24) or higher |
| **Target SDK** | 35 |
| **App version** | 2.0.3 (`versionCode` 4) |
| **Architecture** | ARM / ARM64 / x86 / x86_64 |

---

## ❯ Tech Stack

| Category | Technology |
|-----------|-----------|
| Language | [Kotlin](https://kotlinlang.org/) 2.2 |
| UI | [Jetpack Compose](https://developer.android.com/compose) (Material 3) |
| Architecture | MVVM |
| Navigation | [AndroidX Navigation3](https://developer.android.com/navigation) |
| Playback | [AndroidX Media3](https://developer.android.com/media/media3) / ExoPlayer |
| Database | [Room](https://developer.android.com/training/data-storage/room) 2.7 |
| Preferences | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| Image Loading | [Coil](https://coil-kt.github.io/coil/) |
| Serialization | [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) |
| Networking | `HttpURLConnection` (lyrics fetch), Retrofit/Moshi available |
| Build | Gradle (Kotlin DSL), AGP 9.3 |
| CI | GitHub Actions |

---

## ❯ Permissions

MusicX asks for only what it needs:

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Scan and read your local music library. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep playing music while the app is in the background. |
| `POST_NOTIFICATIONS` | Show the player notification and controls. |
| `INTERNET` | Optional — fetch lyrics from LRCLIB. Not used for tracking or streaming. |

No location, no contacts, no camera, no telemetry.

---

## ❯ Building From Source

### Prerequisites

- **JDK 17** (the project uses the `temurin-17` distribution in CI).
- **Android SDK** with `compileSdk` 37 / `buildTools` matching AGP 9.3.x.
- Android Studio (recommended) or the standalone Gradle wrapper.

### Clone & build

```bash
git clone https://github.com/yummyfiles/MusicX.git
cd MusicX

# Debug build
./gradlew assembleDebug

# Release build (unsigned if no keystore is configured)
./gradlew assembleRelease
```

Output APKs land in `app/build/outputs/apk/`.

### Install on a connected device

```bash
./gradlew installDebug
```

### Release signing

Release builds sign automatically when a keystore is present:

- Place `release.jks` in a `keystore/` directory at the repo root.
- Provide `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD` environment variables (these are wired into the GitHub Actions release workflow).

If the keystore is absent, Gradle still produces an **unsigned release APK** you can sign manually.

### CI/CD

Pushing a tag matching `v*` triggers [`.github/workflows/release.yml`](.github/workflows/release.yml), which builds the release APK and publishes a GitHub Release with auto-generated notes.

---

## ❯ Project Structure

```
MusicX/
├── app/
│   └── src/main/java/com/yummyfiles/musicx/
│       ├── data/            # Room database, DAOs, repositories, settings
│       ├── model/           # Song, Playlist domain models
│       ├── playback/        # Media3 PlaybackService & MusicController
│       └── ui/
│           ├── components/  # Reusable composables (MiniPlayer, icons)
│           ├── import/      # File import screen
│           ├── metadata/    # Metadata editor
│           ├── navigation/  # Destinations & app scaffold
│           ├── nowplaying/  # Now Playing + synced lyrics
│           ├── playlists/   # Playlist list & detail
│           ├── search/      # Search
│           ├── settings/    # All settings screens + ViewModel
│           ├── songs/       # Songs list & ViewModel
│           ├── splash/      # Splash screen
│           └── theme/       # Color, typography, theming
├── assets/                  # Banner & branding assets
├── gradle/                  # Version catalog (libs.versions.toml)
├── wiki/                    # In-repo wiki content
└── .github/                 # Issue templates & release workflow
```

---

## ❯ Roadmap

Ideas and planned work — contributions welcome:

- [ ] Tablet / foldable adaptive layouts (Navigation3 Adaptive scaffolding is already in the version catalog).
- [ ] Android Auto support.
- [ ] Equalizer preset UI (the audio engine flags are in place).
- [ ] More metadata sources and embedded artwork polish.
- [ ] Sleep timer.
- [ ] Crossfade between tracks.
- [ ] ReplayGain tag support.

Check the [issue tracker](https://github.com/yummyfiles/MusicX/issues) for current bugs and feature requests.

---

## ❯ Contributing

Contributions are genuinely appreciated — bug fixes, features, docs, and screenshots all help.

1. **Fork** the repository and create a branch off `main`.
2. Make your change, keeping the style consistent with the surrounding Kotlin/Compose code.
3. Test on a real device (the app targets Android 7.0+).
4. Open a **Pull Request** against `main` and describe what changed and why.

For larger changes, please open an [issue](https://github.com/yummyfiles/MusicX/issues/new) first so we can discuss the approach.

Please follow the existing issue templates when reporting bugs or requesting features:

- 🐛 [Bug report](https://github.com/yummyfiles/MusicX/issues/new?template=bug_report.yml)
- ✨ [Feature request](https://github.com/yummyfiles/MusicX/issues/new?template=feature_request.yml)

---

## ❯ FAQ

**Does it stream music?**
No. MusicX plays local files only — there is no streaming backend.

**Does it need internet?**
No. The only network call is an optional lyrics lookup against LRCLIB; everything else works fully offline.

**Are there ads?**
Never. No ad SDKs are bundled.

**Do I need an account?**
Absolutely not 💀

**Is my music uploaded anywhere?**
No. Your library stays on your device.

**Where are my settings stored?**
Locally via AndroidX DataStore, and playlists/favorites live in a local Room database.

**Does it support synced lyrics?**
Yes — LRC files and LRCLIB synced lyrics are highlighted line-by-line during playback.

**What audio formats work?**
Everything ExoPlayer/Media3 can read, including MP3, M4A/AAC, OGG, FLAC, WAV, and (where the device supports it) OPUS and WebM/MKV audio.

---

## ❯ Support

If MusicX makes your listening life a little less infuriating, consider supporting development:

<p align="center">
  <a href="https://ko-fi.com/Z5Z521S7ER">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="ko-fi" width="220" />
  </a>
</p>

Stars, shares, and kind words also go a long way ⭐

---

> [!CAUTION]
> **Keep Android Open**
>
> MusicX supports open-source Android and user freedom.
> No lock-ins. No forced services. No platform control.
>
> Learn more: https://keepandroidopen.org

---

## ❯ License

MusicX is licensed under the **GNU General Public License v3.0**.
See the [LICENSE](LICENSE) file for the full text.

```
MusicX - Offline Music Player for Android
Copyright (C) yummyfiles

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

---

<div align="center">

<p align="center">
  <img src="./assets/visualizer.svg" alt="MusicX Visualizer" width="100%">
</p>

<br/>

**Open app → pick song → play music. That's it.**

<sub>No popups. No paywalls. No emotional manipulation to buy premium 😭</sub>

</div>

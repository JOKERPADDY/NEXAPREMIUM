# NEXAPREMIUM

NEXAPREMIUM is an Android streaming app for browsing movies and series, searching a title catalog, and watching available video streams. The interface uses Jetpack Compose and supports phone layouts as well as Android TV and Fire TV launchers.

## Features

- Browse featured titles and catalog rows, with a dedicated grid for each row's **View All** action.
- Search titles and view popular searches.
- Open movie and series detail pages, browse seasons and episodes, and select available playback quality and subtitles.
- Save playback progress locally and resume from **Continue Watching**.
- Keep a local **My List** and manage offline downloads, including pause, resume, and delete actions.
- Use player gestures: swipe up on the left side to adjust volume and on the right side to adjust screen brightness.

Favorites, downloads, and watch progress are stored on the device; the app does not currently synchronize them between devices.

## Requirements

- JDK 17 or newer (the project targets Java 17 bytecode).
- Android SDK Platform 35.
- Android Studio or the included Gradle wrapper.
- Android 8.0 (API 26) or newer to install the app.

## Build

From the repository root:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected Android device with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Run unit tests

```bash
.\gradlew.bat testDebugUnitTest
```

## Backend

The app currently uses `https://movie-box-sepia-six.vercel.app` for catalog, title detail, playback, captions, and search requests. Its base URL is configured in `app/src/main/java/com/nexapremium/di/NetworkModule.kt`.

## Project layout

```text
app/src/main/java/com/nexapremium/
├── app/          # Activity and navigation
├── core/ui/      # Compose theme
├── data/         # API models, repositories, Room database, downloads
├── di/           # Hilt modules
├── player/       # Media3 player engine and subtitle parsing
└── ui/           # Screens, reusable components, and ViewModels
```

More detailed design notes and API investigation are in [ARCHITECTURE.md](ARCHITECTURE.md), [NEXAPREMIUM_API_AUDIT.md](NEXAPREMIUM_API_AUDIT.md), and [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

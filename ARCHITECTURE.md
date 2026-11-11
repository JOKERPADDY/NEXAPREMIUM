# NEXAPREMIUM — APPLICATION ARCHITECTURE SPECIFICATION

**Package:** `com.nexapremium`  
**Architecture Pattern:** Clean Architecture + MVVM + Modular Feature Design  
**UI Stack:** Jetpack Compose (Material 3) + Android TV Compose Focus Management  
**DI Framework:** Hilt (`com.google.dagger:hilt-android`)  
**Network Layer:** Retrofit 2 + OkHttp 4.11.0 + Kotlinx Serialization  
**Database Layer:** Room DB (`androidx.room`)  
**Player Engine:** AndroidX Media3 ExoPlayer (`androidx.media3:media3-exoplayer`)  
**Image Engine:** Coil Compose (`io.coil-kt:coil-compose`)  

---

## 1. LAYERED ARCHITECTURE SCHEMATIC

```mermaid
graph TD
    subgraph UI Layer (Jetpack Compose M3)
        A[Screens & Views] --> B[ViewModels / UI State Flow]
        B --> C[TV Remote / D-Pad Focus Manager]
    end

    subgraph Domain Layer
        D[Use Cases / Interactors] --> E[Repository Interfaces]
        E --> F[Domain Entities & Models]
    end

    subgraph Data Layer
        G[Repository Implementations] --> H[Remote Data Source - Retrofit]
        G --> I[Local Data Source - Room DB]
        H --> J[MovieBox OpenAPI Backend]
        I --> K[Room SQLite Database]
    end

    subgraph Player Layer
        L[ExoPlayer Screen] --> M[Media3 ExoPlayer Wrapper]
        M --> N[OkHttpDataSource.Factory with Custom Headers]
        N --> O[HakunaMatata CDN Video/Subtitle Streams]
    end

    B --> D
    G --> E
```

---

## 2. COLOR PALETTE & DESIGN SYSTEM CONTRACT

- **Primary Background:** `#000000` (Pure Black for OLED TV & Cinematic depth)
- **Primary Surface:** `#141414` (Dark Charcoal)
- **Secondary Surface:** `#1E1E1E` (Medium Charcoal)
- **Primary Accent / Brand:** `#E50914` (Netflix Red)
- **Primary Text:** `#FFFFFF` (High contrast white)
- **Secondary Text:** `#A0A0A0` (Muted silver gray)
- **Focus Indicator (TV):** Bright White Border (2dp) + 1.05x Scale Transformation + `#E50914` Glow.

---

## 3. CORE MODULE STRUCTURE & PACKAGE LAYOUT

```
com.nexapremium
├── app/
│   ├── NexaApplication.kt (Hilt Application)
│   └── MainActivity.kt (Single Activity Host with NavHost & Edge-to-Edge)
├── core/
│   ├── model/ (Domain models & OpenAPI DTO mappings)
│   ├── network/ (Retrofit, OkHttp, Interceptors, Headers, API endpoints)
│   ├── database/ (Room DB, Entities: ContinueWatching, MyList, PlaybackProgress, DAOs)
│   ├── player/ (Media3 ExoPlayer wrapper, OkHttpDataSource, Subtitle parser, Focus controls)
│   └── ui/ (Theme, Type, Color, FocusableSurface, Carousel, Posters, TV Nav Rail)
└── feature/
    ├── splash/ (Splash Screen)
    ├── onboarding/ (Onboarding Screen)
    ├── home/ (Cinematic Banner Carousel, Featured Sections, Content Rows)
    ├── movies/ (Movies Catalog & Filter Sheet)
    ├── series/ (Series Catalog & Filter Sheet)
    ├── search/ (Search Bar, Autocomplete Chips, Results Grid, Popular Keywords)
    ├── detail/ (Movie & Series Details, Cast, Seasons, Episode Selector)
    ├── player/ (Media3 Video Player, Transport Controls, Quality Selector, Subtitles)
    ├── mylist/ (Bookmarked Favorites Grid)
    ├── continuewatching/ (Continue Watching Rail & Management)
    ├── downloads/ (Download Manager placeholder / authorized items)
    └── settings/ (App Configuration, App Info, Playback Preferences)
```

---

## 4. NETWORK & MEDIA PLAYER SECURITY CONTRACT

1. **Custom Network Header Interceptor:**
   All ExoPlayer video stream requests to `bcdnxw.hakunaymatata.com` and caption requests to `cacdn.hakunaymatata.com` MUST include:
   - `User-Agent: okhttp/4.11.0` (to avoid HTTP 428 Precondition Required).
   - `Referer: https://themoviebox.xyz/movies/{detail_path}`
   - `Origin: https://themoviebox.xyz`

2. **Error Recovery Strategy:**
   - Exponential backoff retry for Vercel cold starts (~1-2s response time).
   - Video resolution fallback: 1080p -> 720p -> 480p -> 360p if higher bitrate fails.
   - Ignore non-functional DASH manifests (`sbcdnw.hakunaymatata.com` 403) and default to MP4 streams.

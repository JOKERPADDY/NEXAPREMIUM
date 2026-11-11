# NEXAPREMIUM — DEVELOPMENT ROADMAP & MILESTONES

**Project:** NEXAPREMIUM (`com.nexapremium`)  
**Target Date:** November 2026  

---

## MILESTONE 1 — PHASE 1: INSPECTION & PLANNING (COMPLETED)
- [x] Task 1.1: Inspect local development environment (Java 26, Android SDK 34-37, ADB).
- [x] Task 1.2: Verify `google-services (1).json` package name (`com.nexapremium`).
- [x] Task 1.3: Audit backend API (`https://movie-box-sepia-six.vercel.app`) and extract OpenAPI schema (`NEXAPREMIUM_OPENAPI.json`).
- [x] Task 1.4: Execute live endpoint test battery and record response samples (`NEXAPREMIUM_ENDPOINT_TESTS.md` & `NEXAPREMIUM_RESPONSE_SAMPLES.json`).
- [x] Task 1.5: Define Clean Architecture, 6-Agent file ownership matrix, and color palette (`#000000`, `#141414`, `#1E1E1E`, `#E50914`).
- [x] Task 1.6: Create initial coordinator documentation (`PROJECT_STATUS.md`, `ARCHITECTURE.md`, `AGENT_ASSIGNMENTS.md`, `DEVELOPMENT_ROADMAP.md`, `TEST_REPORT.md`, `KNOWN_ISSUES.md`).

---

## MILESTONE 2 — PHASE 2: FOUNDATION & GRADLE SETUP
- [ ] Task 2.1: Initialize Android project root structure with Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`).
- [ ] Task 2.2: Configure Gradle dependencies: Compose M3, Hilt, Retrofit, OkHttp, Room, Media3 ExoPlayer, Coil, Navigation Compose.
- [ ] Task 2.3: Implement `NexaApplication` (`@HiltAndroidApp`) and `MainActivity` (`@AndroidEntryPoint`).
- [ ] Task 2.4: Implement core UI design system (`Theme.kt`, `Color.kt`, `Type.kt`, `FocusableSurface.kt`).
- [ ] Task 2.5: Verify project builds cleanly with zero compilation errors.

---

## MILESTONE 3 — PHASE 3: PARALLEL MODULE DEVELOPMENT
- [ ] Task 3.1: **Agent 3 (API):** Implement Retrofit `NexaApiService`, OpenAPI DTO models, `HeaderInterceptor`, and repositories (`Home`, `Catalog`, `Detail`, `Search`).
- [ ] Task 3.2: **Agent 5 (Database):** Implement Room DB `NexaDatabase`, Entities (`ContinueWatchingEntity`, `MyListEntity`, `PlaybackProgressEntity`), and DAOs.
- [ ] Task 3.3: **Agent 4 (Player):** Build Media3 ExoPlayer wrapper with `OkHttpDataSource.Factory`, Header Injection, and subtitle parser.
- [ ] Task 3.4: **Agent 2 (UI):** Build 14 Compose screens (Splash, Onboarding, Home, Movies, Series, Search, Movie Detail, Series Detail, Season/Episode Selector, Video Player, My List, Continue Watching, Downloads, Settings) with D-pad support.

---

## MILESTONE 4 — PHASE 4: INTEGRATION & WIREUP
- [ ] Task 4.1: Connect UI screens to ViewModels and Hilt Repositories.
- [ ] Task 4.2: Wire Detail screens to API playback endpoints (`/v1/titles/{path}/playback`).
- [ ] Task 4.3: Wire Video Player screen to ExoPlayer engine and caption selector (`/v1/titles/{path}/captions`).
- [ ] Task 4.4: Wire playback position saving to Room DB (`PlaybackProgressDao`) and Continue Watching rail.
- [ ] Task 4.5: Wire My List bookmarking buttons to `MyListDao`.

---

## MILESTONE 5 — PHASE 5: QA, TESTING & PERFORMANCE
- [ ] Task 5.1: Run unit tests for Repositories, ViewModels, and Room DAOs.
- [ ] Task 5.2: Test phone touch interactions and TV D-pad remote focus navigation.
- [ ] Task 5.3: Profile image caching, video buffering, and Vercel cold-start error handling.
- [ ] Task 5.4: Generate Debug APK and verify installation.

---

## MILESTONE 6 — PHASE 6: FINAL DELIVERY
- [ ] Task 6.1: Package complete source code, README, Architecture documentation, Test Report, and Debug APK.

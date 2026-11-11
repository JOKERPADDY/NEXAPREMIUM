# NEXAPREMIUM — AI SUBAGENT ASSIGNMENTS & FILE OWNERSHIP MATRIX

**Lead Architect & Coordinator:** Agent 1 (Architect)  
**Strict Rule:** No agent may modify files owned by another agent without Coordinator approval.

---

## 1. SUBAGENT ROLES & FILE OWNERSHIP

### AGENT 1 — ARCHITECT / COORDINATOR
- **Role:** Technical Lead, Architecture enforcement, Code integration, Compilation checks.
- **Owned Files & Directories:**
  - `build.gradle.kts` (Root)
  - `settings.gradle.kts`
  - `gradle/libs.versions.toml` (Version Catalog)
  - `PROJECT_STATUS.md`
  - `ARCHITECTURE.md`
  - `AGENT_ASSIGNMENTS.md`
  - `DEVELOPMENT_ROADMAP.md`
  - `TEST_REPORT.md`
  - `KNOWN_ISSUES.md`
  - `com.nexapremium.app.NexaApplication`
  - `com.nexapremium.app.MainActivity`
  - `com.nexapremium.app.navigation.NexaNavHost`

### AGENT 2 — UI/UX DEVELOPER
- **Role:** Premium Streaming UI, Jetpack Compose screens, Material 3 Dark Palette, TV D-pad Focus.
- **Owned Files & Directories:**
  - `com.nexapremium.core.ui.**` (Theme, Colors, Typography, FocusableSurface, PosterCard, BannerCarousel, TVNavRail)
  - `com.nexapremium.feature.splash.**`
  - `com.nexapremium.feature.onboarding.**`
  - `com.nexapremium.feature.home.**`
  - `com.nexapremium.feature.movies.**`
  - `com.nexapremium.feature.series.**`
  - `com.nexapremium.feature.search.**`
  - `com.nexapremium.feature.detail.**`
  - `com.nexapremium.feature.mylist.**`
  - `com.nexapremium.feature.continuewatching.**`
  - `com.nexapremium.feature.downloads.**`
  - `com.nexapremium.feature.settings.**`

### AGENT 3 — API / NETWORK DEVELOPER
- **Role:** Retrofit 2, OkHttp Interceptors, OpenAPI DTOs, Repositories, Serialization, Error Handling.
- **Owned Files & Directories:**
  - `com.nexapremium.core.model.**` (Title, Detail, Playback, Stream, Caption, CatalogFilters, etc.)
  - `com.nexapremium.core.network.**` (NexaApiService, NetworkModule, HeaderInterceptor, RemoteDataSource)
  - `com.nexapremium.core.repository.CatalogRepository`
  - `com.nexapremium.core.repository.TitleRepository`
  - `com.nexapremium.core.repository.SearchRepository`

### AGENT 4 — MEDIA PLAYER DEVELOPER
- **Role:** Media3 ExoPlayer integration, Custom OkHttpDataSource.Factory, Player UI, Controls, Subtitles, Quality.
- **Owned Files & Directories:**
  - `com.nexapremium.core.player.**` (ExoPlayerWrapper, PlayerModule, SubtitleDecoder, QualitySelector)
  - `com.nexapremium.feature.player.**` (VideoPlayerScreen, PlayerControlsOverlay, TVTransportControls)

### AGENT 5 — DATABASE / STORAGE DEVELOPER
- **Role:** Room DB, Entities, DAOs, Local Persistence, Preferences, Watch Progress.
- **Owned Files & Directories:**
  - `com.nexapremium.core.database.**` (NexaDatabase, DatabaseModule, ContinueWatchingDao, MyListDao, PlaybackProgressDao)
  - `com.nexapremium.core.database.entity.**` (ContinueWatchingEntity, MyListEntity, PlaybackProgressEntity)
  - `com.nexapremium.core.repository.UserPreferencesRepository`
  - `com.nexapremium.core.repository.WatchHistoryRepository`

### AGENT 6 — QA / PERFORMANCE ENGINEER
- **Role:** Testing, Verification, Performance Profiling, Memory Leaks, D-pad Navigation Audit, Bug Reporting.
- **Owned Files & Directories:**
  - `app/src/test/**` (Unit Tests)
  - `app/src/androidTest/**` (UI & Integration Tests)
  - `TEST_REPORT.md` (Updates)
  - `KNOWN_ISSUES.md` (Updates)

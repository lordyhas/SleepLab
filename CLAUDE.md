# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew :app:test --tests "com.lordyhas.sleeplab.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator).
# Keep the flag: without it Gradle uninstalls the app afterwards, wiping recorded nights on the phone.
./gradlew connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true

# Lint
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

The app must be tested on a **physical device** — emulators lack proper microphone input for the core snore-detection feature.

## Architecture

Single-module Android app (`app/`) using MVVM + Clean Architecture. Three layers:

### Data layer (`data/`)
- **Entities**: `TreatmentEntity`, `SleepSessionEntity`, `SnoreEventEntity`, `NoiseSampleEntity`
- **DAOs**: One per entity under `data/local/dao/`
- **Repositories**: Thin wrappers around DAOs under `data/repository/`
- **`SettingsRepository`**: SharedPreferences-backed `StateFlow`s for user preferences — manual threshold, auto-calibrate toggle, and tonight's treatment choice (shared by Home and Tracking; resolve it with `SettingsRepository.resolveTreatmentId`)
- **`AppDatabase`**: Room DB v3, named `snore_tracker_database`. Schemas are exported to `app/schemas/` (commit them). **Every schema change needs a `Migration`** in `data/local/Migrations.kt`, appended to `ALL_MIGRATIONS` and covered by `MigrationTest` (androidTest). Only pre-release v1 databases fall back to destructive migration.
- **Per-night settings**: `SleepSessionEntity` stores the `thresholdDb` actually used and the measured `noiseFloorDb` (null = manual threshold), plus `note` and `morningRating` (1–5). Delete a night with `SleepSessionDao.deleteNight` (removes its events and samples in one transaction).

### Service layer (`service/`)
- **`SleepTrackingService`**: Foreground service that manages the sleep session lifecycle (creates/completes `SleepSession`, records `SnoreEvent` and `NoiseSample` rows, holds `PARTIAL_WAKE_LOCK`)
- **`AudioAnalyzer`**: Reads from `AudioRecord` at 16 kHz mono PCM-16, computes RMS→dB, and fires `onSnoreDetected` when consecutive above-threshold windows last ≥1 s. dB values are relative (`20·log10(rms)`), not calibrated SPL.
- **`NoiseFloorCalibrator`**: Pure Kotlin (unit-tested). With auto-calibrate on, the analyzer feeds it the first 30 s of each night with detection paused; the floor is the 25th-percentile level and threshold = floor + 12 dB, clamped to 35–75 dB. The result is saved on the session and published in `TrackingState`.
- **`TrackingState`**: A `data class` held in a static `StateFlow` on `SleepTrackingService.Companion` — this is the shared state bus between the service and the UI; no binder/AIDL needed

### UI layer (`ui/`)
- Jetpack Compose screens: `HomeScreen`, `TrackingScreen`, `AnalyticsScreen`, `TreatmentsScreen`, `SessionDetailScreen`
- Navigation defined in `AppNavHost` with a bottom nav bar; routes are sealed objects in `Screen`
- The Tracking tab shows a live badge when `trackingState.isTracking == true`
- **Design system**: dark-only night theme in `ui/theme/` (type scale with tabular figures, shapes, full M3 surface tokens, `snoreLevelColor()` severity bands). Shared composables live in `ui/components/` (`NightBackground`, `GlassCard`, `ScreenHeader`, `MetricTile`, `Pill`, `GaugeRing`, `HoldToConfirmButton`, `MorningRatingSelector`, `rememberMicPermissionGate`); duration formatting in `ui/util/Formatters.kt`. Reuse these instead of styling screens ad hoc.
- **Insets**: the outer `Scaffold` in `AppNavHost` has zero content insets and only reserves the bottom bar. Tab screens add the status-bar padding themselves (`WindowInsets.statusBars`); screens with their own `TopAppBar` (Session detail) get it from that.

### Dependency injection
The README mentions Hilt, but **Hilt is not used**. `SleepLabApp` is a manual DI container — repositories are exposed as lazy properties and ViewModels pull them via `SleepLabApp.instance.*Repository`.

## Key Technical Details

- **minSdk 24**, **compileSdk/targetSdk 37**, Kotlin 2.2.10, AGP 9.3.3
- Annotation processing uses **KSP** (not kapt)
- `NoiseSample` rows are recorded every 10 seconds during a session for the waveform chart
- Snore intensity score on session completion = average of `avgDb` and `maxDb`, clamped to 0–100 (to be replaced in Phase 2 of the roadmap)
- All user-facing text lives in `res/values/strings.xml` (EN) and `res/values-fr/strings.xml` (FR); add every new string to both. Escape apostrophes as `\'` or use the typographic `’` — a bare `'` fails `mergeDebugResources`.

## Roadmap

Planned work is in `docs/ROADMAP.md`, one phase per branch/PR, each tested on a physical device before the next starts.

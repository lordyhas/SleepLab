# SleepLab

SleepLab is a native Android application (AI Healthcare & Wellness to be) designed to track sleep, record and quantify snoring in the background, and correlate this data with medical treatments followed by the user to measure their effectiveness.

## Features

*   **Sleep Tracking & Snore Detection:** Runs a foreground service to monitor audio levels during sleep, securely detecting and recording snoring events using `AudioRecord`.
*   **Treatment Correlation:** Users can log their medical treatments (e.g., anti-snoring devices, nasal sprays) and link them to specific sleep sessions.
*   **Analytics Dashboard:** Visual graphs showing snore intensity and duration over time, overlaid with active treatment periods to easily analyze what works best.
*   **Treatment Management:** Manage active and past treatments easily.
*   **Real-time Visualization:** An active tracking screen showing current status.

## Tech Stack

*   **Language:** Kotlin
*   **UI Toolkit:** Jetpack Compose, Material Design 3
*   **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
*   **Database:** Room Database (Offline first)
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Dependency Injection:** Manual — `SleepLabApp` is a small container exposing the repositories (no Hilt/Dagger)
*   **Annotation Processing:** KSP
*   **Audio Processing:** `AudioRecord`, Foreground Services
*   **Charts/Graphs:** Drawn with Compose `Canvas` (no charting library)

## Architecture Overview

The app follows a modern Android architecture:
- **`data/`**: Contains Room database entities (`Treatment`, `SleepSession`, `SnoreEvent`), DAOs, and repository implementations.
- **`service/`**: Contains the `SleepTrackingService` (Foreground Service) and `AudioAnalyzer` for background audio processing and decibel calculation.
- **`ui/`**: Contains Jetpack Compose screens (`home`, `tracking`, `analytics`, `treatments`), ViewModels, Navigation logic, and the Material 3 UI theme.

## Permissions Used

- `RECORD_AUDIO`: To listen for snoring events.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: To continue tracking sleep while the app is in the background or screen is locked.
- `POST_NOTIFICATIONS`: To display the persistent foreground service notification.
- `WAKE_LOCK`: To prevent the device from sleeping too deeply while actively processing audio.
- `RECEIVE_BOOT_COMPLETED`: To re-schedule the optional bedtime reminder after a reboot.

## Getting Started

1. Clone the repository.
2. Open the project in **Android Studio**.
3. Build and run the app on a **physical device** (Emulators may not have proper microphone input for testing the core feature).
4. Grant the necessary permissions on the first run.

```bash
./gradlew assembleDebug   # build
./gradlew test            # unit tests
./gradlew lint            # lint
```

Unit tests and lint also run on every pull request (`.github/workflows/ci.yml`). Planned work is in [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Releasing

A release is built only from the `production` branch (`.github/workflows/release.yml`): when a commit lands on it, typically by merging `main` into it, or when the workflow is started by hand on that branch. The workflow runs the unit tests and lint, builds a signed APK and App Bundle, and publishes a GitHub Release tagged `v<versionName>` with both files and their checksums.

Before each release, bump `versionName` and `versionCode` in `app/build.gradle.kts`: the workflow stops if the tag already exists.

One-time setup — repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | the release keystore, base64-encoded (`base64 -w0 release.keystore`) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

To build a signed release locally, keep the keystore outside the repository and create an untracked `keystore.properties` at the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`, then run `./gradlew assembleRelease`. Without it the release build is left unsigned. Keep the keystore safe: updates must be signed with the same key.

## License
This project is open-source. Please check the `LICENSE` file for more details.

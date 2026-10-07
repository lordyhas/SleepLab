# SleepLab 
- Old name : SnoreLab  (SnoreTracker)

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
*   **Dependency Injection:** Dagger Hilt
*   **Audio Processing:** `AudioRecord`, Foreground Services
*   **Charts/Graphs:** Compose-compatible charting libraries

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

## Getting Started

1. Clone the repository.
2. Open the project in **Android Studio**.
3. Build and run the app on a **physical device** (Emulators may not have proper microphone input for testing the core feature).
4. Grant the necessary permissions on the first run.

## License
This project is open-source. Please check the `LICENSE` file for more details.

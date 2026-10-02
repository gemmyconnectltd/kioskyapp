# Kiosky Kids Mobile (Frontend) Setup

This repository contains the Kiosky Kids mobile app (Android + shared Kotlin code).  
The backend API is a separate repository.

## What’s Inside

- `app/` Android UI and entry point
- `shared/` Shared Kotlin code (Ktor APIs, models, business logic)

## Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK (installed via Android Studio)

## Configure the API Base URL

The app reads the API base URL from two places. Update both when switching environments:

1. `shared/src/commonMain/kotlin/com/example/kioskyapp/apiServices/KtorClient.kt`
2. `app/build.gradle.kts`

Example:

```kotlin
private const val DEFAULT_API_BASE_URL = "https://shieldkidapi.onrender.com"
```

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://shieldkidapi.onrender.com\"")
```

## Run the App (Android)

1. Open the repo in Android Studio.
2. Sync Gradle.
3. Connect a physical phone or start an emulator.
4. Run the `app` configuration.

From terminal:

```bash
./gradlew :app:installDebug
```

## Notes

- If you use a local backend, set the API base URL to your laptop IP and ensure the phone is on the same Wi‑Fi.
- For hosted backend usage, no local server is required.


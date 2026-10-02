# kioskyApp Mobile + Backend Setup

This repo contains:

- Android app (Compose) in `app/` + shared Kotlin code in `shared/`
- Backend server (Node/Express + Prisma) in `Kiosky_bn/`

Below is a full local setup guide for running the backend on your laptop and the Android app on a physical phone.

## Prerequisites

Install the following:

- Android Studio (with Android SDK + emulator tools if you ever need them)
- JDK 17 (or the JDK version required by your Android Gradle plugin)
- Node.js 18+ and npm
- PostgreSQL 14+
- Git

## Backend Setup (Kiosky_bn)

1. Create a database (example):

```sql
CREATE DATABASE kioskyapp;
```

2. Create `.env` in `Kiosky_bn/.env` (do not commit secrets):

```env
DATABASE_URL="postgresql://postgres:YOUR_PASSWORD@localhost:5432/kioskyapp?schema=public"
JWT_SECRET="replace_with_secure_secret"
PORT=5000

# Email settings (replace with real values)
EMAIL_PASS=YOUR_EMAIL_APP_PASSWORD
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=your_email@gmail.com
EMAIL_FROM=your_email@gmail.com
```

3. Install dependencies:

```bash
cd Kiosky_bn
npm install
```

4. Generate Prisma client and run migrations:

```bash
npm run prisma:generate
npm run prisma:migrate
```

5. Start the backend:

```bash
npm run dev
```

The server binds to `0.0.0.0`, so it is reachable from your phone on the same Wi‑Fi.

## Android App Setup

1. Open the project root in Android Studio.
2. Sync Gradle if prompted.

3. Set the API base URL (this is required when your laptop IP changes):

- `shared/src/commonMain/kotlin/com/example/kioskyapp/apiServices/KtorClient.kt`
  - Update `DEFAULT_API_BASE_URL`
- `app/build.gradle.kts`
  - Update `buildConfigField("String", "API_BASE_URL", "...")`

Example:

```kotlin
private const val DEFAULT_API_BASE_URL = "http://192.168.1.80:5000"
```

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.80:5000\"")
```

4. Rebuild and reinstall on the phone:

```bash
./gradlew :app:installDebug
```

## Running on a Physical Phone

1. Phone and laptop must be on the same Wi‑Fi.
2. Confirm laptop IP:

```bash
ip addr
```

3. Use that IP in both places listed above.

## Common Issues

- **Connect timeout**:
  - Backend not running
  - Phone not on same Wi‑Fi
  - Wrong IP or old app build still installed
  - Server not bound to `0.0.0.0` (it should be in this project)

- **Wrong URL in logs**:
  - You updated `KtorClient.kt` but not `app/build.gradle.kts`
  - App was not rebuilt/reinstalled after changing IP

## Useful Commands

```bash
# Backend
cd Kiosky_bn
npm run dev

# Android app (from repo root)
./gradlew :app:installDebug
```

## Project Structure

- `app/` Android UI and app entry point
- `shared/` Shared Kotlin code and Ktor APIs
- `Kiosky_bn/` Backend server (Node + Prisma)

# kioskyapp
# kioskyapp

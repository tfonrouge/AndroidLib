# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-module Android library project with two modules:
- **`:androidLib`** — Reusable Android library (MVVM ViewModels, Compose UI components, Ktor HTTP client, barcode scanning). Published as `com.fonrouge:androidLib:1.1.0` to Maven Local.
- **`:app`** — Demo/test application consuming the library.

Both depend on internal libraries: `com.fonrouge.fsLib:base` (models, API interfaces, state) and `com.fonrouge.arelLib:arelLib`.

## Build Commands

```bash
./gradlew build                    # Full build
./gradlew :app:assembleDebug       # Debug APK
./gradlew :app:installDebug        # Install debug APK to device
./gradlew :androidLib:assemble     # Build library AAR
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest     # Instrumented tests
./gradlew lint                     # Android Lint

# Publish library to Maven Local
./gradlew :androidLib:publishReleasePublicationToMavenLocalRepository
```

## Architecture

**MVVM with Compose Navigation** — ViewModels manage state via `StateFlow`, Compose screens observe and render.

Key ViewModel hierarchy in `androidLib/src/main/java/com/fonrouge/androidLib/viewModel/`:
- `VMBase` — Base VM with snackbar, state alerts, confirm alerts
- `VMContainer` — Adds filter support via `ICommonContainer<T, ID, FILT>`
- `VMList` — Adds pagination (`Pager`/`BasePagingSource`), periodic updates
- `VMItem` — Single item CRUD operations
- `VMCamera` — Camera/barcode scanning

**Common Container pattern** (`configCommon/commonContainer.kt`): Generic `ICommonContainer<T, ID, FILT>` provides type-safe API filtering, serialization, and Compose Navigation route generation with parameter encoding/decoding.

**HTTP client** (`commonServices/AppApi.kt`): Singleton Ktor client with configurable engine (Android/CIO/OkHttp), cookie support, JSON content negotiation, 15s timeout, login/logout form submission.

**Pagination**: `BasePagingSource` in `domain/` loads paginated API data, cached in `viewModelScope`.

## Tech Stack

- Kotlin 2.3.0 with Compose plugin, JVM target 21
- AGP 8.13.2, Gradle 8.14.3
- Compose BOM 2025.12.01 with Material3
- Ktor Client 3.3.3
- Jetpack Navigation Compose 2.9.6, Paging Compose 3.3.6
- CameraX 1.5.2 + ML Kit Barcode Scanning
- Min SDK 30, Target/Compile SDK 36

## Dependency Management

Version catalog at `gradle/libs.versions.toml`. When updating dependencies, watch for transitive conflicts — recent commits show exclusions needed for guava and META-INF resources. The `androidLib` module excludes `META-INF/**` in packaging options.

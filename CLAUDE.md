# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-module Android library project:
- **`:androidLib`** — Reusable Android library (MVVM ViewModels, Compose UI components, Ktor HTTP client, JSON-RPC 2.0, barcode scanning). Published as `com.fonrouge:androidLib:1.1.0`.
- **`:app`** — Demo/test application consuming the library.
- **`:samples:showcase-android`** — Reference app demonstrating full integration with an fsLib backend (route discovery, paginated lists, CRUD forms).

Dependencies on external library: `com.fonrouge.fslib:core` (models, API interfaces, state).

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

### ViewModel Hierarchy (`androidLib/.../viewModel/`)
- `VMBase` — Base VM with snackbar, state alerts, confirm alerts
- `VMContainer` — Adds filter support via `ICommonContainer<T, ID, FILT>`
- `VMList` — Pagination (`Pager`/`BasePagingSource`), periodic updates, pull-to-refresh
- `VMItem` — Single item CRUD (Create, Read, Update, Delete) with two-phase Query→Action pattern
- `VMCamera` — Camera/barcode scanning (StateFlow-based)

### JSON-RPC 2.0 Remote Calling (`commonServices/`)
- `RouteRegistry` — Injectable class that discovers RPC routes from `/apiContract` endpoint. Thread-safe via Mutex. Supports version validation and auto re-discovery on stale routes.
- `IServiceProxy` — Interface for explicit service name mapping (decouples Android class names from server interface names).
- `call()` / `remoteCall()` — Inline extension functions for type-safe JSON-RPC calls. Method names resolved automatically from the call stack via `callerMethodName()`.
- `AppApi` — Singleton Ktor client with configurable engine (Android/CIO/OkHttp), retry with exponential backoff, cookie support, JSON content negotiation, configurable log level.

### Common Container Pattern (`configCommon/commonContainer.kt`)
Generic `ICommonContainer<T, ID, FILT>` provides type-safe API filtering, serialization, and Compose Navigation route generation with `Uri.encode`/`Uri.decode` parameter encoding.

### Pagination
`BasePagingSource` in `domain/` loads paginated API data, cached in `viewModelScope`. Catches all exceptions and reports via `pushSimpleState`.

### Injectable Globals
- `var appApi: IAppApi` — HTTP client reference (swap for testing)
- `var routeRegistry: RouteRegistry` — Route discovery (swap for testing)

## Tech Stack

- Kotlin 2.3.10 with Compose plugin, JVM target 21
- AGP 9.1.0
- Compose BOM 2026.03.00 with Material3
- Ktor Client 3.4.1 (with HttpRequestRetry)
- Jetpack Navigation Compose 2.9.7, Paging Compose 3.4.2
- CameraX 1.5.3 + ML Kit Barcode Scanning
- Min SDK 30, Target/Compile SDK 36

## Dependency Management

Version catalog at `gradle/libs.versions.toml`. The `androidLib` publish version reads from `libs.versions.androidLib.get()`.

Watch for transitive conflicts — guava is explicitly included. The `showcase-android` sample excludes `core-jvm` from `showcase-lib` to avoid duplicate fsLib classes.

## ProGuard / R8

Consumer rules in `androidLib/consumer-rules.pro` keep:
- `@Serializable` JSON-RPC wire types
- `IServiceProxy` implementors (method names used by stack trace reflection)
- kotlinx.serialization generated serializers

## Testing

Unit tests in `androidLib/src/test/` using JUnit 4, kotlinx.coroutines.test, Turbine, MockK, and Ktor MockEngine.

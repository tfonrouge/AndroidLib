# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multi-module Android library project:
- **`:android`** — Reusable Android library (MVVM ViewModels, Compose UI components, Ktor HTTP client, JSON-RPC 2.0). Published as `com.fonrouge.fslib:android:2.0.0`.
- **`:barcode`** — Optional barcode scanning module (CameraX, ML Kit, GMS code scanner). Published as `com.fonrouge.fslib:barcode:2.0.0`.
- **`:samples:showcase-android`** — Reference app demonstrating full integration with an fsLib backend (route discovery, paginated lists, CRUD forms).

Dependencies on external library: `com.fonrouge.fslib:core` (models, API interfaces, state).

## Build Commands

```bash
./gradlew build                    # Full build
./gradlew :android:assemble        # Build library AAR
./gradlew :barcode:assemble        # Build barcode AAR
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest     # Instrumented tests
./gradlew lint                     # Android Lint

# Publish snapshot to Maven Local
./gradlew publishToMavenLocal -PSNAPSHOT
```

## Architecture

**MVVM with Compose Navigation** — ViewModels manage state via `StateFlow`, Compose screens observe and render.

### ViewModel Hierarchy (`android/.../viewModel/`)
- `VMBase` — Base VM with snackbar, state alerts, confirm alerts
- `VMContainer` — Adds filter support via `ICommonContainer<T, ID, FILT>`
- `VMList` — Pagination (`Pager`/`BasePagingSource`), periodic updates, pull-to-refresh
- `VMItem` — Single item CRUD (Create, Read, Update, Delete) with two-phase Query→Action pattern

### Barcode Scanning (`barcode/` module)
- `VMCamera` — Camera/barcode scanning (StateFlow-based)
- `BarcodeCamera` — CameraX + ML Kit wrapper with torch control
- `ScanBarcodeScreen` — Router composable (GMS or CameraX)

### JSON-RPC 2.0 Remote Calling (`commonServices/`)
- `RouteRegistry` — Resolves RPC routes via convention (`/rpc/{service}.{method}`) by default. Optional `/apiContract` discovery for version validation or counter-based routes. Thread-safe via Mutex.
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
- CameraX 1.5.3 + ML Kit Barcode Scanning (in `:barcode` module)
- Min SDK 30, Target/Compile SDK 36

## Dependency Management

Version catalog at `gradle/libs.versions.toml`. The publish version reads from `libs.versions.fslibAndroid.get()`.

Watch for transitive conflicts — guava is explicitly included in `:barcode`. The `showcase-android` sample excludes `core-jvm` from `showcase-lib` to avoid duplicate fsLib classes.

## ProGuard / R8

Consumer rules in `android/consumer-rules.pro` keep:
- `@Serializable` JSON-RPC wire types
- `IServiceProxy` implementors (method names used by stack trace reflection)
- kotlinx.serialization generated serializers

## Testing

Unit tests in `android/src/test/` and `barcode/src/test/` using JUnit 4, kotlinx.coroutines.test, Turbine, MockK, and Ktor MockEngine.

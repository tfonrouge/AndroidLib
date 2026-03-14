# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Shadow protection guard now fails at configuration time instead of execution time, for configuration-cache compatibility.
- Signing tasks are disabled for local publishes (`-PSNAPSHOT` / `-PFORCE_LOCAL`) to avoid unnecessary GPG overhead and configuration-cache issues.

### Removed
- Removed `:app` demo module — the `:samples:showcase-android` module serves as the integration test and reference app.

## [2.0.0] - 2026-03-14

### Breaking Changes
- Maven coordinates changed from `com.fonrouge:androidLib` to `com.fonrouge.fslib:android`.
- Package renamed from `com.fonrouge.androidLib` to `com.fonrouge.fslib.android`.
- Barcode scanning extracted into separate `com.fonrouge.fslib:barcode` module.
- Gradle module renamed from `:androidLib` to `:android`.
- GitHub repository renamed from `AndroidLib` to `fslib-android`.

### Added
- **`:barcode` module** — CameraX, ML Kit, and GMS code scanner extracted from core. Independent module with no dependency on `:android`. Published as `com.fonrouge.fslib:barcode:2.0.0`.
- **Convention-based routing** — `RouteRegistry` resolves routes via `/rpc/{serviceName}.{methodName}` by default. No `discover()` call needed for fsLib servers using `@RpcBindingRoute`.
- **Configurable route prefix** — `routeRegistry.routePrefix` (default `"/rpc"`).
- **`-PSNAPSHOT` publishing** — `./gradlew publishToMavenLocal -PSNAPSHOT` appends `-SNAPSHOT` to the version.
- **Shadow protection** — Publishing release versions to Maven Local is blocked by default. Use `-PSNAPSHOT` or `-PFORCE_LOCAL` to override.
- **Maven Central publishing** — GPG-signed artifacts with POM metadata, staged via `publishAllPublicationsToStagingRepository` and uploaded via `publishToCentralPortal`.
- **Cross-repo CI** — `snapshot-compat` job (PR-only) builds against latest fsLib source.
- **`fslibVersion` property override** — Build against a specific fsLib version with `-PfslibVersion=X.Y.Z`.
- **Documentation** — README.md, USAGE-GUIDE.md, per-module READMEs, CHANGELOG.md.
- **MIT License**.

### Changed
- `RouteRegistry.getRoute()` falls back to convention route instead of throwing when discovery hasn't been called.
- `remoteCall()` simplified — no longer wraps `getRoute()` in a try/catch re-discovery fallback.
- `.gitignore` uses global `**/build` pattern instead of per-module entries.
- fsLib dependency bumped to 3.0.3.

### Removed
- Barcode dependencies (CameraX, ML Kit, GMS scanner, accompanist-permissions, guava) removed from `:android` module.
- Stale `app/androidLib/` directory removed.
- Stale `@OptIn(ExperimentalGetImage::class)` annotation removed from `VMList`.

## [1.1.0] - 2026-03-13

### Added
- Unit tests for `VMBase`, `RouteRegistry`, `BasePagingSource`, and `VMCamera` using JUnit 4, MockK, Turbine, and Ktor MockEngine.
- GitHub Actions CI workflow (build + test).
- Consumer ProGuard rules for `@Serializable` JSON-RPC types, `IServiceProxy` implementors, and kotlinx.serialization serializers.
- Production-grade `HttpRequestRetry` (3 retries, exponential backoff) in `AppApi`.

### Changed
- Version catalog used for publish version (`libs.versions.androidLib.get()`).
- fsLib dependency migrated from local Maven to Maven Central (3.0.1 → 3.0.2).

## [1.0.0] - 2026-03-12

### Added
- Initial release of `com.fonrouge:androidLib`.
- MVVM ViewModel hierarchy: `VMBase`, `VMContainer`, `VMList`, `VMItem`, `VMCamera`.
- JSON-RPC 2.0 client with `RouteRegistry` discovery, `IServiceProxy`, and `call()` extension functions.
- `AppApi` singleton Ktor HTTP client with configurable engine (Android/CIO/OkHttp).
- Compose UI components: `ScreenList`, `ScreenConfirmAlert`, `ScreenStateAlert`, `Select`, `SelectRemoteItem`, `PeriodicUpdateList`.
- Navigation helpers: `composableItem()`, `composableList()`, `navigateItem()`, `navigateCreateItem()`.
- `BasePagingSource` for Jetpack Paging 3 integration.
- `ICommonContainer` pattern for type-safe navigation and serialization.
- Barcode scanning: `BarcodeCamera` (CameraX + ML Kit), `ScanBarcodeScreen`, `GmsScanScreen`, `CameraXScanScreen1`.
- Showcase sample app (`samples/showcase-android`) demonstrating full fsLib backend integration.

[Unreleased]: https://github.com/tfonrouge/fslib-android/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/tfonrouge/fslib-android/releases/tag/v2.0.0
[1.1.0]: https://github.com/tfonrouge/fslib-android/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/tfonrouge/fslib-android/releases/tag/v1.0.0

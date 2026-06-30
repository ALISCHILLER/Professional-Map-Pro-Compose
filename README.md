# ProfessionalMapPro

ProfessionalMapPro is a production-oriented Android navigation foundation built with Kotlin, Jetpack Compose, MapLibre, OSRM-compatible routing, offline map packs, foreground navigation service, route progress, voice guidance, and modular Clean Architecture boundaries.

This repository is an **Android-only** codebase. It is not a Kotlin Multiplatform project in its current shape: the source sets are Android/JVM source sets such as `main`, `debug`, and `test`, and there are no `commonMain`, `androidMain`, `iosMain`, or `desktopMain` KMP source sets.

- Deep final code/class pass: WorkManager persisted observation now ignores already handled finished WorkInfo IDs, preventing repeated completed-work polling churn.

## Current verification status

The project has passed the included static, architecture, test-suite guardrail, and release-artifact checks in this review flow. A real Gradle build/test run is still blocked until the official Gradle Wrapper JAR is generated and committed:

```text
Missing file: gradle/wrapper/gradle-wrapper.jar
```

Do not treat this repository as fully build-verified until the wrapper JAR is present and the Gradle tasks in this README pass on a machine with Android SDK/NDK installed.

## Architecture

The project follows a modular Clean Architecture style:

```text
app
feature:map
core:model
core:geo
core:mapdata
core:routing
core:location
core:progress
core:guidance
core:offline
core:service
core:observability
```

Dependency direction is intentionally inward:

- `app` wires application-level configuration and hosts the Android shell.
- `feature:map` owns map screen orchestration, presentation state, controllers, and feature use cases.
- `core:*` modules expose focused contracts and provider-neutral models.
- Android, MapLibre, Firebase, WorkManager, fused-location, and foreground-service details stay behind adapter contracts.

Important contracts and use cases include:

- `RoutingRepository`
- `LocationRepository`
- `MapCatalogRepository`
- `OfflineMapRepository`
- `RouteProgressRepository`
- `CalculateRouteUseCase`
- `ResolveRouteRequestUseCase`
- `ApplyRouteCalculationOutcomeUseCase`
- `StartNavigationUseCase`
- `UpdateNavigationProgressUseCase`
- `PrepareOfflineRoutePackUseCase`
- `BuildNavigationSnapshotUseCase`
- `MapNavigationController`
- `MapRouteController`
- `MapLocationController`
- `MapGuidanceController`
- `MapOfflineController`

## Navigation safety policy

Route alternatives carry an explicit navigation policy:

- `RouteNavigationPolicy.Navigable`
- `RouteNavigationPolicy.PreviewOnly`

Provider-backed routes may be navigable. Straight-line fallback routes are preview-only and must not replace an active navigation route during reroute failure. UI state exposes `canStartNavigation` so Compose controls do not need to duplicate routing policy.

Coroutine cancellation is also part of the contract. Routing/location/offline adapters must rethrow `CancellationException` and must not convert cancellation into a provider failure.

## Technology stack

Versions are defined in `gradle/libs.versions.toml` and wrapper properties:

| Area | Version |
| --- | --- |
| Kotlin | `2.4.0` |
| Android Gradle Plugin | `9.0.1` |
| Gradle distribution | `9.5.0` |
| Java/Kotlin toolchain | `17` |
| CI JDK | `21` |
| compileSdk / targetSdk | `36` |
| minSdk | `26` |
| Jetpack Compose BOM | `2026.06.00` |
| Ktor | `3.4.3` |
| Kotlinx Coroutines | `1.10.2` |
| Kotlinx Serialization | `1.9.0` |
| MapLibre Android | `13.3.0` |
| AndroidX Lifecycle | `2.10.0` |
| WorkManager | `2.10.5` |
| Firebase BOM | `34.14.0` |
| NDK | `28.2.13676358` |
| CMake | `3.22.1` |

AGP 9 built-in Kotlin is enabled. Android modules intentionally do not apply `org.jetbrains.kotlin.android`; Kotlin Android compilation is provided by AGP. The top-level build file pins Kotlin Gradle Plugin `2.4.0` for AGP built-in Kotlin and Kotlin compiler plugins such as Compose.

## Requirements

Install or configure:

- JDK 21 for CI parity; Gradle toolchains compile the project with Java 17.
- Android Studio with Android SDK 36.
- NDK `28.2.13676358`.
- CMake `3.22.1`.
- Bash for the verification scripts on Linux/macOS/Git Bash/WSL.

## First-time setup

Generate and commit the official Gradle Wrapper JAR before relying on hermetic builds:

```bash
./scripts/bootstrap-gradle-wrapper.sh
git add gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties gradlew gradlew.bat
```

Then verify the environment:

```bash
./scripts/verify-build-environment.sh
```

The wrapper distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`.

## Build commands

Static and architecture checks:

```bash
./scripts/verify-static.sh
./scripts/verify-architecture.sh
./scripts/verify-test-suite.sh
```

Debug build:

```bash
./gradlew :app:assembleDebug --stacktrace
```

Unit-test matrix:

```bash
./gradlew \
  :core:model:test \
  :core:mapdata:test \
  :core:routing:test \
  :core:progress:test \
  :core:guidance:test \
  :core:geo:testDebugUnitTest \
  :core:location:testDebugUnitTest \
  :core:offline:testDebugUnitTest \
  :core:service:testDebugUnitTest \
  :core:observability:testDebugUnitTest \
  :feature:map:testDebugUnitTest \
  --stacktrace
```

Lint matrix:

```bash
./gradlew \
  :app:lintDebug \
  :feature:map:lintDebug \
  :core:location:lintDebug \
  :core:offline:lintDebug \
  :core:service:lintDebug \
  :core:observability:lintDebug \
  --stacktrace
```

Release build:

```bash
./gradlew :app:assembleRelease --stacktrace
```

## Runtime and build-time configuration

Routing configuration is injected through Gradle properties or environment variables:

```bash
PMP_OSRM_BASE_URL="https://your-routing-provider.example" \
PMP_ROUTING_USER_AGENT="ProfessionalMapPro/1.0" \
./gradlew :app:assembleRelease
```

Use a contracted/owned routing and tile provider before production release. Public demo providers are not a production SLA.

Release signing is read from Gradle properties or environment variables:

```text
PMP_RELEASE_STORE_FILE
PMP_RELEASE_STORE_PASSWORD
PMP_RELEASE_KEY_ALIAS
PMP_RELEASE_KEY_PASSWORD
```

Secret files such as keystores, `.env`, real `google-services.json`, and signing properties are ignored by `.gitignore` and must not be committed.

## Security posture

- `android:allowBackup="false"` is set.
- `android:usesCleartextTraffic="false"` is set.
- OSRM configuration enforces HTTPS by default with explicit debug/build opt-in for cleartext.
- Navigation notifications use a private lock-screen visibility and a redacted public version.
- Release signing is externalized through environment/Gradle properties.
- Firebase Gradle plugins are applied only when `app/google-services.json` exists.
- The project does not intentionally log passwords, tokens, signing secrets, cookies, or authorization headers.

## API overview

This repository has no Spring Boot backend, no REST controllers, and no database migrations. The primary integration boundary is the routing provider contract:

```text
RoutingRepository.calculateRoute(RouteRequest): RoutingResult
```

The default adapter is OSRM-compatible and uses Ktor. Offline maps are handled through MapLibre and WorkManager-backed download orchestration.

## Troubleshooting summary

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `gradle-wrapper.jar is missing` | Wrapper JAR was not generated/committed | Run `./scripts/bootstrap-gradle-wrapper.sh`, then commit the JAR. |
| `ANDROID_HOME` or `ANDROID_SDK_ROOT` missing | Android SDK is not configured | Install Android Studio/SDK and export the SDK path. |
| NDK/CMake errors | Native geo module requires pinned tools | Install NDK `28.2.13676358` and CMake `3.22.1`. |
| Firebase plugin disabled | `app/google-services.json` is absent | Add a real local Firebase file for builds that need Firebase. Do not commit secrets unless your org policy explicitly allows it. |
| Release APK unsigned | Signing env/properties are missing | Set `PMP_RELEASE_*` variables. |
| Routing fails in release | Routing provider not configured or unreachable | Set `PMP_OSRM_BASE_URL` and validate provider SLA/network policy. |

## Documentation files

The documentation set is intentionally limited to exactly four Markdown files:

1. `README.md` - English project overview and technical handoff.
2. `DEV_SETUP_FA.md` - Persian development setup guide.
3. `RELEASE_DEPLOY_FA.md` - Persian release/deployment checklist.
4. `DEV_PRD_WINDOWS_LINUX_FA.md` - Persian DEV/PRD and Windows/Linux operations guide.

## UI Hardening

The map feature now uses a small Compose Material 3 design system: `MapUiKit`, `MapGlassPanel`, `MapSectionCard`, `StatusPill`, an executive `MapHudOverlay`, dynamic color-aware app theming, adaptive large-screen side-panel behavior, bounded compact bottom-sheet controls, and accessibility semantics for key panels. The HUD exposes the highest-value actions first — permission/GPS, route calculation, navigation, and offline download — while detailed controls remain grouped into professional section cards. Keep UI work inside these primitives so future screens stay consistent, responsive, and easier to audit.

## UI hardening update

The map UI now includes an adaptive command-center shell: a map scrim for overlay readability, a compact route-focus card in the HUD, reusable key-value metric tiles, a bottom-sheet handle for compact layouts, and a large-screen side panel. These primitives keep the map interaction product-grade while preserving the existing Compose + MapLibre architecture.
- UI hardening: adaptive map shell, premium HUD, route focus card, centralized layout tokens, executive status strip, and an accessible quick-action system with 48dp touch targets for location, routing, navigation, and offline actions.

## UI hardening note

This delivery includes an accessible action system and an executive control-panel header for the map screen. The HUD and panel share the same action primitives, status tiles, route focus card, Material 3 shape tokens, and adaptive layout tokens so compact and expanded layouts stay consistent.

## Final code hardening note

This pass standardizes every production map-panel action through `MapPrimaryAction` and `MapSecondaryAction`. Raw Compose `Button` / `OutlinedButton` usage is intentionally isolated inside `MapQuickActions.kt`, so touch targets, shape, spacing and accessibility behavior stay consistent across HUD, routing, guidance, location and offline panels. The WorkManager observer cleanup path was also tightened so a completed worker removes its observer map entry and exits instead of self-cancelling the currently running observer coroutine.

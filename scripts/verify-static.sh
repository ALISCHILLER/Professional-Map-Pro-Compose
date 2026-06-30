#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "[1/19] Checking Kotlin package/path consistency and raw delimiter balance"
python3 - <<'PY'
from pathlib import Path
import re, sys
errors=[]
for p in Path('.').glob('**/*.kt'):
    text=p.read_text()
    for left,right in [('(',')'),('[',']'),('{','}')]:
        if text.count(left) != text.count(right):
            errors.append(f'{p}: unbalanced {left}{right}: {text.count(left)} vs {text.count(right)}')
    m=re.search(r'^package\s+([\w.]+)', text, re.M)
    if m:
        parts=str(p).split('/')
        if 'java' in parts:
            index=parts.index('java')+1
        elif 'kotlin' in parts:
            index=parts.index('kotlin')+1
        else:
            index=-1
        if index > 0:
            expected='.'.join(parts[index:-1])
            if expected != m.group(1):
                errors.append(f'{p}: package {m.group(1)} does not match path {expected}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)

# Catch duplicate named arguments in call sites that static text checks used to
# miss when a full Gradle compile is unavailable in this environment. The scanner
# only looks at top-level arguments inside parenthesized call blocks, so data-class
# constructor declarations such as `val enabled: Boolean = true` are ignored.
def top_level_args(block):
    parts=[]
    start=0
    parens=braces=brackets=0
    for index, char in enumerate(block):
        if char == '(':
            parens += 1
        elif char == ')':
            parens -= 1
        elif char == '{':
            braces += 1
        elif char == '}':
            braces -= 1
        elif char == '[':
            brackets += 1
        elif char == ']':
            brackets -= 1
        elif char == ',' and parens == braces == brackets == 0:
            parts.append(block[start:index])
            start = index + 1
    parts.append(block[start:])
    return parts

for p in Path('.').glob('**/*.kt'):
    text = p.read_text(encoding='utf-8', errors='ignore')
    stack=[]
    for index, char in enumerate(text):
        if char == '(':
            stack.append(index)
        elif char == ')' and stack:
            start=stack.pop()
            names=[]
            for arg in top_level_args(text[start + 1:index]):
                match=re.match(r'\s*([A-Za-z_][A-Za-z0-9_]*)\s*=', arg)
                if not match:
                    continue
                name=match.group(1)
                if name in names:
                    errors.append(f'{p}: duplicate named argument {name}')
                names.append(name)
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Kotlin static checks passed.')
PY

echo "[2/19] Checking XML well-formedness"
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
import sys
errors=[]
for p in Path('.').glob('**/*.xml'):
    try:
        ET.parse(p)
    except Exception as exc:
        errors.append(f'{p}: {exc}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('XML checks passed.')
PY

echo "[3/19] Checking version catalog duplicate keys"
python3 - <<'PY'
from pathlib import Path
import sys
catalog=Path('gradle/libs.versions.toml')
section=None
seen={}
errors=[]
for line_no,line in enumerate(catalog.read_text().splitlines(), start=1):
    stripped=line.strip()
    if not stripped or stripped.startswith('#'):
        continue
    if stripped.startswith('[') and stripped.endswith(']'):
        section=stripped
        seen.setdefault(section,set())
        continue
    if '=' in stripped and section:
        key=stripped.split('=',1)[0].strip()
        if key in seen[section]:
            errors.append(f'{catalog}:{line_no}: duplicate key {key} in {section}')
        seen[section].add(key)
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Version catalog checks passed.')
PY

echo "[4/19] Syntax-checking C++/JNI"
JNI_INCLUDE="$(dirname "$(readlink -f "$(command -v javac)")")/../include"
JNI_LINUX="$JNI_INCLUDE/linux"
g++ -std=c++17 -Wall -Wextra -Werror -fsyntax-only \
  -I"$JNI_INCLUDE" -I"$JNI_LINUX" \
  core/geo/src/main/cpp/geokit.cpp

echo "[5/19] Checking launcher scripts"
if [[ ! -x "gradlew" ]]; then
  echo "gradlew must be executable." >&2
  exit 1
fi
if [[ ! -f "gradle/wrapper/gradle-wrapper.properties" ]]; then
  echo "gradle-wrapper.properties is missing." >&2
  exit 1
fi
if ! grep -q "gradle-wrapper.jar" gradlew; then
  echo "gradlew must prefer the official Gradle wrapper JAR when it is committed." >&2
  exit 1
fi
if ! grep -q "GradleWrapperMain" gradlew; then
  echo "gradlew must be able to launch org.gradle.wrapper.GradleWrapperMain." >&2
  exit 1
fi
if [[ ! -f "gradlew.bat" ]]; then
  echo "gradlew.bat is missing." >&2
  exit 1
fi
if ! grep -q "gradle-wrapper.jar" gradlew.bat; then
  echo "gradlew.bat must prefer the official Gradle wrapper JAR when it is committed." >&2
  exit 1
fi
if ! grep -q "GradleWrapperMain" gradlew.bat; then
  echo "gradlew.bat must be able to launch org.gradle.wrapper.GradleWrapperMain." >&2
  exit 1
fi
if ! grep -q "EnableDelayedExpansion" gradlew.bat; then
  echo "gradlew.bat must enable delayed expansion so checksum variables set inside blocks are evaluated correctly." >&2
  exit 1
fi
if grep -q "%ERRORLEVEL% EQU" gradlew.bat; then
  echo "gradlew.bat must not use parse-time %ERRORLEVEL% expansion inside parenthesized blocks; use if errorlevel or delayed expansion." >&2
  exit 1
fi

echo "[6/19] Checking build and security configuration guardrails"
python3 - <<'PYSEC'
from pathlib import Path
import re
import sys

errors=[]
wrapper=Path('gradle/wrapper/gradle-wrapper.properties').read_text()
if 'distributionSha256Sum=553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746' not in wrapper:
    errors.append('gradle-wrapper.properties must pin the official Gradle 9.5.0 binary distributionSha256Sum.')
if 'PMP_ALLOW_LOCAL_GRADLE_FALLBACK' not in Path('gradlew').read_text():
    errors.append('gradlew must not silently fall back to local Gradle; require PMP_ALLOW_LOCAL_GRADLE_FALLBACK=1.')
if 'EXPECTED_WRAPPER_JAR_SHA256=497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7' not in Path('gradlew').read_text():
    errors.append('gradlew must verify the committed Gradle wrapper JAR checksum.')
if '--gradle-distribution-sha256-sum' not in Path('scripts/bootstrap-gradle-wrapper.sh').read_text():
    errors.append('bootstrap-gradle-wrapper.sh must generate the wrapper with a pinned distribution checksum.')
if not Path('scripts/verify-build-environment.sh').exists():
    errors.append('scripts/verify-build-environment.sh is required for build readiness checks.')
else:
    verifier=Path('scripts/verify-build-environment.sh').read_text()
    for token in ('gradle-wrapper.jar', 'EXPECTED_WRAPPER_JAR_SHA256', 'ANDROID_HOME', 'ANDROID_SDK_ROOT'):
        if token not in verifier:
            errors.append(f'verify-build-environment.sh must check {token}.')
props=Path('gradle.properties').read_text()
if 'android.defaults.buildfeatures.buildconfig=true' in props:
    errors.append('Do not enable BuildConfig globally through android.defaults.buildfeatures.buildconfig=true; opt in per module.')
for module in ['app', 'core/geo', 'core/location', 'core/observability', 'core/offline', 'core/service', 'feature/map']:
    gradle_file=Path(module, 'build.gradle.kts')
    text=gradle_file.read_text()
    if 'compileOptions {' not in text or 'JavaVersion.VERSION_17' not in text:
        errors.append(f'{gradle_file}: Android modules must set Java 17 compileOptions explicitly.')
    if module == 'core/geo' and 'ndkVersion = "28.2.13676358"' not in text:
        errors.append('core/geo/build.gradle.kts must pin AGP 9 default NDK 28.2.13676358 for reproducible native builds.')
    if 'libs.plugins.kotlin.android' in text or 'org.jetbrains.kotlin.android' in text:
        errors.append(f'{gradle_file}: AGP 9 built-in Kotlin is enabled; do not apply the kotlin-android plugin.')
    if 'jvmToolchain(17)' in text:
        errors.append(f'{gradle_file}: Android modules must not use top-level kotlin.jvmToolchain with AGP 9 built-in Kotlin; android.compileOptions controls the Kotlin JVM target.')
root_gradle=Path('build.gradle.kts').read_text()
catalog=Path('gradle/libs.versions.toml').read_text()
props=Path('gradle.properties').read_text()
if 'libs.plugins.kotlin.android' in root_gradle or 'org.jetbrains.kotlin.android' in root_gradle:
    errors.append('Top-level build.gradle.kts must not declare kotlin-android when AGP 9 built-in Kotlin is enabled.')
if 'kotlin-android' in catalog or 'org.jetbrains.kotlin.android' in catalog:
    errors.append('gradle/libs.versions.toml must not define the removed kotlin-android plugin alias under AGP 9 built-in Kotlin.')
if 'android.builtInKotlin=false' in props:
    errors.append('gradle.properties must not opt out of AGP 9 built-in Kotlin.')
if 'classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")' not in root_gradle:
    errors.append('Top-level build.gradle.kts must pin Kotlin Gradle Plugin 2.4.0 for AGP 9 built-in Kotlin and Kotlin compiler plugins.')
if 'buildscript {' not in root_gradle or 'repositories {' not in root_gradle.split('dependencies {', 1)[0]:
    errors.append('Top-level build.gradle.kts buildscript must declare repositories before resolving the pinned Kotlin Gradle Plugin classpath.')
for repo in ('google()', 'mavenCentral()', 'gradlePluginPortal()'):
    if repo not in root_gradle.split('dependencies {', 1)[0]:
        errors.append(f'Top-level build.gradle.kts buildscript.repositories must include {repo}.')
ignore=Path('.gitignore').read_text()
for token in ('app/google-services.json', '*.jks', '*.keystore', '*.env', 'secrets.properties'):
    if token not in ignore:
        errors.append(f'.gitignore must exclude {token}.')
workflow=Path('.github/workflows/android.yml').read_text()
for token in ('permissions:', 'verify-build-environment.sh', 'wrapper-validation@v4', '--stacktrace'):
    if token not in workflow:
        errors.append(f'Android CI must include {token}.')
if 'ndk;28.2.13676358' not in workflow:
    errors.append('Android CI must install the same pinned NDK 28.2.13676358 used by core/geo.')
for path in [Path('app/google-services.json'), Path('secrets.properties'), Path('release-signing.properties')]:
    if path.exists():
        errors.append(f'{path} must not be committed.')
for path in Path('.').glob('**/*'):
    if not path.is_file():
        continue
    if any(part in {'.git', '.gradle', 'build'} for part in path.parts):
        continue
    if path.suffix.lower() in {'.jks', '.keystore', '.p12', '.pem', '.key'}:
        errors.append(f'signing/secret material must not be committed: {path}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Build and security configuration guardrails passed.')
PYSEC

echo "[7/19] Optional pure Kotlin compile checks"
if [[ "${RUN_KOTLINC_CHECKS:-0}" == "1" ]]; then
  echo "RUN_KOTLINC_CHECKS is disabled in this static verifier because standalone kotlinc does not model Android Gradle classpaths reliably. Use the Gradle test tasks in CI for compile-grade verification."
else
  echo "Skipping kotlinc checks. Android/Gradle test tasks remain the compile-grade verification path."
fi

echo "[8/19] Checking module dependency graph"
./scripts/verify-architecture.sh

echo "[9/19] Checking presentation and SOLID architecture guardrails"
if grep -R "lastActionMessage" feature/map/src/main/java >/dev/null 2>&1; then
  echo "Raw lastActionMessage is not allowed in feature:map main sources. Use MapUiMessage." >&2
  exit 1
fi
if grep -R "RoutingUiState\.Error(\"\|RoutingUiState\.Success(\"" feature/map/src/main/java >/dev/null 2>&1; then
  echo "RoutingUiState must carry typed MapUiMessage, not raw strings." >&2
  exit 1
fi
if grep -R "MapUiMessage\.Raw\|data class Raw" feature/map/src/main/java >/dev/null 2>&1; then
  echo "MapUiMessage.Raw is not allowed. Model every user-facing event as a typed message." >&2
  exit 1
fi
route_controller="feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapRouteController.kt"
if ! grep -q "catch (cancellation: CancellationException)" "$route_controller" || ! grep -q "telemetry.routeCalculationFailed(throwable)" "$route_controller"; then
  echo "MapRouteController must rethrow route coroutine cancellations and convert unexpected route application failures into typed UI/telemetry errors." >&2
  exit 1
fi
if grep -R "java.text.DecimalFormat" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation >/dev/null 2>&1; then
  echo "Presentation ViewModels must not use DecimalFormat. Keep formatting in i18n/formatters." >&2
  exit 1
fi
if grep -R "String\.format(Locale" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not format user-visible strings. Use typed MapUiMessage and formatters." >&2
  exit 1
fi
if grep -R "AppMonitor\|MonitorEvents\|MonitorTraces" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not know observability SDK/event details. Use MapFeatureTelemetry." >&2
  exit 1
fi
if grep -R "DisabledAppMonitor\|Default.*Repository\|GeoEngineProvider" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must receive configured dependencies; defaults/null objects belong in composition roots or tests." >&2
  exit 1
fi
if ! grep -R "interface MapCatalogRepository" core/mapdata/src/main/java >/dev/null 2>&1; then
  echo "core:mapdata must expose MapCatalogRepository abstraction." >&2
  exit 1
fi
if grep -R "val origin = when" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not own route-origin fallback policy. Use RouteOriginResolver." >&2
  exit 1
fi
if grep -R "lastProgressUpdateTimestampMillis" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not own progress throttling timestamp state. Use ProgressUpdateThrottle." >&2
  exit 1
fi
if grep -R "RouteProgressRepository\|calculateProgress(" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not calculate route progress directly. Use UpdateNavigationProgressUseCase." >&2
  exit 1
fi
if [[ ! -f "feature/map/src/main/java/com/msa/professionalmap/feature/map/domain/UpdateNavigationProgressUseCase.kt" ]]; then
  echo "UpdateNavigationProgressUseCase is required so navigation progress calculation stays outside MapViewModel." >&2
  exit 1
fi
if grep -R "LocationConfig\|trackingState.collect" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not own live-location state collection/configuration. Use MapLocationController." >&2
  exit 1
fi
if [[ ! -f "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapLocationController.kt" ]]; then
  echo "MapLocationController is required so GPS/location orchestration stays outside MapViewModel." >&2
  exit 1
fi
if grep -E "android\.Manifest|android\.provider\.Settings|PackageManager|ContextCompat" core/location/src/main/java/com/msa/professionalmap/core/location/AndroidFusedLocationRepository.kt >/dev/null 2>&1; then
  echo "AndroidFusedLocationRepository must delegate permission/provider details to focused collaborators." >&2
  exit 1
fi
if ! grep -R "AndroidLocationPermissionChecker" core/location/src/main/java/com/msa/professionalmap/core/location >/dev/null 2>&1; then
  echo "AndroidLocationPermissionChecker is required so permission policy stays outside AndroidFusedLocationRepository." >&2
  exit 1
fi
if ! grep -R "AndroidLocationProviderStateReader" core/location/src/main/java/com/msa/professionalmap/core/location >/dev/null 2>&1; then
  echo "AndroidLocationProviderStateReader is required so provider-state reads stay outside AndroidFusedLocationRepository." >&2
  exit 1
fi

service_controller="core/service/src/main/java/com/msa/professionalmap/core/service/data/AndroidNavigationServiceController.kt"
if ! grep -q "appContext.stopService" "$service_controller"; then
  echo "AndroidNavigationServiceController.stop must use stopService instead of starting a foreground service only to stop it; this avoids Android 12+ background FGS start restrictions during teardown." >&2
  exit 1
fi
if awk '/override fun stop\(\)/,/^    }/' "$service_controller" | grep -q "startForegroundService"; then
  echo "AndroidNavigationServiceController.stop must not call startForegroundService." >&2
  exit 1
fi
if grep -R "NavigationProgressInput\|maybeScheduleReroute\|serviceSnapshot(\|reroutingJob" feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt >/dev/null 2>&1; then
  echo "MapViewModel must not own active-navigation progress/service/reroute orchestration. Use MapNavigationController." >&2
  exit 1
fi
if [[ ! -f "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapNavigationController.kt" ]]; then
  echo "MapNavigationController is required so active-navigation orchestration stays outside MapViewModel." >&2
  exit 1
fi
if grep -R "\bMapError\b\|\bRoutePlan\b" core feature app >/dev/null 2>&1; then
  echo "Removed dead model types MapError/RoutePlan must not be reintroduced. Use typed domain-specific failures instead." >&2
  exit 1
fi

if grep -R "com.msa.professionalmap.core.offline.data" feature/map/src/main/java/com/msa/professionalmap/feature/map/domain >/dev/null 2>&1; then
  echo "Feature domain must not import core:offline data implementations. Keep offline planning policies in core.offline.domain." >&2
  exit 1
fi

if grep -R "nextInstruction\?\.text\|nextInstruction\.text" feature/map/src/main/java core >/dev/null 2>&1; then
  echo "Progress instructions must use NextInstruction.instruction; field name text does not exist." >&2
  exit 1
fi
if grep -R "NoOpTtsEngine\|NoOpLocationRepository\|NoOpRoutingRepository\|NoOpOfflineMapRepository\|NoOpOfflineDownloadManager\|NoOpNavigationServiceController\|NoOpLanguagePreferenceStore" app/src/main core feature/*/src/main >/dev/null 2>&1; then
  echo "Production sources must not keep test-only no-op repositories/controllers. Keep fakes in test sources or real fallbacks in composition roots." >&2
  exit 1
fi

echo "[10/19] Checking map bootstrap boundaries"
python3 - <<'PYBOOT'
from pathlib import Path
import sys
errors=[]
vm=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt').read_text()
for token in ('repository.styles(', 'repository.referenceRoutePoints(', 'repository.pois(', 'destinationPoint(route.first()', 'Reference route must contain'):
    if token in vm:
        errors.append(f'MapViewModel must not own initial catalog/bootstrap logic; found {token!r}. Use LoadInitialMapSceneUseCase.')
use_case=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/domain/LoadInitialMapSceneUseCase.kt')
if not use_case.exists():
    errors.append('LoadInitialMapSceneUseCase is required to keep startup/catalog validation outside MapViewModel.')
else:
    text=use_case.read_text()
    for token in ('MapCatalogRepository', 'GeoEngine', 'require(referenceRoute.size'):
        if token not in text:
            errors.append(f'LoadInitialMapSceneUseCase must own startup dependency/validation token: {token}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Map bootstrap boundary checks passed.')
PYBOOT

echo "[11/19] Checking route geometry boundaries"
python3 - <<'PYGEOM'
from pathlib import Path
import sys
errors=[]
vm_path=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt')
vm=vm_path.read_text()
for token in (
    'import com.msa.professionalmap.core.geo.GeoEngine',
    'geoEngine',
    '.simplifyRoute(',
    '.routeMetrics(',
    '.distanceMeters(',
):
    if token in vm:
        errors.append(f'MapViewModel must not call low-level geometry policy directly; found {token!r}. Use BuildRoutePresentationUseCase.')
use_case=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/domain/BuildRoutePresentationUseCase.kt')
if not use_case.exists():
    errors.append('BuildRoutePresentationUseCase is required to keep route presentation geometry outside MapViewModel.')
else:
    text=use_case.read_text()
    for token in ('GeoEngine', 'RoutePresentation', 'simplifyRoute', 'routeMetrics', 'distanceFromRouteStartKm'):
        if token not in text:
            errors.append(f'BuildRoutePresentationUseCase must own route presentation geometry token: {token}')
use_cases=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/domain/MapUseCases.kt').read_text()
if 'buildRoutePresentation: BuildRoutePresentationUseCase' not in use_cases:
    errors.append('MapUseCases must expose BuildRoutePresentationUseCase through the explicit use-case graph.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Route geometry boundary checks passed.')
PYGEOM

echo "[12/19] Checking production source cleanup"
python3 - <<'PY'
from pathlib import Path
import re, sys
errors=[]
for base in [Path('app/src/main'), Path('core'), Path('feature')]:
    for path in base.glob('**/*'):
        if not path.is_file() or path.suffix not in {'.kt','.kts','.xml'}:
            continue
        if '/src/test/' in str(path) or '/src/androidTest/' in str(path):
            continue
        text=path.read_text(encoding='utf-8', errors='ignore')
        if re.search(r'\bDemo\b|\bdemo\b|placeholder|Placeholder|sample app|sample point|stub', text):
            errors.append(f'{path}: demo/sample/placeholder wording is not allowed in production source.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Production source cleanup checks passed.')
PY

echo "[13/19] Checking test-only fakes stay out of production"
python3 - <<'PY'
from pathlib import Path
import sys
forbidden = (
    'DisabledTtsEngine',
    'DisabledLocationRepository',
    'DisabledRoutingRepository',
    'DisabledOfflineMapRepository',
    'DisabledOfflineDownloadManager',
    'DisabledNavigationServiceController',
    'DisabledLanguagePreferenceStore',
)
errors=[]
for base in [Path('app/src/main'), Path('core'), Path('feature')]:
    for path in base.glob('**/*'):
        if not path.is_file() or path.suffix not in {'.kt', '.kts', '.xml'}:
            continue
        if '/src/test/' in str(path) or '/src/androidTest/' in str(path):
            continue
        text=path.read_text(encoding='utf-8', errors='ignore')
        for token in forbidden:
            if token in text:
                errors.append(f'{path}: test-only fallback {token} is not allowed in production source.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Test-only fallback guardrails passed.')
PY

echo "[14/19] Checking production hardening guardrails"
python3 - <<'PY2'
from pathlib import Path
import sys
errors=[]
manifest=Path('app/src/main/AndroidManifest.xml').read_text()
if 'android:allowBackup="false"' not in manifest:
    errors.append('app manifest must disable allowBackup for release hardening.')
if 'android:usesCleartextTraffic="false"' not in manifest:
    errors.append('app manifest must disable cleartext traffic.')
service_files=[p for p in Path('core/service/src/main/java').glob('**/*.kt') if p.name != 'NavigationNotificationText.kt']
for path in service_files:
    text=path.read_text()
    for raw in ['Navigating to ', 'Navigation paused.', 'Navigation stopped.', 'Pause",', 'Stop",']:
        if raw in text:
            errors.append(f'{path}: hardcoded notification/service copy found: {raw}')
routing_config=Path('core/routing/src/main/java/com/msa/professionalmap/core/routing/OsrmRoutingConfig.kt').read_text()
if 'allowCleartextTraffic' not in routing_config or 'https://' not in routing_config:
    errors.append('OsrmRoutingConfig must enforce HTTPS by default with explicit cleartext opt-in.')
feature_gradle=Path('feature/map/build.gradle.kts').read_text()
if 'OSRM_BASE_URL' not in feature_gradle or 'ALLOW_CLEARTEXT_ROUTING' not in feature_gradle:
    errors.append('feature:map must expose routing endpoint BuildConfig fields for build-type injection.')
required_docs=['README.md','DEV_SETUP_FA.md','RELEASE_DEPLOY_FA.md','DEV_PRD_WINDOWS_LINUX_FA.md']
markdown_files=sorted(str(path) for path in Path('.').glob('**/*.md') if '.gradle' not in path.parts and 'build' not in path.parts)
if markdown_files != sorted(required_docs):
    errors.append('documentation policy requires exactly these Markdown files in project root: ' + ', '.join(required_docs) + '; found: ' + ', '.join(markdown_files))
for doc in required_docs:
    if not Path(doc).exists():
        errors.append(f'missing required project documentation: {doc}')
obsolete_docs_dir=Path('docs')
if obsolete_docs_dir.exists() and any(obsolete_docs_dir.iterdir()):
    errors.append('docs directory must be empty or absent after consolidating documentation into the four approved Markdown files.')

offline_worker=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/OfflineDownloadWorker.kt').read_text()
if 'catch (throwable: CancellationException)' not in offline_worker or 'throw throwable' not in offline_worker:
    errors.append('OfflineDownloadWorker must rethrow coroutine CancellationException instead of reporting cancellation as failure.')
offline_manager=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/AndroidWorkManagerOfflineDownloadManager.kt').read_text()
if 'isStaleWorkInfo(clientId, info.id)' not in offline_manager:
    errors.append('AndroidWorkManagerOfflineDownloadManager must ignore stale WorkInfo emissions for replaced clientIds.')
if 'ignoredFinishedWorkIds' not in offline_manager:
    errors.append('AndroidWorkManagerOfflineDownloadManager must remember finished WorkManager IDs so persisted observation does not reprocess completed work every polling tick.')
if 'ignoredFinishedWorkIds.contains(info.id)' not in offline_manager:
    errors.append('AndroidWorkManagerOfflineDownloadManager persisted observer must skip already handled finished WorkInfo records.')
if 'ignoredFinishedWorkIds.add(info.id)' not in offline_manager:
    errors.append('AndroidWorkManagerOfflineDownloadManager must mark finished WorkInfo IDs as handled before removing known work IDs.')
if 'getWorkInfoByIdFlow' in offline_manager or 'getWorkInfosByTagFlow' in offline_manager:
    errors.append('AndroidWorkManagerOfflineDownloadManager must not depend on WorkManager Flow extensions that are not resolved in this AGP 9 build.')
maplibre_await=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/MapLibreOfflineAwait.kt').read_text()
required_maplibre_signatures=[
    'override fun onCreate(offlineRegion: OfflineRegion)',
    'override fun onError(error: String)',
    'override fun onList(offlineRegions: Array<OfflineRegion>?)',
    'override fun onStatus(status: OfflineRegionStatus?)',
    'override fun onError(error: String?)',
]
for signature in required_maplibre_signatures:
    if signature not in maplibre_await:
        errors.append('MapLibreOfflineAwait must keep MapLibre Android offline callback signature: ' + signature)
if 'override fun onList(offlineRegions: Array<OfflineRegion>)' in maplibre_await:
    errors.append('MapLibreOfflineAwait listOfflineRegions callback must accept nullable OfflineRegion arrays.')
offline_poller=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/OfflineDownloadProgressPoller.kt').read_text()
if 'withTimeout<Result>(timeoutMillis)' not in offline_poller:
    errors.append('OfflineDownloadProgressPoller must explicitly type withTimeout<Result> to avoid Unit inference.')
offline_controller=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapOfflineController.kt').read_text()
if 'catch (cancellation: CancellationException)' not in offline_controller or 'throw cancellation' not in offline_controller:
    errors.append('MapOfflineController must rethrow coroutine CancellationException while enqueueing offline work.')
if 'finally {' not in offline_controller or 'trace.stop()' not in offline_controller:
    errors.append('MapOfflineController must stop offline download traces in finally blocks.')
location_repo=Path('core/location/src/main/java/com/msa/professionalmap/core/location/AndroidFusedLocationRepository.kt').read_text()
if 'AndroidProviderChangeMonitor' not in location_repo or 'LocationPermissionReader' not in location_repo:
    errors.append('AndroidFusedLocationRepository must depend on injectable platform readers and a receiver lifecycle collaborator.')
offline_map_repo=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/AndroidMapLibreOfflineMapRepository.kt').read_text()
if offline_map_repo.count('manager.clearAmbientCacheAwait()') != 1:
    errors.append('AndroidMapLibreOfflineMapRepository must clear ambient cache exactly once per clearAmbientCache() call.')
service=Path('core/service/src/main/java/com/msa/professionalmap/core/service/data/ForegroundNavigationService.kt').read_text()
if 'START_STICKY' in service:
    errors.append('ForegroundNavigationService must not use START_STICKY; null restarts must not silently restart navigation.')
if 'fun from(action: String?): Action?' not in service or 'return START_NOT_STICKY' not in service:
    errors.append('ForegroundNavigationService must fail closed for null or unknown service actions.')
action_runner=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapOfflineActionRunner.kt')
if not action_runner.exists():
    errors.append('MapOfflineActionRunner is required for safe fire-and-forget offline presentation commands.')
else:
    runner_text=action_runner.read_text()
    if 'catch (cancellation: CancellationException)' not in runner_text or 'throw cancellation' not in runner_text:
        errors.append('MapOfflineActionRunner must rethrow CancellationException.')
    if 'telemetry.record(telemetryArea, throwable)' not in runner_text:
        errors.append('MapOfflineActionRunner must report non-cancellation failures to telemetry.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Production hardening guardrails passed.')
PY2


echo "[15/19] Checking cleaned delivery scope"
python3 - <<'PY3'
from pathlib import Path
import sys
errors=[]
if Path('feature/auto').exists():
    errors.append('feature:auto was removed from this focused delivery and must not be reintroduced as an unfinished scaffold.')
settings=Path('settings.gradle.kts').read_text()
app_gradle=Path('app/build.gradle.kts').read_text()
catalog=Path('gradle/libs.versions.toml').read_text()
if 'feature:auto' in settings or 'feature:auto' in app_gradle:
    errors.append('app/settings must not depend on feature:auto in the clean delivery scope.')
scanned_text='\n'.join(
    p.read_text(encoding='utf-8', errors='ignore')
    for p in Path('.').glob('**/*')
    if p.is_file() and p.suffix in {'.kt', '.xml', '.md', '.kts', '.toml'}
)
for token in ('androidx-car-app', 'carApp', 'CarNavigationService', 'CarMapScreen', 'CarVoiceController'):
    if token in catalog or token in scanned_text:
        errors.append(f'unfinished automotive scaffold token found: {token}')
if Path('core/routing/src/main/java/com/msa/professionalmap/core/routing/DirectRoutingRepository.kt').exists():
    errors.append('DirectRoutingRepository is test-only behavior and must not live in production routing sources.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Clean delivery scope checks passed.')
PY3

echo "[16/19] Checking source-size guardrails"
check_line_limit() {
  local file="$1"
  local limit="$2"
  if [[ ! -f "$file" ]]; then
    echo "missing expected source file: $file" >&2
    exit 1
  fi
  local count
  count=$(wc -l < "$file" | tr -d ' ')
  if (( count > limit )); then
    echo "$file has $count lines; limit is $limit. Split responsibilities before adding more code." >&2
    exit 1
  fi
}
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapScreen.kt" 320
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanel.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapPanels.kt" 40
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapLocationRoutingPanels.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapNavigationGuidancePanels.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapStatusContent.kt" 140
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapHudOverlay.kt" 120
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiKit.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiTiles.kt" 120
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapRouteFocusCard.kt" 140
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapSystemStatusStrip.kt" 120
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiLayout.kt" 80
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapQuickActions.kt" 100
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanelHeader.kt" 170
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapOfflinePanel.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapViewModel.kt" 260
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapRouteController.kt" 240
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapNavigationController.kt" 230
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapGuidanceController.kt" 180
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapOfflineController.kt" 170
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapLocationController.kt" 130
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/presentation/MapPresentationMappers.kt" 120
check_line_limit "core/location/src/main/java/com/msa/professionalmap/core/location/AndroidFusedLocationRepository.kt" 230
check_line_limit "core/location/src/main/java/com/msa/professionalmap/core/location/AndroidLocationPermissionChecker.kt" 80
check_line_limit "core/location/src/main/java/com/msa/professionalmap/core/location/AndroidLocationProviderStateReader.kt" 80
check_line_limit "core/location/src/main/java/com/msa/professionalmap/core/location/AndroidLocationRequestMapper.kt" 80
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/i18n/MapStrings.kt" 240
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/i18n/MapStringCatalog.kt" 220
check_line_limit "feature/map/src/main/java/com/msa/professionalmap/feature/map/i18n/MapMessageTextFormatter.kt" 170
check_line_limit "core/offline/src/main/java/com/msa/professionalmap/core/offline/data/AndroidMapLibreOfflineMapRepository.kt" 260
check_line_limit "core/offline/src/main/java/com/msa/professionalmap/core/offline/data/MapLibreOfflineAwait.kt" 120
check_line_limit "core/offline/src/main/java/com/msa/professionalmap/core/offline/data/MapLibreOfflineMetadata.kt" 80
check_line_limit "core/offline/src/main/java/com/msa/professionalmap/core/offline/data/MapLibreOfflineRegionMapper.kt" 80

python3 - <<'PYSIZE'
from pathlib import Path
import sys
errors=[]
for path in list(Path('app/src/main').glob('**/*.kt')) + list(Path('core').glob('**/src/main/**/*.kt')) + list(Path('feature').glob('**/src/main/**/*.kt')):
    if path.name.endswith('Generated.kt'):
        continue
    count=sum(1 for _ in path.open(encoding='utf-8', errors='ignore'))
    if count > 300:
        errors.append(f'{path} has {count} lines; production Kotlin files must stay below 300 lines.')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
PYSIZE
echo "Source-size guardrails passed."

echo "[17/19] Checking UI import hygiene"
for file in \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanel.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapPanels.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapLocationRoutingPanels.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapNavigationGuidancePanels.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapStatusContent.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapHudOverlay.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiKit.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiTiles.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapRouteFocusCard.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapSystemStatusStrip.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiLayout.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapQuickActions.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanelHeader.kt \
  feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapOfflinePanel.kt; do
  for forbidden in \
    "import android.Manifest" \
    "import android.os.Build" \
    "import androidx.lifecycle.viewmodel.compose.viewModel" \
    "import com.google.accompanist.permissions" \
    "import com.msa.professionalmap.feature.map.di.rememberDefaultMapFeatureDependencies" \
    "import com.msa.professionalmap.feature.map.presentation.MapViewModel" \
    "import com.msa.professionalmap.feature.map.presentation.MapViewModelFactory"; do
    if grep -F "$forbidden" "$file" >/dev/null 2>&1; then
      echo "$file: screen-level import leaked into leaf UI component: $forbidden" >&2
      exit 1
    fi
  done
done
echo "UI import hygiene checks passed."


echo "[18/20] Checking professional UI guardrails"
python3 - <<'PYUI'
from pathlib import Path
import sys
errors=[]
required_files=[
    Path('app/src/main/java/com/msa/professionalmap/ui/theme/Theme.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiKit.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiTiles.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapRouteFocusCard.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapSystemStatusStrip.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiLayout.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapQuickActions.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanelHeader.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapHudOverlay.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapScreenContent.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanel.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapStatusContent.kt'),
]
for path in required_files:
    if not path.exists():
        errors.append(f'missing professional UI file: {path}')

theme=Path('app/src/main/java/com/msa/professionalmap/ui/theme/Theme.kt').read_text()
for token in ('dynamicLightColorScheme', 'dynamicDarkColorScheme', 'primaryContainer', 'surfaceVariant', 'AppShapes'):
    if token not in theme:
        errors.append(f'ProfessionalMapTheme must keep Material 3 expressive token: {token}')
ui_kit=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiKit.kt').read_text()
for token in ('MapGlassPanel', 'MapSectionCard', 'StatusPill', 'MapUiTokens', 'ActionShape', 'semantics'):
    if token not in ui_kit:
        errors.append(f'MapUiKit must provide reusable professional UI primitive: {token}')
ui_tiles=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiTiles.kt').read_text()
for token in ('MapPanelHandle', 'MapKeyValueTile', 'MapStatusTile', 'TileShape'):
    if token not in ui_tiles:
        errors.append(f'MapUiTiles must keep polished panel/tile primitive: {token}')
screen=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapScreen.kt').read_text()
content=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapScreenContent.kt').read_text()
for token in ('CenterAlignedTopAppBar', 'ReadyMapContent'):
    if token not in screen:
        errors.append(f'MapScreen must keep professional map shell token: {token}')
for token in ('BoxWithConstraints', 'MapHudOverlay', 'navigationBarsPadding', 'MapUiLayout.CompactPanelMaxWidth', 'MapUiLayout.ExpandedPanelMinWidth', 'MapUiLayout.ExpandedBreakpoint', 'Brush.verticalGradient', 'matchParentSize'):
    if token not in content:
        errors.append(f'MapScreenContent must keep adaptive professional map shell token: {token}')
if 'statusBarsPadding' in screen + content:
    errors.append('MapScreen must not double-apply statusBarsPadding on Material3 TopAppBar.')
hud=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapHudOverlay.kt').read_text()
for token in ('onRequestLocationPermission', 'onStartLocation', 'onCalculateRoute', 'onStartNavigation', 'onDownloadOfflineRoute', 'MapQuickActionBar', 'MapPrimaryAction', 'MapSecondaryAction', 'MapRouteFocusCard', 'MapSystemStatusStrip'):
    if token not in hud:
        errors.append(f'MapHudOverlay must keep executive quick action token: {token}')
route_focus=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapRouteFocusCard.kt').read_text()
for token in ('MapRouteFocusCard', 'LinearProgressIndicator', 'selectedRouteSummary', 'progressOrNull', 'String.format(Locale.US'):
    if token not in route_focus:
        errors.append(f'MapRouteFocusCard must keep premium route focus token: {token}')
status_strip=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapSystemStatusStrip.kt').read_text()
for token in ('MapSystemStatusStrip', 'MapStatusTile', 'permissionLabel', 'routingLabel', 'offlineSummary'):
    if token not in status_strip:
        errors.append(f'MapSystemStatusStrip must keep executive system status token: {token}')
layout=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapUiLayout.kt').read_text()
for token in ('ExpandedBreakpoint', 'CompactPanelMaxWidth', 'ExpandedPanelMaxWidth', 'HudMaxWidth', 'MinimumTouchTarget', 'CompactActionMinWidth', 'SignalBannerMinHeight'):
    if token not in layout:
        errors.append(f'MapUiLayout must centralize adaptive UI token: {token}')

quick_actions=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapQuickActions.kt').read_text()
for token in ('MapQuickActionBar', 'MapPrimaryAction', 'MapSecondaryAction', 'heightIn(min = MapUiLayout.MinimumTouchTarget)', 'widthIn(min = MapUiLayout.CompactActionMinWidth)'):
    if token not in quick_actions:
        errors.append(f'MapQuickActions must keep accessible action-system token: {token}')
header=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanelHeader.kt').read_text()
for token in ('MapControlPanelHeader', 'MapSignalBanner', 'MapSignalTone', 'MapSystemStatusStrip', 'heading()', 'SignalBannerMinHeight'):
    if token not in header:
        errors.append(f'MapControlPanelHeader must keep executive panel header token: {token}')
control=Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapControlPanel.kt').read_text()
for token in ('MapGlassPanel', 'MapPanelHandle', 'MapControlPanelHeader', 'heightIn(max = maxPanelHeight)', 'verticalScroll', 'MapPrimaryAction', 'MapSecondaryAction'):
    if token not in control:
        errors.append(f'MapControlPanel must keep polished adaptive control behavior: {token}')
if 'OutlinedButton(onClick = onDecreaseSimplification)' in control or 'Button(onClick = onIncreaseSimplification)' in control:
    errors.append('MapControlPanel footer actions must use the shared accessible MapQuickActions components, not raw Button/OutlinedButton calls.')
work_manager_observer=Path('core/offline/src/main/java/com/msa/professionalmap/core/offline/data/AndroidWorkManagerOfflineDownloadManager.kt').read_text()
finished_block = '''if (info.state.isFinished) {
                            removeKnownWorkId(clientId, info.id)
                            observerJobs.remove(clientId)?.cancel()'''
if finished_block in work_manager_observer:
    errors.append('AndroidWorkManagerOfflineDownloadManager must not self-cancel an observer job after finished work; remove the map entry and return instead.')

for action_panel in (
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapLocationRoutingPanels.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapNavigationGuidancePanels.kt'),
    Path('feature/map/src/main/java/com/msa/professionalmap/feature/map/ui/MapOfflinePanel.kt'),
):
    text = action_panel.read_text()
    for forbidden in ('Button(', 'OutlinedButton('):
        if forbidden in text:
            errors.append(f'{action_panel} must route panel actions through MapPrimaryAction/MapSecondaryAction, not raw {forbidden}')
    for token in ('MapPrimaryAction', 'MapSecondaryAction'):
        if token not in text:
            errors.append(f'{action_panel} must keep shared action-system token: {token}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Professional UI guardrails passed.')
PYUI

echo "[19/20] Checking final documentation policy"
python3 - <<'PYDOC'
from pathlib import Path
import sys
expected=['README.md','DEV_SETUP_FA.md','RELEASE_DEPLOY_FA.md','DEV_PRD_WINDOWS_LINUX_FA.md']
errors=[]
actual=sorted(str(path) for path in Path('.').glob('**/*.md') if '.gradle' not in path.parts and 'build' not in path.parts)
if actual != sorted(expected):
    errors.append('expected exactly four root Markdown files: ' + ', '.join(expected) + '; found: ' + ', '.join(actual))
checks={
    'README.md': ['ProfessionalMapPro', 'Architecture', 'Build commands', 'gradle-wrapper.jar', 'MapNavigationController', 'UpdateNavigationProgressUseCase'],
    'DEV_SETUP_FA.md': ['راه‌اندازی', 'پیش‌نیازها', 'اجرای تست‌ها', 'gradle/wrapper/gradle-wrapper.jar'],
    'RELEASE_DEPLOY_FA.md': ['Release', 'Security checklist', 'Rollback plan', 'PMP_RELEASE_STORE_FILE'],
    'DEV_PRD_WINDOWS_LINUX_FA.md': ['DEV', 'PRD', 'Windows PowerShell', 'Linux Bash', 'systemd'],
}
for file_name, tokens in checks.items():
    path=Path(file_name)
    text=path.read_text(encoding='utf-8') if path.exists() else ''
    for token in tokens:
        if token not in text:
            errors.append(f'{file_name}: missing required documentation token: {token}')
for path in expected:
    text=Path(path).read_text(encoding='utf-8')
    forbidden=('TODO implement later', '// ... existing code', '// rest of code')
    for token in forbidden:
        if token in text:
            errors.append(f'{path}: documentation must not contain placeholder token {token}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Final four-file documentation policy checks passed.')
PYDOC

echo "[20/20] Checking test-suite guardrails"
for required_test_file in \
  core/routing/src/test/java/com/msa/professionalmap/core/routing/CachedRoutingRepositoryTest.kt \
  feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/CalculateRouteUseCaseTest.kt \
  feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/ApplyRouteCalculationOutcomeUseCaseTest.kt \
  feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/StartNavigationUseCaseTest.kt \
  feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/UpdateNavigationProgressUseCaseTest.kt \
  feature/map/src/test/java/com/msa/professionalmap/feature/map/MapUiStateTest.kt \
  core/service/src/test/java/com/msa/professionalmap/core/service/NavigationServiceSnapshotTest.kt; do
  if [[ ! -f "$required_test_file" ]]; then
    echo "missing required test file: $required_test_file" >&2
    exit 1
  fi
done
TEST_FILE_COUNT=$(find . -path "*/src/test/*.kt" -o -path "*/src/test/**/*.kt" | wc -l | tr -d " ")
if [[ "$TEST_FILE_COUNT" -lt 35 ]]; then
  echo "expected at least 35 Kotlin test files, found $TEST_FILE_COUNT" >&2
  exit 1
fi
for task in \
  ":core:offline:testDebugUnitTest" \
  ":feature:map:testDebugUnitTest" \
  ":app:lintDebug"; do
  if ! grep -F "$task" .github/workflows/android.yml >/dev/null 2>&1; then
    echo "Android CI must run task $task" >&2
    exit 1
  fi
done
echo "Test-suite guardrails passed with $TEST_FILE_COUNT Kotlin test files."

echo "Static verification completed successfully."

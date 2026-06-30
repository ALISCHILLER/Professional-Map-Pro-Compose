#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

python3 - <<'PY'
from pathlib import Path
import sys

errors=[]
required_files={
    'core/routing/src/test/java/com/msa/professionalmap/core/routing/CachedRoutingRepositoryTest.kt': [
        'returns cached result for identical request inside ttl',
        'does not cache provider failures',
        'propagates cancellation without caching it',
    ],
    'feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/CalculateRouteUseCaseTest.kt': [
        'propagates cancellation instead of converting it to fallback',
        'forwards routing request options to repository',
        'RouteNavigationPolicy.PreviewOnly',
    ],
    'feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/ApplyRouteCalculationOutcomeUseCaseTest.kt': [
        'rejects preview fallback while active navigation is rerouting',
        'FallbackPreviewApplied',
        'ProviderFailedDuringActiveNavigation',
    ],
    'feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/StartNavigationUseCaseTest.kt': [
        'rejects preview-only fallback routes',
        'MissingCurrentLocation',
        'Ready',
    ],
    'feature/map/src/test/java/com/msa/professionalmap/feature/map/domain/UpdateNavigationProgressUseCaseTest.kt': [
        'execute rejects preview-only route before calculating progress',
        'execute throttles rapid location updates before calculating progress',
        'reset clears throttling state for the next location update',
    ],
    'feature/map/src/test/java/com/msa/professionalmap/feature/map/MapUiStateTest.kt': [
        'canStartNavigation_isFalseForPreviewOnlySelectedRoute',
        'canStartNavigation_isTrueForNavigableSelectedRouteWithLocation',
        'canStartNavigation_allowsLocalReferenceRouteWhenNoAlternativeIsSelected',
    ],
    'core/service/src/test/java/com/msa/professionalmap/core/service/NavigationServiceSnapshotTest.kt': [
        'publicNotificationCopyHidesRouteDetails',
        'Open the app to view route details.',
    ],
}

for file_name, tokens in required_files.items():
    path=Path(file_name)
    if not path.exists():
        errors.append(f'missing required test file: {file_name}')
        continue
    text=path.read_text(encoding='utf-8')
    for token in tokens:
        if token not in text:
            errors.append(f'{file_name}: missing required test coverage token: {token}')

test_files=list(Path('.').glob('**/src/test/**/*.kt'))
if len(test_files) < 35:
    errors.append(f'expected at least 35 Kotlin test files, found {len(test_files)}')

for path in test_files:
    text=path.read_text(encoding='utf-8', errors='ignore')
    if 'TODO implement later' in text or '// ... existing code' in text or '// rest of code' in text:
        errors.append(f'{path}: tests must not contain placeholders')

workflow=Path('.github/workflows/android.yml').read_text(encoding='utf-8')
for task in (
    ':core:model:test',
    ':core:routing:test',
    ':core:progress:test',
    ':core:guidance:test',
    ':core:geo:testDebugUnitTest',
    ':core:location:testDebugUnitTest',
    ':core:offline:testDebugUnitTest',
    ':core:service:testDebugUnitTest',
    ':feature:map:testDebugUnitTest',
):
    if task not in workflow:
        errors.append(f'Android CI must run test task {task}')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print(f'Test-suite guardrails passed with {len(test_files)} Kotlin test files.')
PY

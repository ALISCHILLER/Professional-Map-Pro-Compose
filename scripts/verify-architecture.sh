#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

python3 - <<'PY'
from pathlib import Path
import re
import sys

allowed = {
    ':app': {':feature:map', ':core:observability'},
    ':benchmark': {':app'},
    ':feature:map': {
        ':core:model', ':core:geo', ':core:location', ':core:mapdata', ':core:routing',
        ':core:progress', ':core:guidance', ':core:offline', ':core:service', ':core:observability',
    },
    ':core:geo': {':core:model'},
    ':core:location': {':core:model'},
    ':core:mapdata': {':core:model'},
    ':core:routing': {':core:model'},
    ':core:progress': {':core:model'},
    ':core:guidance': {':core:model', ':core:progress'},
    ':core:offline': {':core:model'},
    ':core:service': {':core:model', ':core:location', ':core:progress', ':core:routing', ':core:guidance'},
    ':core:observability': set(),
    ':core:model': set(),
}

module_paths = {
    ':app': Path('app/build.gradle.kts'),
    ':benchmark': Path('benchmark/build.gradle.kts'),
    ':feature:map': Path('feature/map/build.gradle.kts'),
    ':core:model': Path('core/model/build.gradle.kts'),
    ':core:geo': Path('core/geo/build.gradle.kts'),
    ':core:location': Path('core/location/build.gradle.kts'),
    ':core:mapdata': Path('core/mapdata/build.gradle.kts'),
    ':core:routing': Path('core/routing/build.gradle.kts'),
    ':core:progress': Path('core/progress/build.gradle.kts'),
    ':core:guidance': Path('core/guidance/build.gradle.kts'),
    ':core:offline': Path('core/offline/build.gradle.kts'),
    ':core:service': Path('core/service/build.gradle.kts'),
    ':core:observability': Path('core/observability/build.gradle.kts'),
}

errors=[]
pattern=re.compile(r'project\("([^"]+)"\)')
for module, gradle_file in module_paths.items():
    if not gradle_file.exists():
        errors.append(f'{module}: missing {gradle_file}')
        continue
    deps=set(pattern.findall(gradle_file.read_text()))
    unexpected=deps-allowed[module]
    if unexpected:
        errors.append(f'{module}: unexpected project dependencies {sorted(unexpected)}')
    if module.startswith(':core:'):
        illegal=[dep for dep in deps if dep.startswith(':feature') or dep == ':app']
        if illegal:
            errors.append(f'{module}: core modules must not depend on app/feature modules: {illegal}')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('Architecture dependency graph check passed.')
PY

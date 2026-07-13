#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_VERSION="9.5.1"
DISTRIBUTION_SHA256="bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f"
WRAPPER_JAR_SHA256="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"

if ! command -v gradle >/dev/null 2>&1; then
  cat >&2 <<MSG
Gradle is required to generate a real wrapper.
Install Gradle ${GRADLE_VERSION} or use Android Studio sync first, then rerun this script.
MSG
  exit 127
fi

gradle wrapper \
  --gradle-version "$GRADLE_VERSION" \
  --distribution-type bin \
  --gradle-distribution-sha256-sum "$DISTRIBUTION_SHA256"

chmod +x gradlew

if [[ ! -f gradle/wrapper/gradle-wrapper.jar ]]; then
  echo "gradle/wrapper/gradle-wrapper.jar was not generated." >&2
  exit 1
fi

ACTUAL_WRAPPER_SHA256="$(sha256sum gradle/wrapper/gradle-wrapper.jar | awk '{print $1}')"
if [[ "$ACTUAL_WRAPPER_SHA256" != "$WRAPPER_JAR_SHA256" ]]; then
  echo "Generated wrapper JAR checksum mismatch." >&2
  echo "expected: $WRAPPER_JAR_SHA256" >&2
  echo "actual:   $ACTUAL_WRAPPER_SHA256" >&2
  exit 1
fi

./scripts/verify-build-environment.sh

echo "Real Gradle wrapper generated and verified. Commit gradle/wrapper/gradle-wrapper.jar with the wrapper scripts."

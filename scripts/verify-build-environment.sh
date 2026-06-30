#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

EXPECTED_GRADLE_VERSION="9.5.0"
EXPECTED_DISTRIBUTION_SHA256="553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746"
EXPECTED_WRAPPER_JAR_SHA256="497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
WRAPPER_PROPERTIES="gradle/wrapper/gradle-wrapper.properties"
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

fail() {
  echo "Build environment verification failed: $*" >&2
  exit 1
}

[[ -f "$WRAPPER_PROPERTIES" ]] || fail "$WRAPPER_PROPERTIES is missing."

grep -Fq "distributionUrl=https\\://services.gradle.org/distributions/gradle-${EXPECTED_GRADLE_VERSION}-bin.zip" "$WRAPPER_PROPERTIES" || \
  fail "Gradle distributionUrl must pin Gradle ${EXPECTED_GRADLE_VERSION}."

grep -q "distributionSha256Sum=${EXPECTED_DISTRIBUTION_SHA256}" "$WRAPPER_PROPERTIES" || \
  fail "Gradle distributionSha256Sum is missing or does not match the official Gradle ${EXPECTED_GRADLE_VERSION} binary checksum."

if [[ ! -f "$WRAPPER_JAR" ]]; then
  fail "$WRAPPER_JAR is missing. Generate it with ./scripts/bootstrap-gradle-wrapper.sh and commit it before claiming build-ready status."
fi

ACTUAL_WRAPPER_SHA256="$(sha256sum "$WRAPPER_JAR" | awk '{print $1}')"
if [[ "$ACTUAL_WRAPPER_SHA256" != "$EXPECTED_WRAPPER_JAR_SHA256" ]]; then
  fail "$WRAPPER_JAR checksum mismatch. Expected ${EXPECTED_WRAPPER_JAR_SHA256}, got ${ACTUAL_WRAPPER_SHA256}."
fi

JAVA_VERSION_OUTPUT="$(java -version 2>&1 | head -n 1 || true)"
if [[ -z "$JAVA_VERSION_OUTPUT" ]]; then
  fail "Java is required and was not found on PATH."
fi
if [[ "$JAVA_VERSION_OUTPUT" != *'17.'* && "$JAVA_VERSION_OUTPUT" != *'21.'* ]]; then
  fail "Use JDK 17 or 21 for this project. Current java -version: ${JAVA_VERSION_OUTPUT}"
fi

if [[ ! -n "${ANDROID_HOME:-}" && ! -n "${ANDROID_SDK_ROOT:-}" && ! -f "local.properties" ]]; then
  fail "Android SDK was not detected. Set ANDROID_HOME/ANDROID_SDK_ROOT or create local.properties from local.properties.example."
fi

if [[ -f app/google-services.json ]]; then
  if grep -E '"api_key"[[:space:]]*:[[:space:]]*"AIza[^"]+"' app/google-services.json >/dev/null 2>&1; then
    echo "Firebase config detected locally. This file is ignored by Git and must not be committed."
  fi
fi

echo "Build environment verification passed."

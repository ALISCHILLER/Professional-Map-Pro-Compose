#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_PATH="${1:-}"
CHECKSUM_PATH="${2:-}"

if [[ -z "$ARTIFACT_PATH" ]]; then
  echo "Usage: $0 <artifact.zip> [artifact.zip.sha256]" >&2
  exit 64
fi
if [[ ! -f "$ARTIFACT_PATH" ]]; then
  echo "Artifact does not exist: $ARTIFACT_PATH" >&2
  exit 66
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

unzip -t "$ARTIFACT_PATH" >/dev/null
unzip -q "$ARTIFACT_PATH" -d "$TMP_DIR"

ROOT_COUNT="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"
if [[ "$ROOT_COUNT" != "1" ]]; then
  echo "Artifact must contain exactly one top-level project directory." >&2
  exit 1
fi
PROJECT_ROOT="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [[ "$(basename "$PROJECT_ROOT")" != "ProfessionalMapPro" ]]; then
  echo "Artifact root directory must be ProfessionalMapPro." >&2
  exit 1
fi
if find "$PROJECT_ROOT" -name .git -type d | grep -q .; then
  echo "Artifact must not contain .git directories." >&2
  exit 1
fi
if find "$PROJECT_ROOT" -path '*/build/*' -type f | grep -q .; then
  echo "Artifact must not contain Gradle build outputs." >&2
  exit 1
fi
if [[ -n "$CHECKSUM_PATH" ]]; then
  if [[ ! -f "$CHECKSUM_PATH" ]]; then
    echo "Checksum file does not exist: $CHECKSUM_PATH" >&2
    exit 66
  fi
  ACTUAL="$(sha256sum "$ARTIFACT_PATH" | awk '{print $1}')"
  EXPECTED="$(awk '{print $1}' "$CHECKSUM_PATH")"
  if [[ "$ACTUAL" != "$EXPECTED" ]]; then
    echo "Checksum mismatch." >&2
    echo "expected: $EXPECTED" >&2
    echo "actual:   $ACTUAL" >&2
    exit 1
  fi
fi

echo "Release artifact verification passed."

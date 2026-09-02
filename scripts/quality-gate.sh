#!/usr/bin/env bash
# quality-gate.sh — Chemie Lernen Free
# Erweitert gegenüber F-Droid-Version: Lint + Unit-Tests + Debug-Build.
set -euo pipefail

echo "=== Lint (release) ==="
./gradlew :app:lintRelease --no-daemon

echo "=== Unit Tests ==="
./gradlew :app:testDebugUnitTest --no-daemon

echo "=== Build Debug APK ==="
./gradlew :app:assembleDebug --no-daemon

echo "=== Quality Gate passed ==="

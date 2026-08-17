#!/usr/bin/env bash
set -euo pipefail

echo "=== Unit Tests ==="
./gradlew :app:testDebugUnitTest --no-daemon

echo "=== Build Debug APK ==="
./gradlew :app:assembleDebug --no-daemon

echo "=== Quality Gate passed ==="

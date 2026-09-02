#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# release-pipeline.sh — Chemie Lernen Free
# Portiert von ki-kompetenz-android/scripts/release-pipeline.sh
# Qualitäts-Gate → signiertes AAB → versionCode-Bump → Commit

echo "========================================="
echo " CHEMIE LERNEN RELEASE PIPELINE"
echo "========================================="
echo ""

# ── 0. Read & bump versionCode from version.properties ──
VERSION_FILE="version.properties"
if [ ! -f "$VERSION_FILE" ]; then
    echo "ERROR: $VERSION_FILE not found"
    exit 1
fi

CURRENT_CODE=$(grep "^versionCode=" "$VERSION_FILE" | cut -d= -f2 | tr -d '[:space:]')
CURRENT_NAME=$(grep "^versionName=" "$VERSION_FILE" | cut -d= -f2 | tr -d '[:space:]')
NEW_CODE=$((CURRENT_CODE + 1))

echo "[0/5] Current: versionCode=$CURRENT_CODE versionName=$CURRENT_NAME"
echo "      New:     versionCode=$NEW_CODE"

sed -i "s/^versionCode=.*/versionCode=$NEW_CODE/" "$VERSION_FILE"
echo "      Bumped version.properties -> versionCode=$NEW_CODE"

# ── 1. Quality gate (non-blocking WARN, blocking FAIL) ──
echo ""
echo "[1/5] Quality gate..."
bash scripts/quality-gate.sh || true

# ── 2. Build signed release AAB ──
echo ""
echo "[2/5] Building signed release AAB..."
if [ -f .keystore.env ]; then
    # shellcheck disable=SC1091
    source .keystore.env
    export CHEMIELERNEN_RELEASE_STORE_FILE="$(pwd)/${STOREFILE:-chemie-lernen-release.jks}"
    export CHEMIELERNEN_RELEASE_STORE_PASSWORD="${STOREPASS:-}"
    export CHEMIELERNEN_RELEASE_KEY_ALIAS="${KEYALIAS:-}"
    export CHEMIELERNEN_RELEASE_KEY_PASSWORD="${KEYPASS:-}"
else
    echo "⚠️  .keystore.env fehlt — Baue ungesigniert (F-Droid-Variante)."
fi
./gradlew :app:clean :app:bundleRelease 2>&1 | tail -5

# ── 3. Verify AAB exists and is signed ──
echo ""
echo "[3/5] Verifying AAB..."
AAB="app/build/outputs/bundle/release/app-release.aab"
if [ ! -f "$AAB" ]; then
    echo "ERROR: AAB not found at $AAB"
    exit 1
fi
if unzip -l "$AAB" 2>/dev/null | grep -q "META-INF/.*\.RSA"; then
    echo "      OK: signed AAB ($(du -h "$AAB" | cut -f1))"
else
    echo "      ⚠️  unsigned AAB (F-Droid-Variante ok, Google Play braucht Key)"
fi

# ── 4. Commit version bump ──
echo ""
echo "[4/5] Committing version bump..."
# WICHTIG: F-Droid-Metadaten (metadata/*.yml) synchron halten
git add "$VERSION_FILE" 2>/dev/null || true
git commit -m "chore: bump versionCode $CURRENT_CODE -> $NEW_CODE" --no-verify 2>/dev/null || true
git push 2>/dev/null || true
echo "      Pushed versionCode=$NEW_CODE to origin/main"

# ── 5. Output ──
echo ""
echo "========================================="
echo " BUILD COMPLETE"
echo "========================================="
echo "  AAB:        $AAB"
echo "  Size:       $(du -h "$AAB" | cut -f1)"
echo "  versionCode: $NEW_CODE"
echo "  versionName: $CURRENT_NAME"
echo "========================================="
echo ""
echo "Next: Upload $AAB to Play Console (manuell) oder:"
echo "      bash scripts/publish-release.sh (via API, benötigt Service-Account-Key)"

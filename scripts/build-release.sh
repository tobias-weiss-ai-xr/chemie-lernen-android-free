#!/usr/bin/env bash
# build-release.sh — Chemie Lernen Free
# Automatisierter Build-Prozess für Google Play Release (signiertes AAB).
# Portiert und adaptiert von ki-kompetenz-android/scripts/build-release.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AAB_FILE="$PROJECT_ROOT/app/build/outputs/bundle/release/app-release.aab"

echo "🏗️  Chemie Lernen - Release Build"
echo "=================================="

# Check prerequisites
echo "📋 Checking prerequisites..."
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install JDK 17+"
    exit 1
fi
if [ ! -x "$PROJECT_ROOT/gradlew" ]; then
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        echo "❌ gradlew not found"
        exit 1
    fi
fi

# Check keystore + env (optional: ohne Keystore bauen wir ungesigniert = F-Droid)
KEYSTORE="$PROJECT_ROOT/chemie-lernen-release.jks"
ENV_FILE="$PROJECT_ROOT/.keystore.env"
if [ ! -f "$KEYSTORE" ]; then
    echo "⚠️  Keystore nicht gefunden — F-Droid-Variante (ungesigniert): $KEYSTORE"
    echo "💡 Für Google Play: ./scripts/generate-keystore.sh"
fi
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1091
    source "$ENV_FILE"
    export CHEMIELERNEN_RELEASE_STORE_FILE="$PROJECT_ROOT/${STOREFILE:-chemie-lernen-release.jks}"
    export CHEMIELERNEN_RELEASE_STORE_PASSWORD="${STOREPASS:-}"
    export CHEMIELERNEN_RELEASE_KEY_ALIAS="${KEYALIAS:-}"
    export CHEMIELERNEN_RELEASE_KEY_PASSWORD="${KEYPASS:-}"
fi

# Read current version
VERSION_CODE=$(grep "^versionCode=" "$PROJECT_ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]')
VERSION_NAME=$(grep "^versionName=" "$PROJECT_ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]')
TARGET_SDK=$(grep "targetSdk" "$PROJECT_ROOT/app/build.gradle.kts" | grep -oP '\d+' | head -1)

echo "📱 Current version: $VERSION_NAME (code: $VERSION_CODE)"
echo "📱 Target SDK: $TARGET_SDK"

# Validate Target SDK
if [ "$TARGET_SDK" -lt 35 ]; then
    echo "❌ ERROR: Target SDK must be 35 or higher (current: $TARGET_SDK)"
    exit 1
fi
echo "✅ Target SDK validated: $TARGET_SDK"

# Build
echo "🔨 Building release bundle..."
cd "$PROJECT_ROOT"
./gradlew clean bundleRelease --no-daemon

# Verify
if [ ! -f "$AAB_FILE" ]; then
    echo "❌ AAB not found after build"
    exit 1
fi
AAB_SIZE=$(ls -lh "$AAB_FILE" | awk '{print $5}')
echo "✅ Build successful!"
echo "📦 AAB: $AAB_FILE"
echo "📊 Size: $AAB_SIZE"

# Verify signature
echo "🔍 Verifying signature..."
if unzip -l "$AAB_FILE" | grep -q "META-INF.*RSA"; then
    echo "✅ AAB is signed"
else
    echo "⚠️  AAB is NOT signed — this variant is for F-Droid (it signs itself)."
    echo "    Für Google Play: Keystore via generate-keystore.sh erzeugen und env setzen."
fi

# Show SHA-256 fingerprint (falls Keystore vorhanden)
if [ -f "$KEYSTORE" ] && [ -n "${STOREPASS:-}" ]; then
    echo ""
    echo "🔐 SHA-256 Fingerprint:"
    KEYTOOL=$(ls "/c/Program Files/Java/"jdk-*/bin/keytool.exe 2>/dev/null | head -1 || true)
    "${KEYTOOL:-keytool}" -list -v -keystore "$KEYSTORE" -storepass "$STOREPASS" 2>/dev/null | grep "SHA256:" || true
fi

echo ""
echo "🎉 Ready for Google Play Store!"
echo "================================"
echo "Upload: $AAB_FILE"
echo "Version: $VERSION_NAME (code: $VERSION_CODE)"
echo "Package: ai.chemistry_learning_org"

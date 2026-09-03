#!/usr/bin/env bash
# build-release.sh — Chemie Lernen
# Automatisierter Build-Prozess für Google Play Release (signiertes AAB).
# Portiert von ki-kompetenz-android/scripts/build-release.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
AAB_FILE="$PROJECT_ROOT/app/build/outputs/bundle/release/app-release.aab"

# ---- Konfiguration ----------------------------------------------------------
KEYSTORE_FILE="$PROJECT_ROOT/chemie-lernen-release.jks"
ENV_FILE="$PROJECT_ROOT/.keystore.env"
PACKAGE_NAME="ai.chemistry_learning_org"

# ---- Hilfsfunktionen --------------------------------------------------------
die() {
    echo "❌ $1" >&2
    exit 1
}

fail_if_non_zero() {
    if [ "$1" -ne 0 ]; then
        echo "❌ $2 failed (exit=$1)"
        exit 1
    fi
}

# ---- Präfix für Umgebungsvariablen (F-Droid-Kompatibilität) -----------------
# ohne CHEMIELERNEN_RELEASE_* bleibt der Build Nicht-gesigned → F-Droid kann
# später selbst signieren. Google Play өfordert ampliare Variablen.

# ---- Vorbedingungen prüfen --------------------------------------------------
echo "📋 Checking prerequisites..."
command -v java &>/dev/null || die "Java not found. Please install JDK 17+"
[ -x "$PROJECT_ROOT/gradlew" ] || die "gradlew not found"

# Keystore + Umgebungsvariablen prüfen (optional für F-Droid)
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  Keystore nicht gefunden — F-Droid-Variante (ungesigniert): $KEYSTORE_FILE"
    echo "💡 Für Google Play: ./scripts/generate-keystore.sh"
fi
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1091
    source "$ENV_FILE"
    export CHEMIELERNEN_RELEASE_STORE_FILE="$PROJECT_ROOT/${STOREFILE:-chemie-lernen-release.jks}"
    export CHEMIELERNEN_RELEASE_STORE_PASSWORD="${STOREPASS:-}"
    export CHEMIELERNEN_RELEASE_KEY_ALIAS="${KEYALIAS:-}"
    export CHEMIELERNEN_RELEASE_KEY_PASSWORD="${KEYPASS:-}"
else
    echo "⚠️  .keystore.env nicht gefunden — F-Droid-Variante (ungesigniert)"
    echo "💡 Für Google Play: ./scripts/generate-keystore.sh"
fi

# ---- Versionsdaten auslesen --------------------------------------------------
VERSION_CODE=$(grep "^versionCode=" "$PROJECT_ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]')
VERSION_NAME=$(grep "^versionName=" "$PROJECT_ROOT/version.properties" | cut -d= -f2 | tr -d '[:space:]')
TARGET_SDK=$(grep "targetSdk" "$PROJECT_ROOT/app/build.gradle.kts" | grep -oP '\d+' | head -1)

if [ -z "$VERSION_CODE" ] || [ -z "$VERSION_NAME" ]; then
    die "version.properties fehlt oder ist ungültig"
fi

echo "📱 Current version: $VERSION_NAME (code: $VERSION_CODE)"
echo "📱 Target SDK: $TARGET_SDK"

# Target SDK validieren
echo "📍 Validating Target SDK..."
if [ "$TARGET_SDK" -lt 35 ] 2>/dev/null; then
    die "Target SDK must be >= 35 (current: $TARGET_SDK)"
fi
echo "✅ Target SDK validated: $TARGET_SDK"

# ---- Gradle Cache zurücksetzen (verhindert Windows File-Lock auf lint-cache) --
echo "🧹 Reset Gradle daemon to avoid file locks..."
./gradlew --stop >/dev/null 2>&1 || true
sleep 1

# ---- Leeres Clean-Aufruf dévoile Windows-File-Lock auf lint-cache ---------------
# Workaround: ohne clean funktioniert der Build-Host im Second Run nicht.
# https://github.com/gradle/gradle/issues/19886

echo "🔨 Building release bundle (signed)..."
cd "$PROJECT_ROOT"
./gradlew clean bundleRelease --no-daemon

fail_if_non_zero $? "release bundle build"

# ---- Verifizierung ------------------------------------------------------------
echo "🔍 Verifying build artifacts..."
if [ ! -f "$AAB_FILE" ]; then
    die "AAB not found after build: $AAB_FILE"
fi

AAB_SIZE=$(du -h "$AAB_FILE" | cut -f1)
echo "✅ Build successful!"
echo "📦 AAB: $AAB_FILE"
echo "📊 Size: $AAB_SIZE"

# Signatur prüfen (META-INF/*.RSA == signiert)
if unzip -l "$AAB_FILE" 2>/dev/null | grep -qP "META-INF/.*\.(RSA|SF|DSA)"; then
    echo "✅ AAB is SIGNED"
else
    die "❌ AAB is NOT signed! Building with CHEMIELERNEN_RELEASE_* env vars is required for Google Play."
fi

# Package-Name prüfen
echo "📋 Verifying package name..."
PACKAGE_CHECK=$(unzip -p "$AAB_FILE" "base/AndroidManifest.xml" 2>/dev/null | \
    grep -oP 'package="[^"]+"' | grep -oP '[^"]+' || true)
if [ "$PACKAGE_CHECK" = "$PACKAGE_NAME" ]; then
    echo "✅ Package name correct: $PACKAGE_CHECK"
else
    echo "⚠️  Package name warning: expected $PACKAGE_NAME, found: ${PACKAGE_CHECK:-<not readable>}"
fi

# SHA-256 Fingerprint anzeigen (sofern Keystore vorhanden)
if [ -f "$KEYSTORE_FILE" ] && [ -n "${STOREPASS:-}" ]; then
    echo ""
    echo "🔐 SHA-256 Fingerprint:"
    KEYTOOL=$(ls "/c/Program Files/Java/"jdk-*/bin/keytool.exe 2>/dev/null | head -1 || echo keytool)
    "$KEYTOOL" -list -v -keystore "$KEYSTORE_FILE" -storepass "$STOREPASS" 2>/dev/null | grep "SHA256:" || true
fi

echo ""
echo "🎉 Ready for Google Play Store!"
echo "================================"
echo "Upload: $AAB_FILE"
echo "Version: $VERSION_NAME (code: $VERSION_CODE)"
echo "Package: $PACKAGE_NAME"
echo "AAB Size: $AAB_SIZE"
echo ""
echo "Play Console reminder:"
echo "  • upload-key-cert SHA-256: 8B:BC:A0:54:20:27:A2:58:4A:FB:16:19:1B:94:03:AE:B3:56:F8:6A:36:71:27:59:F9:1F:40:DE:83:4D:93:1B"
echo "  • deletion URL:        https://chemie-lernen.org/disable-account/"
echo "  • App-Zugriff:         Demo user registered and in Ansible-Vault"

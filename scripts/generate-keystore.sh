#!/usr/bin/env bash
# generate-keystore.sh
# Generiert den Release-Signing-Keystore und speichert Passwörter in .keystore.env.
# Portiert von ki-kompetenz-android/scripts/generate-keystore.sh
#
# WICHTIG:
#  - .keystore.env und *.jks sind gitignored – niemals committen!
#  - BACKUP UNTER E:\backup-recovery\<...>-keystore\ anlegen (siehe README unten)
#  - Keystore-Verlust = App kann nicht mehr aktualisiert werden.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEYSTORE_FILE="$PROJECT_ROOT/chemie-lernen-release.jks"
ENV_FILE="$PROJECT_ROOT/.keystore.env"
BACKUP_DIR="/e/backup-recovery/chemie-lernen-keystore"

KEYSTORE_PASSWORD=$(openssl rand -hex 16)
KEY_PASSWORD=$KEYSTORE_PASSWORD  # PKCS12 requires same password
KEY_ALIAS="chemielernen"
DNAME="CN=Chemie Lernen, OU=Mobile, O=Chemie Lernen, L=Giessen, ST=Hessen, C=DE"

echo "🔐 Generiere Keystore..."
rm -f "$KEYSTORE_FILE" "$ENV_FILE"

# Find keytool
KEYTOOL_CMD=""
for path in \
    "/c/Program Files/Java/jdk-22/bin/keytool.exe" \
    "/c/Program Files/Java/jdk-21/bin/keytool.exe" \
    "/c/Program Files/Java/jdk-17/bin/keytool.exe" \
    "/usr/bin/keytool" \
    "/usr/local/bin/keytool"; do
    if [ -f "$path" ]; then
        KEYTOOL_CMD="$path"
        break
    fi
done

if [ -z "$KEYTOOL_CMD" ]; then
    echo "❌ keytool nicht gefunden"
    exit 1
fi

# Generate Keystore
"$KEYTOOL_CMD" -genkeypair \
    -v \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_FILE" \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -alias "$KEY_ALIAS" \
    -dname "$DNAME" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD"

echo "✅ Keystore erstellt: $KEYSTORE_FILE"

# Write .keystore.env
cat > "$ENV_FILE" << EOF
STOREFILE=chemie-lernen-release.jks
STOREPASS=$KEYSTORE_PASSWORD
KEYPASS=$KEY_PASSWORD
KEYALIAS=$KEY_ALIAS
EOF

echo "✅ Passwörter gespeichert in: $ENV_FILE"
echo ""

# Backup nach E:\backup-recovery
if [ -d "/e/backup-recovery" ]; then
    mkdir -p "$BACKUP_DIR"
    cp "$KEYSTORE_FILE" "$BACKUP_DIR/"
    cp "$ENV_FILE" "$BACKUP_DIR/"
    echo "✅ Backup angelegt: $BACKUP_DIR"
else
    echo "⚠️  E:\\backup-recovery nicht erreichbar – BITTE MANUELL BACKUPEN:"
    echo "   $KEYSTORE_FILE"
    echo "   $ENV_FILE"
fi

echo ""
echo "SHA-256 Fingerprint:"
"$KEYTOOL_CMD" -list -v -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" | grep "SHA256:" || true
echo ""
echo "VORSICHT: Alle bisherigen Passwort-Masterkopien prüfen, Backup doppelt ablegen."

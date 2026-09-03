#!/usr/bin/env bash
set -uo pipefail
cd "$(dirname "$0")/.."
# ============================================================
# Google Play Developer Publishing API — automated release
# Portiert von ki-kompetenz-android/scripts/publish-release.sh
# Requires: google-play-api-key.json (Service Account) [gitignored]
# Usage:    bash scripts/publish-release.sh
# ============================================================

API_KEY_FILE="google-play-api-key.json"
PACKAGE="ai.chemistry_learning_org"
AAB="app/build/outputs/bundle/release/app-release.aab"
DE_DIR="play-store/listing/de-DE"
EN_DIR="play-store/listing/en-US"

if [ ! -f "$API_KEY_FILE" ]; then
    echo "❌ ERROR: $API_KEY_FILE not found"
    echo ""
    echo "Setup required (once):"
    echo "  1. Play Console → Setup → API access → Create service account"
    echo "  2. Google Cloud Console → IAM → Service Accounts → Keys → Add Key (JSON)"
    echo "  3. Save as $API_KEY_FILE"
    echo "  4. Add service account email to Play Console → API access"
    echo ""
    echo "See: GOOGLE_PLAY_API_SETUP.md"
    exit 1
fi

if [ ! -f "$AAB" ]; then
    echo "❌ ERROR: $AAB not found. Run: bash scripts/release-pipeline.sh"
    exit 1
fi

echo "=== GOOGLE PLAY PUBLISH ==="

# 1. Get OAuth2 access token (JWT assert)
HEADER=$(echo -n '{"alg":"RS256","typ":"JWT"}' | base64 -w0 | tr '+/' '-_' | tr -d '=')
NOW=$(date +%s)
CLAIMS=$(echo -n "{\"iss\":\"$(grep -o '"client_email": *"[^"]*"' "$API_KEY_FILE" | head -1 | sed 's/.*"client_email": *"//;s/"//')\",\"scope\":\"https://www.googleapis.com/auth/androidpublisher\",\"aud\":\"https://oauth2.googleapis.com/token\",\"iat\":$NOW,\"exp\":$((NOW+3600))}" | base64 -w0 | tr '+/' '-_' | tr -d '=')
PRIVKEY=$(grep '"private_key"' "$API_KEY_FILE" | sed 's/.*"private_key": *"//;s/" *,\?$//' | tr -d '\\n' | sed 's/\\n/\n/g')
SIG=$(printf '%s' "$HEADER.$CLAIMS" | openssl dgst -sha256 -sign <(printf '%s' "$PRIVKEY") -binary | base64 -w0 | tr '+/' '-_' | tr -d '=')
JWT="$HEADER.$CLAIMS.$SIG"

ACCESS_TOKEN=$(curl -s -X POST "https://oauth2.googleapis.com/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$JWT" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Authentication failed"
    exit 1
fi
echo "✅ Authenticated"
AUTH="Authorization: Bearer $ACCESS_TOKEN"

# 2. Create edit
echo "[1/4] Creating edit..."
EDIT_ID=$(curl -s -X POST \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$EDIT_ID" ]; then
    echo "❌ Failed to create edit"
    echo "   Hinweis: App muss in Play Console angelegt sein (Package $PACKAGE)."
    exit 1
fi
echo "✅ Edit: $EDIT_ID"

# 3. Upload AAB
echo "[2/4] Uploading AAB..."
HTTP_CODE=$(curl -s -o /tmp/upload_result.json -w "%{http_code}" -X POST \
    -H "$AUTH" \
    -H "Content-Type: application/octet-stream" \
    --data-binary "@$AAB" \
    "https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/$PACKAGE/edits/$EDIT_ID/bundles")

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Upload failed (HTTP $HTTP_CODE)"
    cat /tmp/upload_result.json 2>/dev/null | head -5
    exit 1
fi
BUNDLE_CODE=$(grep -o '"versionCode":"[^"]*"' /tmp/upload_result.json | cut -d'"' -f4 || echo "?")
echo "✅ Uploaded: versionCode=$BUNDLE_CODE"

# 4. Read version + create production release
VCODE=$(grep "^versionCode=" version.properties | cut -d= -f2 | tr -d '[:space:]')
VNAME=$(grep "^versionName=" version.properties | cut -d= -f2 | tr -d '[:space:]')

echo "[3/4] Creating production release ($VNAME, code $VCODE)..."
curl -s -X POST \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    -d "{
        \"releases\": [{
            \"name\": \"$VNAME ($VCODE)\",
            \"versionCodes\": [$VCODE],
            \"releaseNotes\": {
                \"de-DE\": \"Version $VNAME: Chemie Lernen — interaktive Lernplattform für Chemie (12 Themenbereiche, 10+ Rechner, 15 Lernvideos).\",
                \"en-US\": \"Version $VNAME: Chemie Lernen — interactive chemistry learning platform (12 topics, 10+ calculators, 15 videos).\"
            },
            \"status\": \"inProgress\"
        }]
    }" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits/$EDIT_ID/tracks/production:releases" > /dev/null
echo "✅ Release created"

# 5. Update store listings
echo "[4/4] Updating store listings..."
for LANG_DIR in "$DE_DIR" "$EN_DIR"; do
    if [ ! -f "$LANG_DIR/full-description.txt" ]; then
        echo "      ⚠️  $LANG_DIR fehlt — übersprungen (Assets später via play-store/ hinzufügen)"
        continue
    fi
    LANG=$(basename "$LANG_DIR")
    echo -n "  $LANG... "
    curl -s -X PATCH \
        -H "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{\"fullDescription\": $(python3 -c 'import json,sys; print(json.dumps(open(sys.argv[1]).read()))' "$LANG_DIR/full-description.txt")}" \
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits/$EDIT_ID/listings/$LANG/fullDescription" > /dev/null
    curl -s -X PATCH \
        -H "$AUTH" \
        -H "Content-Type: application/json" \
        -d "{\"shortDescription\": $(python3 -c 'import json,sys; print(json.dumps(open(sys.argv[1]).read()))' "$LANG_DIR/short-description.txt")}" \
        "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits/$EDIT_ID/listings/$LANG/shortDescription" > /dev/null
    echo "✅"
done

# 6. Commit
echo "Committing..."
COMMIT_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "$AUTH" \
    -H "Content-Type: application/json" \
    "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/$PACKAGE/edits/$EDIT_ID:commit")

if [ "$COMMIT_CODE" = "200" ]; then
    echo "✅ Committed successfully"
else
    echo "❌ Commit failed (HTTP $COMMIT_CODE)"
    exit 1
fi

echo ""
echo "=== PUBLISHED ==="
echo "Package: $PACKAGE"
echo "Version: $VNAME ($VCODE)"
echo "Status: inProgress (review starts automatically)"
echo ""
echo "⚠️  Remaining manual steps in Play Console:"
echo "  - Countries/regions (if not set)"
echo "  - Content rating (if not set)"
echo "  - Financial/Health declarations (if not set)"

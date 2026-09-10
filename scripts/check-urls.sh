#!/usr/bin/env bash
# check-urls.sh — live audit of every URL the app can open.
#
# The WebView shows the site's 404 page ("Seite nicht gefunden") for any
# slug the site has moved or renamed (this happened: /molar-masse-rechner/
# -> /molare-masse-rechner/, /konzentration-rechner/ -> /konzentrationsumrechner/).
# This script curls every slug from the app's data lists and fails on any
# non-200, so a broken link can never reach a release unnoticed.
#
# Usage:  bash scripts/check-urls.sh [base-url]     (default: https://chemie-lernen.org)
# Exit:   0 = all 200, 1 = at least one broken/missing
set -euo pipefail
cd "$(dirname "$0")/.."

BASE="${1:-https://chemie-lernen.org}"
SRC="app/src/main/java/ai/chemistry_learning_org/app/ui"

# --- collect slugs straight from the source lists (single source of truth) ---
CALC_SLUGS=$(grep -oE '"/[a-z0-9/-]+/"' "$SRC/calculators/CalculatorsScreen.kt" | tr -d '"')
TOPIC_SLUGS=$(grep -oE '"/themenbereiche/[^"]+/"' "$SRC/topics/TopicsScreen.kt" | tr -d '"')
STATIC_SLUGS=$(printf '/\n/lernvideos/\n/datenschutz/\n/impressum/\n')

FAIL=0
CHECKED=0

check() {
    local path="$1"
    local url="${BASE%/}${path}"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" -m 15 "$url")
    CHECKED=$((CHECKED + 1))
    if [ "$code" != "200" ]; then
        echo "  BROKEN ($code)  $url"
        FAIL=1
    else
        echo "  ok       ($code)  $url"
    fi
}

echo "=== Live URL audit: $BASE ==="
echo "-- static pages --"
while IFS= read -r p; do [ -n "$p" ] && check "$p"; done <<< "$STATIC_SLUGS"
echo "-- calculators ($(echo "$CALC_SLUGS" | wc -l)) --"
while IFS= read -r p; do [ -n "$p" ] && check "$p"; done <<< "$CALC_SLUGS"
echo "-- topics --"
while IFS= read -r p; do [ -n "$p" ] && check "$p"; done <<< "$TOPIC_SLUGS"

echo "=== $CHECKED URLs checked ==="
if [ "$FAIL" != "0" ]; then
    echo "RESULT: FAIL — broken links above will render 'Seite nicht gefunden' in the app"
    exit 1
fi
echo "RESULT: PASS — all URLs live"

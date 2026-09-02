# GO LIVE — Status & Checklist (Chemie Lernen · Google Play)

Stand: **2026-09-02** · Branch `main` (gepusht)

## Status-Übersicht

| # | Schritt | Status |
|---|---------|--------|
| 1 | Versionierung via `version.properties` (Single Source of Truth) | ✅ |
| 2 | Signing-Config (F-Droid-sicher: unsigniert ohne Env) | ✅ |
| 3 | Release-Keystore `chemie-lernen-release.jks` + `.keystore.env` (gitignored) | ✅ |
| 4 | Keystore-Backup `E:\backup-recovery\chemie-lernen-keystore\` | ✅ |
| 5 | Unit-Tests (17): WebUrlPolicy-Whitelist + Navigation | ✅ |
| 6 | Quality Gate (Lint + Tests + Debug-Build) | ✅ |
| 7 | Release-Skripte: `build-release.sh`, `release-pipeline.sh`, `publish-release.sh` | ✅ |
| 8 | CI `build-release.yml` (signiertes AAB, Signatur-Verify) | ⛔ wartet auf GitHub Secrets |
| 9 | Launch-Crash-Fix (HomeScreen: LazyGrid ↔ verticalScroll) | ✅ (Gerätetest gefunden) |
| 10 | Store-Assets: 8 Screenshots (1080×2160), Icon 512², Feature-Graphic 1024×500 | ✅ |
| 11 | Store-Listing DE/EN (Limits konform, Safe-Area-Aspect) | ✅ |
| 12 | Privacy Policy Text (Repo) + Live-URL `https://chemie-lernen.org/datenschutz` (HTTP 200) | ✅ |
| 13 | Google Play Developer Account ($25) | ⛔ **Manuell im Browser** |
| 14 | App in Play Console anlegen | ⛔ **Manuell im Browser** |
| 15 | GitHub Actions Secrets (4×) | ⛔ Werte in `E:\backup-recovery\chemie-lernen-keystore\.keystore.env`, Anleitung `SECRETS_SETUP.md` |
| 16 | Tentative: Play App Signing aktivieren | ⛔ nach App-Erstellung |
| 17 | Publishing via API (Service-Account) | ⛔ wartet auf `google-play-api-key.json` |
| 18 | Review starten | ⛔ nach Upload |

## Wichtige IDs

- **Package Name**: `org.chemie_lernen_org.free`
- **Version**: `1.0.0` (code 1) — aktuell identisch mit F-Droid
- **Keystore**: `chemie-lernen-release.jks` (Upload-Key; Alias `chemielernen`)
- **SHA-256**: `8B:BC:A0:54:20:27:A2:58:4A:FB:16:19:1B:94:03:AE:B3:56:F8:6A:36:71:27:59:F9:1F:40:DE:83:4D:93:1B`

## Nächste Schritte (nur 3 Blöcke)

### A) CI Secrets setzen (~2 min, Browser)
GitHub → `tobias-weiss-ai-xr/chemie-lernen-android-free` → Settings → Secrets → **4×**:
`CHEMIELERNEN_RELEASE_KEYSTORE_B64`, `_STORE_PASSWORD`, `_KEY_PASSWORD`, `_KEY_ALIAS`
Werte: `E:\backup-recovery\chemie-lernen-keystore\.keystore.env` · Anleitung: `SECRETS_SETUP.md`

### B) Play Console einrichten (~30 min, Browser)
Anleitung komplett: `PLAY_CONSOLE_CHECKLIST.md` + `GOOGLE_PLAY_API_SETUP.md`
1. $25 Account → App „Chemie Lernen" anlegen (Package `org.chemie_lernen_org.free`)
2. Store Listing (DE+EN, Assets aus `play-store/`)
3. IARC-Rating 3+, Ads=Nein, Finanzfunktionen=Nein, Data-Safety-Formular
4. Länder: DE, AT, CH
5. (API-Zugang für Automatisierung, optional)

### C) AAB hochladen & Review
- Variante 1: AAB aus `scripts/release-pipeline.sh` → Play Console Production-Track
- Variante 2: nach API-Setup `bash scripts/publish-release.sh`

## Wichtig
- `versionCode` UND `versionName` in `version.properties` ÄNDERN → F-Droid-Metadata
  `metadata/org.chemie_lernen_org.free.yml` **synchron halten** (F-Droid liest daraus).
- Keystore-Verlust = keine Updates mehr. Backup liegt in `E:\backup-recovery\chemie-lernen-keystore\`
  (zusätzlich zweites Medium, z. B. verschlüsseltes Backup).

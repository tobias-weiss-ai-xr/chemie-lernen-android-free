# Google Play Console Release Checkliste — Chemie Lernen (Free)

Package: `ai.chemistry_learning_org` · App-Name: „Chemie Lernen" · Kategorie: **Education**

## ✅ Automatisiert (CI/CD & Skripte)
- [x] Quality Gate (Lint + Unit-Tests + Debug-Build)
- [x] 17+ Unit-Tests (WebUrlPolicy-Whitelist, Navigation)
- [x] Signiertes AAB (`scripts/build-release.sh`, `.github/workflows/build-release.yml`)
- [x] Signatur-Verifikation + SHA-256-Fingerprint
- [x] Target-SDK-36-Validierung (compileSdk/targetSdk = 36, Android 16)
- [x] versionCode-Auto-Increment (`scripts/release-pipeline.sh`)

## ⚠️ Manuelle Schritte (Google Play Console)

### 1. Developer Account
- [ ] $25 einmalig bezahlen
- [ ] Account erstellen

### 2. App erstellen
- [ ] „Create app": Name **Chemistry Learning**, Package `ai.chemistry_learning_org`
- [ ] **Standard-Sprache: Englisch (en-US)** (App-Name „Chemistry Learning"), zusätzlich Deutsch (de-DE, „Chemie Lernen")

### 3. Store Listing
- [ ] **App-Name pro Sprache**: en-US „Chemistry Learning", de-DE „Chemie Lernen"
- [ ] Kurzbeschreibung (≤80): `play-store/listing/{de-DE,en-US}/short-description.txt`
- [ ] Vollbeschreibung (≤4000): `play-store/listing/{de-DE,en-US}/full-description.txt`
- [ ] Screenshots (8×, 1080×2160): `play-store/screenshots/phone/`
- [ ] App-Icon (512×512): `play-store/icons/icon-512-square.png`
- [ ] Feature-Graphic (1024×500): `play-store/feature-graphic.png`
- [ ] Kategorie: **Education**
- [ ] Kontakt-E-Mail: chemie-lernen@tobias-weiss.org
- [ ] Datenschutz-URL: `https://chemie-lernen.org/datenschutz` (HTTP 200 ✓)

### 4. Content Rating (IARC)
- [ ] Fragebogen ausfüllen (Bildung, keine Gewalt/Glücksspiel/Drogen)
- [ ] Rating: **3+** (kindgerecht, reine Bildung)

### 5. App Content Fragen
- [ ] **Finanzfunktionen**: „Nein"
- [ ] **Gesundheitserklärung**: ausfüllen (Bildung, keine Medizin)
- [ ] **Ads**: „Nein" (keine Werbung, kein Tracking)
- [ ] **US Elections**: „Nein"

### 6. Data Safety
- [ ] App erhebt **keine** Daten (kein Tracking, keine Analysedienste)
- [ ] Netzwerkverkehr nach chemie-lernen.org angeben
- [ ] Vgl. `play-store/privacy-policy.txt`

### 7. Release erstellen
- [ ] Production-Track: AAB hochladen `app/build/outputs/bundle/release/app-release.aab`
- [ ] Release-Notes (DE/EN): `play-store/listing/*/release-notes.txt`
- [ ] Länder: Deutschland, Österreich, Schweiz (+ optional weitere EU)
- [ ] Review starten (1–7 Tage)

## 📋 Bekannte Fallstricke
| Fehler | Lösung |
|---|---|
| „Keine Länder ausgewählt" | Production Track → Edit → Countries/regions |
| „APK durch höheren VersionCode blockiert" | `bash scripts/release-pipeline.sh` (bumpt automatisch) |
| Screenshot-Verhältnis falsch | 1080×2160 = 2:1 (longest ≤ 2× shortest) |
| „APK muss Paketnamen ai.chemistry_learning_org haben" | applicationId ist bereits `ai.chemistry_learning_org` — App in Console mit genau diesem Paket erstellen (Paketnamen sind nach Erstellung unveränderlich) |
| „Sollte API-Mindestebene 36 haben" | targetSdk ist bereits 36 (`app/build.gradle.kts`) — ggf. Console-Cache refreshen / AAB erneut hochladen |
| „Nativer Code ohne Debug-Symbole" (Warnung) | Ignorierbar: einziges natives Modul ist die vorab-gesymbolfreie Jetpack-Lib `libandroidx.graphics.path.so` (kein `.symtab`) — es gibt keine Symbole hochzuladen; `ndk.debugSymbolLevel=FULL` aktiviert automatisches Symbol-ZIP falls später eigene Native-Libs hinzukommen |

## 🚀 Workflow
```bash
bash scripts/release-pipeline.sh                      # Gate + signiertes AAB + Bump
# AAB manuell in Play Console hochladen ODER (nach API-Setup):
bash scripts/publish-release.sh                       # vollautomatisch via API
```

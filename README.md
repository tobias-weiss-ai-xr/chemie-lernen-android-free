# Chemie Lernen — Android App (Free Edition / F-Droid)

Inoffizielle Android-App f\u00fcr **chemie-lernen.org** — die kostenlose
interaktive Lernplattform f\u00fcr Chemie von Tobias Wei\u00df.

Die App bietet eine mobile-optimierte Ansicht des Webauftrags mit
schneller Navigation zu Themenbereichen, Rechnern und Lernvideos.

## Inhalt

- **12 Themenbereiche** der Chemie (Einf\u00fchrung bis Biochemie)
- **10+ Rechner** (Molare Masse, pH, St\u00f6chiometrie, Gasgesetze…)
- **15 Lernvideos** von Zig\u2019s Chemistry 42 (Prof. Siegfried Schindler)
- **Wissensnetz** der Chemie

Alle Inhalte stammen von chemie-lernen.org. Die App ist ein WebView-Wrapper
und bietet keine eigenen Inhalte — sie \u00f6ffnet die Website mobil-optimiert
an, ohne Werbung, ohne Tracking.

## Features

- **Native Android UI** (Jetpack Compose, Material 3, NavigationBar)
- **Offline-f\u00e4hig**: WebView-cached Seiten funktionieren ohne Internet
- **F-Droid-kompatibel**: keine Google Play Services, kein Tracking
- **Open Source**: Apache-2.0 Lizenz

## Build

\`\`\`bash
./gradlew :app:assembleDebug        # Debug-APK
./gradlew :app:testDebugUnitTest    # Unit-Tests
\`\`\`\`

Voraussetzungen: JDK 17, Android SDK (compileSdk 35).

Die App benötigt **keinen Keystore** zum Bauen — F-Droid und andere
Distributionen signieren selbst.

## Distribution

| | Free Edition (dieses Repo) |
|---|---|
| App-ID | `org.chemie_lernen_org.free` |
| Lizenz | Apache-2.0 |
| Distribution | F-Droid / direkt · **Google Play** (in Vorbereitung) |
| Quellcode | [GitHub](https://github.com/tobias-weiss-ai-xr/chemie-lernen-android-free) |

### Release für Google Play

```bash
bash scripts/generate-keystore.sh      # einmalig: Keystore + .keystore.env (Backup nach E:\backup-recovery)
bash scripts/release-pipeline.sh       # Quality Gate → signiertes AAB → versionCode-Bump
bash scripts/publish-release.sh        # (optional) via Publishing-API, braucht Service-Account-Key
```

- `version.properties` ist Single Source of Truth für versionCode/versionName
  (F-Droid-Metadata `metadata/*.yml` synchron halten)
- **F-Droid-Kompatibilität bleibt**: ohne Signing-Env baut die App weiterhin
  `app-release-unsigned.apk` (F-Droid signiert selbst)
- Store-Assets & Checklisten: `play-store/`, `PLAY_CONSOLE_CHECKLIST.md`, `GO_LIVE.md`

## Architektur

Die App ist ein **Jetpack Compose WebView-Wrapper**:

- `MainActivity` → `ChemieNavHost` → Bottom-Navigation
- Screens: Home, Topics, Calculators, Videos, Settings
- `WebViewScreen`: l\u00e4dt chemie-lernen.org URLs in einer nativen Activity
- Inhalt: geh\u00f6st komplett von der Website; Updates erfolgen
  automatisch (Hugo rebuilds die Seite kontinuierlich)
- Kein API-Client, keine Room-Datenbank n\u00f6tig

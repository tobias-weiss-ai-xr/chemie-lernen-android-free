# Google Play Developer Publishing API — Setup
# (adaptiert von ki-kompetenz-android)

## Voraussetzung: Service Account in Play Console erstellen

1. https://play.google.com/console → App „Chemie Lernen"
2. **Setup → API access → Create new service account**
3. Rolle: **Admin** (oder App Editor) zuweisen

### JSON Key herunterladen
1. Google Cloud Console → IAM → Service Accounts
2. Service Account → **Keys** → **Add Key** → JSON
3. Speichern als `google-play-api-key.json` im Repo-Root
4. **NICHT committen** (→ in `.gitignore`)

### Service Account E-Mail in Play Console eintragen
1. Play Console → Setup → API access
2. E-Mail (aus JSON: `client_email`) eintragen, Rolle **Admin**

## IDs
- **Package**: `ai.chemistry_learning_org`
- **AAB**: `app/build/outputs/bundle/release/app-release.aab`
- **Listings**: `play-store/listing/{de-DE,en-US}/`

## Nutzung
```bash
bash scripts/publish-release.sh
```
Skript macht:
1. OAuth2 (JWT mit Service-Account-Key)
2. Edit erstellen
3. AAB hochladen
4. Production-Release anlegen (inProgress)
5. Store-Listings (DE+EN) aktualisieren
6. Commit

**Kein Browser. Kein MCP. Nur HTTP.**

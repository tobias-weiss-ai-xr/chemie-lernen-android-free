# SECRETS SETUP — Chemie Lernen Free (Google Play)

Nur GitHub-Actions-Secrets, die für den signierten Play-Build (`build-release.yml`)
nötig sind. Werte kommen aus `E:\backup-recovery\chemie-lernen-keystore\.keystore.env`.

## GitHub Secrets setzen (4 Stück)

GitHub → `tobias-weiss-ai-xr/chemie-lernen-android-free` →
Settings → Secrets and variables → Actions → **New repository secret**:

| Name | Wert (aus `.keystore.env`) |
|---|---|
| `CHEMIELERNEN_RELEASE_KEYSTORE_B64` | `base64 -w0 chemie-lernen-release.jks` |
| `CHEMIELERNEN_RELEASE_STORE_PASSWORD` | `STOREPASS` |
| `CHEMIELERNEN_RELEASE_KEY_PASSWORD` | `KEYPASS` |
| `CHEMIELERNEN_RELEASE_KEY_ALIAS` | `KEYALIAS` (aktuell: `chemielernen`) |

Key-Base64 erzeugen (nur einmal):
```bash
base64 -w0 chemie-lernen-release.jks > keystore.b64   # Inhalt → Secret kopieren
```

## Optional: Play Publishing API (Automatisierung)

- Service-Account-Key (`google-play-api-key.json`) nach **Repo-Root** legen
  (gitignored!) → Setup-Anleitung in `GOOGLE_PLAY_API_SETUP.md`
- Danach: `bash scripts/publish-release.sh`

## Sicherheit

- `chemie-lernen-release.jks` + `.keystore.env` sind **gitignored** ❗
- Backup: `E:\backup-recovery\chemie-lernen-keystore\` (inkl. Passwort-README)
- Geht der Upload-Key verloren: Play App Signing erlaubt Upload-Key-Reset,
  solange das App-Signing-Key bei Google liegt

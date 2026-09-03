# SECRETS SETUP — Chemie Lernen Free (Google Play)

Nur GitHub-Actions-Secrets, die für den signierten Play-Build (`build-release.yml`)
nötig sind. Werte kommen aus der **Primärablage** `C:\Users\Tobias\secrets\chemie-lernen-play\github-secrets.env`
(fertig vorbereitet; E:\backup-recovery\chemie-lernen-keystore\ ist nur Backup-Spiegel).

## GitHub Secrets setzen (4 Stück)

GitHub → `tobias-weiss-ai-xr/chemie-lernen-android-free` →
Settings → Secrets and variables → Actions → **New repository secret**:

| Name | Wert (aus `github-secrets.env`) |
|---|---|
| `CHEMIELERNEN_RELEASE_KEYSTORE_B64` | fertig vorbereitet (base64 des jks, einzeilig) |
| `CHEMIELERNEN_RELEASE_STORE_PASSWORD` | fertig vorbereitet |
| `CHEMIELERNEN_RELEASE_KEY_PASSWORD` | fertig vorbereitet |
| `CHEMIELERNEN_RELEASE_KEY_ALIAS` | fertig vorbereitet (aktuell: `chemielernen`) |

**Alle 4 Werte sind fertig vorbereitet** in
`C:\Users\Tobias\secrets\chemie-lernen-play\github-secrets.env` —
einfach den jeweiligen Wert aus der Datei per Copy-Paste ins Secret übernehmen.

(Regenerieren, falls nötig, nur einmal:)
```bash
base64 -w0 chemie-lernen-release.jks > keystore.b64   # Inhalt → Secret kopieren
```

## Optional: Play Publishing API (Automatisierung)

- Service-Account-Key (`google-play-api-key.json`) nach **Repo-Root** legen
  (gitignored!) → Setup-Anleitung in `GOOGLE_PLAY_API_SETUP.md`
- Danach: `bash scripts/publish-release.sh`

## Sicherheit

- `chemie-lernen-release.jks` + `.keystore.env` sind **gitignored** ❗
- **Primär:** `C:\Users\Tobias\secrets\chemie-lernen-play\` (nur lokal, chmod 600)
- **Backup-Spiegel (nur Backup):** `E:\backup-recovery\chemie-lernen-keystore\`
- Geht der Upload-Key verloren: Play App Signing erlaubt Upload-Key-Reset,
  solange das App-Signing-Key bei Google liegt

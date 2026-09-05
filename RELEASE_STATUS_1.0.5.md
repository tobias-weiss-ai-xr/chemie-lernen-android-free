# Release Status: Chemie Lernen 1.0.5 (Code 6)

## Summary

This release includes the major **Knowledge Graph (Wissensnetz) rewrite** with ego-graph exploration and 2D/3D toggle, replacing the global 3D graph that was deemed "bugged" by users (10-23s layout time).

## ✅ Completed

### App Changes
- **KnowledgeGraphScreen rewrite** (`e844b2a`):
  - Per-term ego graphs (center + 1-hop neighbors with cross edges)
  - 2D/3D toggle via `Graph3DLayout.Config(planar)`
  - Picker state for term selection with search + category chips
  - Node labels via `TextMeasurer` for graphs ≤ 60 nodes
  - Auto-rotate when nothing selected
  - Proper back navigation chain
  - All 5 `Log.d("KgDebug")` calls removed
  - Removed unused `kg_computing` strings from both locales

- **Version bump** (`d6a8117`, `eb49532`):
  - versionCode: 5 → 6
  - versionName: 1.0.4 → 1.0.5

- **Release notes** (`2a3e25b`):
  - DE: "Das Wissensnetz als interaktiver Ego-Graph – wähle einen Begriff und erkunde sein direktes Umfeld in 2D oder 3D"
  - EN: "Knowledge Network as interactive Ego Graph – pick a term and explore its neighborhood in 2D or 3D"

- **F-Droid metadata** (`308da5c`):
  - Added version 1.0.5 (code 6, commit eb49532) to `metadata/ai.chemistry_learning_org.yml`

### Build Artifacts
- **Signed AAB**: `app/build/outputs/bundle/release/app-release.aab` (3.0M)
- **Test Status**: All unit tests pass (`BUILD SUCCESSFUL`)
- **Target**: Android 16 (API 36), minSdk 26

### Play Store Assets
All required assets are present in `play-store/`:
- Listings: `de-DE/` and `en-US/` (short-description, full-description, keywords, release-notes)
- Screenshots: 8 phone screenshots (1080×2160)
- Feature graphic: `feature-graphic.png` (1024×500)
- Icons: `icon-512.png` and `icon-512-square.png` (512×512)

### TaskFleet TUD Configuration (`taskfleet@5b9ce14`)
- `config/workers-tud.json`: 6 TUD model workers (glm-5.2-awq, gpt-oss-120b, gemma-4-31b-it, mistral-medium-3.5-128b, qwen3.6-35b-a3b, ministral-3-14b-instruct)
- `config/tasks-chemie-kg.json`: 3 KG-specific tasks (UI review, code review, implement fixes)
- `config/tasks-chemie-tud.json`: 4 general app tasks (code health, accessibility, test coverage, implement)
- `run-chemie-kg-tud.sh`: Launcher for KG-specific TUD tasks
- `run-chemie-tud.sh`: Launcher for general app improvement with TUD models

## ⚠️ Pending / Notes

### Play Store Upload
1. **Create app** in Play Console:
   - Package name: `ai.chemistry_learning_org`
   - App name: "Chemistry Learning" (en-US), "Chemie Lernen" (de-DE)
   - Category: Education

2. **Upload AAB**: `app/build/outputs/bundle/release/app-release.aab` (3.0M)

3. **Store listing**:
   - Copy texts from `play-store/listing/{de-DE,en-US}/`
   - Upload screenshots from `play-store/screenshots/phone/`
   - Upload feature graphic: `play-store/feature-graphic.png`
   - Upload app icon: `play-store/icons/icon-512-square.png`
   - Set contact email: `chemie-lernen@tobias-weiss.org`
   - Set privacy policy URL: `https://chemie-lernen.org/datenschutz`

4. **Content rating**: Fill IARC questionnaire → **3+**

5. **App content questions**:
   - Finanzfunktionen: No
   - Gesundheitserklärung: Bildung, keine Medizin
   - Ads: **No** (keine Werbung, kein Tracking)
   - US Elections: No

6. **Data Safety**: App erhebt **keine** Daten. Network traffic to chemie-lernen.org.

### Outstanding Tasks
- ⚠️ **screenshot_06_wissensnetz.png**: Update to show new ego-graph picker + 2D/3D view (currently shows old global graph). Requires device with app installed.
- ⚠️ **GitHub Actions secrets**: Set 4 secrets in repository from `C:/Users/Tobias/secrets/chemie-lernen-play/github-secrets.env`:
  - `CHEMIELERNEN_RELEASE_KEYSTORE_B64`
  - `CHEMIELERNEN_RELEASE_STORE_PASSWORD`
  - `CHEMIELERNEN_RELEASE_KEY_PASSWORD`
  - `CHEMIELERNEN_RELEASE_KEY_ALIAS`
- ⚠️ **Device smoke test**: Manually test on Pixel 8 Pro (3C211FDJG0001X) German locale: picker → tap term → fast ego graph → toggle 2D/3D → tap node → card → chip re-center → article → WebView
- ⚠️ **Google Play API key**: For automated publishing, place `google-play-api-key.json` in repo root (gitignored) and run `bash scripts/publish-release.sh`

## Git Commits (chemie-lernen-android-free)

```
308da5c chore(f-droid): add version 1.0.5 (code 6) metadata for ego-graph Wissensnetz
EB49532 chore: bump versionName 1.0.4 -> 1.0.5 for ego-graph Wissensnetz rewrite
d6a8117 chore: bump versionCode 5 -> 6
2a3e25b chore(store): update release notes for ego-graph Wissensnetz with 2D/3D toggle
e844b2a feat(kg): rewrite KnowledgeGraphScreen with ego graphs + 2D/3D toggle
c261ce0 Native 3D knowledge graph (Wissensnetz) replacing dead /wissensnetz/ webview
```

## Git Commits (taskfleet)

```
5b9ce14 feat(taskfleet): add TUD-only workers + tasks for chemie-lernen improvements
```

## Test Results

```
./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
```

Test count: ~148 green (all tests pass).

## Technical Details

- **AI Model Setup**: TUD models work via LiteLLM at `http://127.0.0.1:4000/v1`
- **API Key**: From `~/.pi/agent/models.json` (fallback chain in TaskFleet)
- **tud/glm-5.2-awq**: Reasoning model, requires `max_tokens >= 512`
- **Device**: Pixel 8 Pro Android 16 `3C211FDJG0001X`, German locale, PIN-locked

---

*Last updated: $(date)*

# Release Guide

This document describes the process for preparing, signing, and publishing new versions of Reflex for GitHub Releases and F-Droid.

---

## 1. Release Secrets (GitHub Actions)

To enable automated release signing for stable releases in GitHub Actions, configure the following **Repository Secrets** in GitHub (*Settings → Secrets and variables → Actions*):

- `KEYSTORE_BASE64`: The base64-encoded release keystore file (`base64 -w 0 release.keystore` or `[Convert]::ToBase64String([IO.File]::ReadAllBytes('release.keystore'))`).
- `KEYSTORE_PASSWORD`: Keystore password.
- `KEY_ALIAS`: Key alias.
- `KEY_PASSWORD`: Key password.

> **Note**: Pre-release tags containing `-` (e.g. `v1.0.0-rc1`) can build without signing secrets for dry runs. Stable tags (e.g. `v1.0.0`) will fail the build if signing secrets are missing.

---

## 2. Release Checklist

1. **Version Update**:
   - Verify `versionCode` and `versionName` in `app/build.gradle.kts`.
   - Update `CHANGELOG.md` with the new version section (e.g. `## [1.0.0] - YYYY-MM-DD`).
   - Update `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.

2. **Local Verification**:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug assembleRelease
   ```

3. **Tag & Push**:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

4. **Automated Release**:
   - The `.github/workflows/release.yml` workflow extracts release notes from `CHANGELOG.md`, signs the release APK, verifies the signature with `apksigner`, generates SHA-256 checksums, and uploads:
     - `Reflex-vX.Y.Z.apk`
     - `Reflex-vX.Y.Z.apk.sha256`

5. **Checksum Verification**:
   ```bash
   sha256sum -c Reflex-vX.Y.Z.apk.sha256
   ```

6. **F-Droid Distribution**:
   - Ensure the recipe in `docs/fdroid/com.reflex.productivity.yml` matches the latest tag and version code.

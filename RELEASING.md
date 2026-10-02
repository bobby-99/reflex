# Release Guide

This document describes the step-by-step process for preparing and publishing new versions of Reflex for GitHub Releases and F-Droid.

---

## 1. Release Checklist

1. **Version Update**:
   - Bump `versionCode` (integer) and `versionName` (SemVer) in `app/build.gradle.kts`.
   - Update `CHANGELOG.md` with the new version section and release notes.
   - Update `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.

2. **Run Quality Checks**:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```

3. **Verify Git Tree & Secrets**:
   - Ensure working tree is clean and no private keys or configs are staged.

4. **Tag the Release**:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

5. **GitHub Release**:
   - The `.github/workflows/release.yml` workflow will automatically build the release APK and attach it to the GitHub Release.
   - Alternatively, draft the release manually on GitHub with the changelog snippet.

6. **F-Droid Distribution**:
   - F-Droid builds Reflex from tagged source commits.
   - Ensure the recipe in `docs/fdroid/com.reflex.productivity.yml` matches the latest tag and version code.

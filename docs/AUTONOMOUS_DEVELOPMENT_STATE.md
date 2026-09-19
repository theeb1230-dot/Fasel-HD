# Fasel HD autonomous development state

Last updated: 2026-09-19

## Repository truth

- Repository: `theeb1230-dot/Fasel-HD`
- Default branch: `main`
- Exact main SHA at run start: `1e8d3b1f657ce77f1f0a178bcba22e65eb7e21b0`.
- Open PR: #1 on `recovery/android-ci-bootstrap`.
- Previous PR head: `75ae81de4b578e3197df400f3b42f22ba4bd7398`.
- CI workflow repair commit: `6eed3488a6fd8377d44679d866b687f3ef4373b1`.

## Reference APK

- File: `FaselhdV20.0.2.apk`
- Expected SHA-256: `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- The APK is not committed to this public repository.

## Current recovery slice

PR #1 contains the Android/Kotlin scaffold, launcher, SafeHttp validation and tests, Media3 HLS/DASH playback dependencies, playback classification, and an Android CI gate for tests, lint, debug APK build, and artifact upload.

## CI evidence

Run `35437554979` failed before Gradle because the runner could not find the `sdkmanager` command. The workflow now uses the hosted runner Android SDK directly, verifies `ANDROID_HOME` and installed platform/build-tool directories, pins `ubuntu-24.04`, and uses checkout v5. The old failed SHA must not be treated as evidence for the repaired head.

## Current gaps

1. CI has not yet reached Gradle compilation/tests on the repaired exact head.
2. No GitHub-built APK has been verified yet.
3. Catalog/Search to Details/Episodes/Sources/Player remains unverified end-to-end.
4. No stable release claim is justified yet.

## أهداف التشغيل التالي

1. Read Actions for the new exact PR head.
2. Fix the first real compile/test/lint failure on PR #1 and add regression coverage where appropriate.
3. Reach green `testDebugUnitTest`, `lintDebug`, and `assembleDebug` on one exact head.
4. Inspect the produced debug APK artifact for structure, package/version, and manifest evidence.
5. Merge PR #1 only after its exact head is green and mergeable, then start the typed catalog/search/details/episodes recovery slice.

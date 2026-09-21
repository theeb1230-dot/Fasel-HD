# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/end main SHA: `32bf410b81e49384a58810b072fab6debaa24b03` (unchanged while PR #18 is pending).
- Single open PR: #18 `recovery/runtime-emulator-smoke`; no second PR may be opened.
- Evaluated PR head at run start: `6a49d3cd957a95f50564a336a39e6112f7a4b0b3`; Android CI run `35539045696`: build SUCCESS, runtime-smoke FAILURE.
- Production corrective commit this run: `8347a974aabbed46ab1e2a23065a1362b4d37600`. This documentation commit advances the head again; newest exact-head CI must pass before merge.

## Reference APK
- File: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- Reference APK was not needed for this Android 15 layout/runtime defect. No recovered secrets may enter source/logs/tests/docs.

## Work completed this run
1. Re-read GitHub truth for PR #18 and exact head, run `35539045696`, jobs and artifacts. Build remained green; only emulator runtime smoke failed.
2. Downloaded and unpacked preserved artifact `runtime-smoke-reports` ID `10613757815` (23,426 bytes; SHA-256 `f0f34318a3012836a14cc96bb96c441b1fa62dee894fe79436d3e35a73a9b0c7`) instead of guessing from job metadata.
3. The new `dumpsys` assertion proved the resumed component after Play was `com.android.launcher3/.uioverrides.QuickstepLauncher`, not PlayerActivity.
4. Logcat identified the root cause immediately around the Play touch: Espresso reports a click on `playButton`, then Launcher logs `LAUNCHER_TASKBAR_HOME_BUTTON_TAP`; MainActivity transitions PAUSED -> STOPPED and Home becomes foreground. There is no app fatal exception and no evidence that PlayerActivity was launched.
5. Re-read the main layout. `playButton` is the bottom-most full-width control. On targetSdk 35 / Android 15, enforced edge-to-edge allows content behind system navigation/taskbar unless insets are handled, making the bottom control overlap the Home hit target on the API 35 emulator.
6. Fixed production `MainActivity` to consume system-bar insets on the content root using `ViewCompat` / `WindowInsetsCompat`, keeping the Play control outside navigation/taskbar hit targets. This addresses the proven UI/runtime defect rather than weakening the test.
7. No provider/resolver/player/network/security semantics changed. No blind rerun was performed.

## Acceptance criteria / blockers
### P0
- P0-1: #18 remains the only open PR; merge only on newest exact-head green build + runtime-smoke and mergeable state.
- P0-2: deterministic Catalog/Search -> Details/Episodes -> Sources -> playback decision -> internal Native Player has integration/build evidence. Emulator runtime acceptance remains OPEN until the system-inset fix passes newest-head smoke.
- P0-3: provider transport/decode/mapping/adapter/pagination and UI loading/content/empty/error/retry/load-more/cancellation are merged and CI verified. Authorized concrete live-provider configuration remains OPEN.
- P0-4: bounded resolver is merged and CI verified; safe HTTPS native candidates only. Runtime resolver evidence remains OPEN.
- P0-5: Media3 player error/retry and position/play-state lifecycle handling are merged and CI/build verified. Actual media decode/playback runtime remains OPEN.
- P0-6: exact-main APK is non-zero and package/version/SDK/integrity/placeholder gate is merged and CI verified. Emulator smoke remains OPEN pending corrected exact-head CI.

### P1
Movies/Series/Anime/Streaming complete live flows, Favorites/History/Resume, Downloads, Settings/Profiles, and full Arabic/RTL/reference parity remain OPEN.

### P2
DNS-rebinding/IPv6 SSRF hardening, performance, dependency/security/license audit, accessibility and maintenance remain OPEN.

## Honest weighted completion
Unmerged #18 changes receive no credit.

| Area | Weight | Evidence-level completion |
|---|---:|---:|
| Build/Gradle/CI + valid Debug APK | 8% | 90% |
| Architecture/domain/models/contracts | 8% | 75% |
| Catalog/Home | 7% | 55% |
| Search | 7% | 55% |
| Details | 7% | 55% |
| Seasons/Episodes | 7% | 55% |
| Sources/provider/pagination | 8% | 75% |
| Resolver | 7% | 55% |
| Native Media3 Player + UI/lifecycle | 10% | 75% |
| End-to-end Catalog/Search->Play integration | 10% | 75% |
| Movies/Series/Anime/Streaming | 5% | 30% |
| Favorites/History/Resume | 4% | 0% |
| Downloads | 3% | 0% |
| Settings/Profiles | 3% | 0% |
| UI/navigation/Arabic-RTL/reference parity | 3% | 30% |
| Runtime/device smoke + edge cases | 2% | 0% |
| Security/privacy/licenses/dependencies | 1% | 55% |

- **Overall Verified Product Completion: 56.4%**.
- **Current P0 Path Completion: 67.7%** (weighted over product areas 1-10).
- **Runtime-Verified Completion: 0.0%** until corrected emulator smoke passes on newest exact PR head and is merged.
- Scores remain unchanged because the production fix is unmerged and has not yet passed exact-head CI.

## CI / artifacts
- Latest evaluated run: `35539045696`, evaluated head `6a49d3cd957a95f50564a336a39e6112f7a4b0b3`; build SUCCESS, runtime-smoke FAILURE.
- Runtime artifact ID `10613757815`, `runtime-smoke-reports`, 23,426 bytes, SHA-256 `f0f34318a3012836a14cc96bb96c441b1fa62dee894fe79436d3e35a73a9b0c7`.
- Exact failure evidence: Android reported Launcher as top/resumed activity; logcat recorded `LAUNCHER_TASKBAR_HOME_BUTTON_TAP` immediately after Espresso touched the bottom `playButton`, followed by MainActivity PAUSED/STOPPED.
- Production system-inset corrective commit: `8347a974aabbed46ab1e2a23065a1362b4d37600`; newest exact-head CI not yet observed at this state update.

## Security / licensing
- Clean-room implementation only; no credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking, or external-browser playback.
- Provider transport disables redirects and bounds timeouts; resolver fails closed and accepts HTTPS native candidates only.
- DNS-resolution/IPv6 SSRF hardening remains a known P2 gap.

## أهداف التشغيل التالي
1. Inspect #18 newest exact-head CI; merge immediately if build and runtime-smoke are green and the PR is mergeable.
2. If runtime-smoke remains red, download the preserved report and fix only the newly proven cause on #18.
3. Once emulator navigation smoke is merged, add deterministic authorized Media3 decode/playback runtime evidence without external-network dependence.
4. Keep authorized live-provider configuration explicit and do not invent protected endpoints, tokens or cookies.

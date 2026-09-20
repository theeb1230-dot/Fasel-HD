# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/end main SHA: `32bf410b81e49384a58810b072fab6debaa24b03` (unchanged while PR #18 is pending).
- Single open PR: #18 `recovery/runtime-emulator-smoke`; no second PR may be opened.
- Evaluated PR head at run start: `0a6fb59b361674baba2c059cd13205ee73fa546b`; Android CI run `35516918098`: build SUCCESS, runtime-smoke FAILURE.
- Corrective test commit this run: `8d1d922f57d91dae572f7d152567d4056906096b`. This documentation commit advances the head again; newest exact-head CI must pass before merge.

## Reference APK
- File: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- Reference APK was not needed for this instrumentation defect. No recovered secrets may enter source/logs/tests/docs.

## Work completed this run
1. Re-read PR #18 and exact head, then fetched the exact runtime-smoke job log for run `35516918098` / job `106094309798`.
2. Confirmed API 35 emulator boot and instrumentation startup are healthy. The single test still fails only with `Timed out waiting for com.faselhd.restored.ui.PlayerActivity.onResume` after the deterministic catalog/details/play path reaches the Play click.
3. Confirmed build job remains green and runtime reports are preserved as artifact ID `10607127008`, 22,798 bytes, SHA-256 `c38757434c12edb4d9bd97c9af46e7f4920b9cdf02d10a10fbd887104856c295`.
4. Re-read `MainActivity`, `PlayerActivity`, and the manifest. Production navigation is an internal explicit activity path; `PlayerActivity` is declared non-exported and no browser playback is introduced.
5. Replaced the fragile in-process `Application.ActivityLifecycleCallbacks` observer with Android system task-state evidence: after Play, the test polls `dumpsys activity activities` via instrumentation `UiAutomation` and requires the resumed component to be `com.faselhd.restored/.ui.PlayerActivity`. Failure output now includes the last resumed-activity state, so the next red run will identify what Android actually considers resumed instead of merely timing out on an observer.
6. No provider/resolver/player/network/security production behavior changed. No blind rerun was performed.

## Acceptance criteria / blockers
### P0
- P0-1: #18 remains the only open PR; merge only on newest exact-head green build + runtime-smoke and mergeable state.
- P0-2: deterministic Catalog/Search -> Details/Episodes -> Sources -> playback decision -> internal Native Player has integration/build evidence. Emulator runtime acceptance remains OPEN until newest-head smoke passes.
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

## CI / artifacts
- Latest evaluated run: `35516918098`, evaluated head `0a6fb59b361674baba2c059cd13205ee73fa546b`; build SUCCESS, runtime-smoke FAILURE.
- Runtime failure: `RuntimeFlowSmokeTest.catalogDetailsSourcesDecisionNavigatesToNativePlayer` -> timeout waiting for `PlayerActivity.onResume`.
- Preserved runtime artifact ID `10607127008`, name `runtime-smoke-reports`, 22,798 bytes, SHA-256 `c38757434c12edb4d9bd97c9af46e7f4920b9cdf02d10a10fbd887104856c295`.
- Corrective system-state assertion commit: `8d1d922f57d91dae572f7d152567d4056906096b`; newest exact-head CI was not yet observed at this state update. No runtime credit is claimed.

## Security / licensing
- Clean-room implementation only; no credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking, or external-browser playback.
- Provider transport disables redirects and bounds timeouts; resolver fails closed and accepts HTTPS native candidates only.
- DNS-resolution/IPv6 SSRF hardening remains a known P2 gap.

## أهداف التشغيل التالي
1. Inspect #18 newest exact-head CI; merge immediately if build and runtime-smoke are green and the PR is mergeable.
2. If runtime-smoke is red, use the new `Last task state` output to identify the actual resumed Android component and fix only the proven cause on #18.
3. Once emulator navigation smoke is merged, add deterministic authorized Media3 decode/playback runtime evidence without external-network dependence.
4. Keep authorized live-provider configuration explicit and do not invent protected endpoints, tokens or cookies.

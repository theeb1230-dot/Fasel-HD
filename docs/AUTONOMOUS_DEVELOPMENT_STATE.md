# Fasel HD autonomous development state

Last updated: 2026-09-20

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/end main SHA: `32bf410b81e49384a58810b072fab6debaa24b03` (unchanged while PR #18 is pending).
- Single open PR: #18 `recovery/runtime-emulator-smoke`; do not open a second PR.
- Latest evaluated exact head: `7310156c23c17dbf7423e40aaa773d97ce709513`, Android CI run `35505226008`: build SUCCESS, runtime-smoke FAILURE.
- Corrective commits this run: `d0aff91529bba3f6544452369854153cff76c40c` (lifecycle callback test) and `b071c1c10d8ad8f592509dfbe772ba633fc50c98` (always-upload instrumentation reports). This documentation commit advances the head again; newest exact-head CI must pass before merge.

## Reference APK
- File: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- Reference APK was not needed for this instrumentation-CI defect. No recovered secrets may enter source/logs/tests/docs.

## Work completed this run
1. Re-read repository/default branch, single open PR #18, its exact head, latest Actions result, artifact metadata, workflow and affected runtime code.
2. Confirmed run `35505226008` on exact head `7310156c...` failed only in runtime-smoke while producing a non-zero `fasel-hd-debug-apk` artifact (7,208,643 bytes, workflow digest `sha256:0b5920823e66490b0bebc0e60d75e149d113a68ba45df21665eb067de685041e`).
3. Re-inspected `MainActivity`, `PlaybackNavigator`, `PlayerActivity` and manifest. The deterministic path starts an explicit non-exported internal `PlayerActivity`; PlayerActivity constructs Media3 during `onStart`.
4. Replaced polling `ActivityLifecycleMonitorRegistry` with an `Application.ActivityLifecycleCallbacks` observer registered before launching MainActivity. The smoke now requires an actual `PlayerActivity.onResume` callback after Play, avoiding lifecycle-monitor polling ambiguity while still proving the real activity launched.
5. Added an `if: always()` CI artifact upload for connected Android test HTML/XML/results. Future runtime failures now preserve exact instrumentation evidence instead of depending on restricted raw Actions-log download endpoints.
6. No production provider/resolver/player/security/network behavior changed.

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
- Latest evaluated run: `35505226008`, head `7310156c23c17dbf7423e40aaa773d97ce709513`; build SUCCESS, runtime-smoke FAILURE.
- APK artifact from that run: `fasel-hd-debug-apk`, 7,208,643 bytes, workflow digest `sha256:0b5920823e66490b0bebc0e60d75e149d113a68ba45df21665eb067de685041e`.
- Corrective runtime observer commit: `d0aff91529bba3f6544452369854153cff76c40c`.
- Diagnostic preservation commit: `b071c1c10d8ad8f592509dfbe772ba633fc50c98`; future runs upload `runtime-smoke-reports` even when instrumentation fails.
- Newest exact-head CI is pending/not yet observed after these corrective commits. No runtime credit is claimed.

## Security / licensing
- Clean-room implementation only; no credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking, or external-browser playback.
- Provider transport disables redirects and bounds timeouts; resolver fails closed and accepts HTTPS native candidates only.
- DNS-resolution/IPv6 SSRF hardening remains a known P2 gap.

## أهداف التشغيل التالي
1. Inspect #18 newest exact-head CI; merge immediately if build and runtime-smoke are green and the PR is mergeable.
2. If runtime-smoke fails, download the new `runtime-smoke-reports` artifact and fix the exact assertion/runtime cause on #18 only; never rerun blindly.
3. Once emulator navigation smoke is merged, add deterministic authorized Media3 decode/playback runtime evidence without external-network dependence.
4. Keep authorized live-provider configuration explicit and do not invent protected endpoints, tokens or cookies.

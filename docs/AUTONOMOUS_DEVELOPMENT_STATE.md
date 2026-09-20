# Fasel HD autonomous development state

Last updated: 2026-09-20

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/end main SHA: `32bf410b81e49384a58810b072fab6debaa24b03` (unchanged while PR #18 is pending).
- Single open PR: #18 `recovery/runtime-emulator-smoke`; do not open a second PR.
- PR #18 failed runtime-smoke at head `31bff746476ac3796b8ba0468c9402f3bc720e64` while the build job was green. The runtime test synchronization fix commit is `fc31055de2c323b9c57fbe22ec464ee4c8474a18`; this documentation commit advances the exact head again, so newest-head CI must pass before merge.

## Reference APK
- File: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- Reference APK was not needed for this runtime-CI defect. No recovered secrets may enter source/logs/tests/docs.

## Work completed this run
1. Re-read GitHub truth and PR #18 exact head. CI run `35494858510` proved the build job green but `runtime-smoke` failed specifically at `Emulator end-to-end smoke`; checkout, Java, Gradle setup and KVM all succeeded.
2. The connector cannot retrieve the job log endpoint, so no unverified exception text is claimed.
3. Inspected the instrumentation test and production flow. `MainActivity` loads catalog/details/sources asynchronously in `lifecycleScope`, while the smoke immediately asserted list/details state. Espresso does not automatically treat arbitrary lifecycle coroutines as an idling resource, creating a real race in the test harness.
4. Fixed the regression on the same PR by replacing first-frame assertions with bounded polling of observable acceptance states (catalog row displayed, episode details rendered, play enabled, PlayerActivity intent observed). Polling is capped at 10 seconds and rethrows unexpected failures; it is not an unbounded retry or blind CI rerun.
5. No production provider/playback/security behavior changed.

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
- Failed evidence: run `35494858510`, head `31bff746476ac3796b8ba0468c9402f3bc720e64`; build SUCCESS, runtime-smoke FAILURE at emulator test step.
- Corrective code commit: `fc31055de2c323b9c57fbe22ec464ee4c8474a18`.
- Newest exact-head CI is pending/not yet observed after the corrective commits. No runtime credit is claimed.

## Security / licensing
- Clean-room implementation only; no credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking, or external-browser playback.
- Provider transport disables redirects and bounds timeouts; resolver fails closed and accepts HTTPS native candidates only.
- DNS-resolution/IPv6 SSRF hardening remains a known P2 gap.

## أهداف التشغيل التالي
1. Inspect #18 newest exact-head CI; merge immediately if build and runtime-smoke are green and the PR is mergeable.
2. If runtime-smoke still fails, obtain the strongest available exact failure evidence and fix the root cause on #18 only; never rerun blindly.
3. Once emulator navigation smoke is merged, add deterministic authorized Media3 decode/playback runtime evidence without external-network dependence.
4. Keep authorized live-provider configuration explicit and do not invent protected endpoints, tokens or cookies.

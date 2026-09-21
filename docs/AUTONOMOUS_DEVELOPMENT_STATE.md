# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Exact run-start/current main SHA: `7c1da0a7f69abc2582eb564ce891ff6f0753d51b`.
- Sole open PR: #27 `recovery/provider-e2e-fixture`.
- PR #27 entered this run at exact head `602a2070bbca46fe8f99dc2ba46571b27b8caeb1`; Android CI run `35653570777` failed in Unit tests while `runtime-smoke` succeeded.
- Root cause was a code/test compile defect: the new provider E2E test referenced nonexistent `PlaybackDecisionEngine`; production exposes `PlaybackPipeline.prepare`.
- Fix commit on the same PR: `e504a0fcfec01305f24489a3ed490f7f6633cfb1`. This handoff commit follows it, so re-read exact PR head before judging CI.

## Reference APK
- Reference: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- APK reinspection was not needed for this compile-defect repair. No recovered endpoint/token/cookie/credential/bypass material was introduced.

## Work completed this run
1. Re-read GitHub truth rather than inheriting the previous handoff: main, sole PR/head, CI jobs/checks and the changed provider test.
2. Confirmed #27 exact-head run `35653570777`: build FAILED at Unit tests; runtime-smoke SUCCESS. Therefore #27 was not merged despite being mergeable.
3. Traced the failure to the new test importing/calling nonexistent `PlaybackDecisionEngine` while the real production decision API is `PlaybackPipeline.prepare`.
4. Fixed the test on the same PR to exercise the actual production playback pipeline, preserving the intended deterministic credential-free chain: HTTPS fixture -> ProviderTransport -> JSON decode -> typed SERIES -> details -> sources -> SafeHttp filtering -> `PlaybackPipeline.prepare` -> Native HLS decision.
5. Kept the negative `javascript:` source so the test still proves unsafe source removal before playback decision. No production security boundary was weakened.

## Acceptance criteria / blockers
### P0
- P0-1: #27 remains OPEN. Acceptance: exact post-fix head must have green Unit/Lint/APK verification/runtime-smoke and remain mergeable, then merge immediately.
- P0-2 Player: deterministic project-owned Media3 fixture reaches READY and advances >=250 ms on emulator; rotation/lifecycle runtime-proven. Physical-device/long-playback remain open.
- P0-3 E2E: deterministic Catalog/Search -> Details/Episodes -> Sources -> native Player runtime is proven separately; #27 adds transport/decode/typed-domain/safe-source/native-decision integration but gets no completion credit until exact-head CI passes.
- P0-4 Provider: transport/mapping/pagination/retry/cancellation and typed Movie/Series/Anime search are merged and CI-proven. External authorized provider runtime remains OPEN.
- P0-5 Resolver: bounded HTTPS decision layer/tests exist; authorized runtime resolver evidence remains OPEN.
- P0-6 APK: last accepted #26 artifact is non-zero and CI-verified; physical-device evidence remains OPEN.

### P1
- Movies/Series/Anime/Streaming independent runtime coverage remains open.
- Favorites/History/Resume, Downloads, Settings/Profiles, full Arabic/RTL/reference parity remain OPEN.

### P2
IPv6/private/link-local/DNS-rebinding hardening, dependency/license audit, accessibility, performance and long-playback edge cases remain OPEN.

## Honest weighted completion
| Area | Weight | Evidence-level completion |
|---|---:|---:|
| Build/Gradle/CI + valid Debug APK | 8% | 90% |
| Architecture/domain/models/contracts | 8% | 75% |
| Catalog/Home | 7% | 90% |
| Search | 7% | 90% |
| Details | 7% | 90% |
| Seasons/Episodes | 7% | 90% |
| Sources/provider/pagination | 8% | 90% |
| Resolver | 7% | 55% |
| Native Media3 Player + UI/lifecycle | 10% | 90% |
| End-to-end Catalog/Search->Play integration | 10% | 90% |
| Movies/Series/Anime/Streaming | 5% | 30% |
| Favorites/History/Resume | 4% | 0% |
| Downloads | 3% | 0% |
| Settings/Profiles | 3% | 0% |
| UI/navigation/Arabic-RTL/reference parity | 3% | 30% |
| Runtime/device smoke + edge cases | 2% | 80% |
| Security/privacy/licenses/dependencies | 1% | 55% |

- **Overall Verified Product Completion: 72.0%**.
- **Current P0 Path Completion: 89.0%**.
- **Runtime-Verified Completion: 32.0%**.
- **Beta Readiness: 70.0%**.
- P0 is held below the prior provisional figure because #27 exact-head build failed; the fix is not credited until its own exact-head CI passes.

## CI / artifacts
- Failed PR #27 head: `602a2070bbca46fe8f99dc2ba46571b27b8caeb1`.
- Android CI run `35653570777`: build FAILURE at Unit tests; runtime-smoke SUCCESS.
- No new APK from the failed build because Lint/APK build/verification/upload were skipped.
- Last accepted artifact remains PR #26 / run `35652043541`: APK 7,213,826 bytes, SHA-256 `3c1d27b1077242a8b4da82c8945b05457595c17c824df43bb10b14a4eed4c403`; runtime reports 42,100 bytes, SHA-256 `ef94094fde5bd71a7a234261b087ecc300527d13288409a142fb318029c0b2c3`.
- Fix commit before this handoff: `e504a0fcfec01305f24489a3ed490f7f6633cfb1`; fetch exact post-handoff head and CI before merge.

## Security / licensing
- Clean-room only. No credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking or external-browser playback.
- SafeHttp, redirect and timeout boundaries remain unchanged.

## ما لا يعمل بعد بصراحة
- PR #27 post-fix exact-head CI is not yet proven green/merged.
- No verified authorized external provider/resolver runtime E2E path yet.
- No physical-device smoke or long-duration playback proof.
- Movies/Series/Anime/Streaming are not all runtime-proven independently.
- Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain incomplete.

## أهداف التشغيل التالي
1. Re-read exact PR #27 head and CI; merge immediately if Unit/Lint/APK/runtime-smoke are green and mergeable. If not, inspect the exact failure and fix the same PR.
2. Extend deterministic provider proof into Android runtime/UI evidence connected to the proven Media3 path.
3. Add authorized external provider/resolver evidence only when an explicitly permitted credential-free source is available.
4. Prove Movies/Series/Anime/Streaming independently before lower-value UI polish.

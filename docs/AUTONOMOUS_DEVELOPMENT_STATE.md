# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/current main SHA: `096f1e481cc225b09cd5590723750dafcf0dd226`.
- One open PR only: #25, branch `recovery/provider-cancellable-transport`.
- Failed exact head: `2aeb2c245c952ad7732f109cecbf708ca8e082c3`; corrective test code head: `6a9c615335129362817432813b3587c34c812f48`.

## Reference APK
- Reference: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- APK reinspection was not required for this isolated transport-cancellation test defect; no recovered endpoint, token, cookie, credential or bypass material was introduced.

## Work completed this run
1. Re-read GitHub truth: main, branches, PR #25, recent commits, handoff and exact-head Actions.
2. Exact-head run `35619346885`: runtime-smoke SUCCESS; build FAILURE only at unit test `ProviderTransportPolicyTest.coroutineCancellationCancelsUnderlyingHttpCall`; 54 tests ran, one failed. Lint/APK steps were skipped after the unit failure.
3. Pulled the build job log rather than rerunning blindly. Production `ProviderTransport` still uses `suspendCancellableCoroutine`, `enqueue()` and `invokeOnCancellation { call.cancel() }` correctly.
4. Root issue was the regression test's indirect polling assertion: it waited for the interceptor worker to observe cancellation rather than asserting the exact captured OkHttp Call state after coroutine cancellation joined.
5. Reworked the test deterministically on the same PR: capture the exact `Call`, cancel and join the coroutine, assert `Call.isCanceled()` directly, then release the blocking interceptor and clean up dispatcher/connection pool. This tests the intended contract without a real provider/network dependency.
6. No production security boundary or transport policy was weakened.

## Acceptance criteria / blockers
### P0
- P0-1: #25 remains OPEN until corrective exact-head Unit/Lint/APK/runtime-smoke are green and the PR is mergeable; then merge immediately and re-read main.
- P0-2 Player: deterministic owned-fixture Media3 playback reaches READY and advances >=250 ms on emulator; rotation/lifecycle are runtime-proven. Physical-device/long-playback remain open.
- P0-3 E2E: deterministic Catalog/Search -> Details/Episodes -> Sources -> decision -> native Player is runtime-proven. Authorized concrete provider/resolver E2E remains OPEN.
- P0-4 Provider: contracts/transport/mapping/pagination/loading-error-empty/retry exist; bounded retry is merged. #25 targets active cancellation of in-flight HTTP work; corrective test awaits CI. Authorized concrete provider runtime evidence remains OPEN.
- P0-5 Resolver: bounded HTTPS decision layer exists/tests pass; authorized runtime resolver evidence remains OPEN.
- P0-6 APK: merged main has build/metadata/secret hygiene and emulator evidence. #25 has no accepted APK artifact until corrective CI passes.

### P1
Movies/Series/Anime/Streaming independent runtime coverage; Favorites/History/Resume; Downloads; Settings/Profiles; full Arabic/RTL/reference parity remain OPEN.

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
- **Current P0 Path Completion: 88.5%**.
- **Runtime-Verified Completion: 32.0%**.
- **Beta Readiness: 70.0%**.
- No new completion credit this run: #25 corrective head is not yet exact-head CI-proven/merged. The failed unit assertion is treated as a test defect, not accepted product evidence.

## CI / artifacts
- Failed PR #25 exact head: `2aeb2c245c952ad7732f109cecbf708ca8e082c3`.
- Android CI run `35619346885`: build FAILURE at Unit tests; runtime-smoke SUCCESS.
- Corrective test code head: `6a9c615335129362817432813b3587c34c812f48`; this handoff update advances the branch again, so always fetch the PR exact head before judging CI.
- No accepted new Debug APK artifact from failed build because APK build/upload steps were skipped.

## Security / licensing
- Clean-room only. No credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking or external-browser playback.
- SafeHttp/redirect/timeouts remain unchanged; cancellation is lifecycle/resource cleanup only.

## ما لا يعمل بعد بصراحة
- PR #25 corrective head is not yet CI-proven/merged.
- No verified authorized concrete provider/resolver E2E path yet.
- No physical-device smoke or long-duration playback proof.
- Movies/Series/Anime/Streaming are not all runtime-proven independently.
- Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain incomplete.

## أهداف التشغيل التالي
1. Inspect exact-head CI for #25; merge immediately if Unit/Lint/APK/runtime-smoke are green and mergeable. If it fails, use the exact log and fix on the same branch.
2. After merge, build deterministic authorized provider HTTP-boundary evidence covering transport -> decode -> domain plus loading/error/empty/retry/cancellation, without recovered hosts/secrets.
3. Connect provider evidence to the proven Catalog/Search -> native Media3 path and then prove bounded resolver runtime behavior.
4. Prove Movies/Series/Anime/Streaming independently before UI polish.

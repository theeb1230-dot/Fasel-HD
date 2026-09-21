# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/current main SHA: `096f1e481cc225b09cd5590723750dafcf0dd226`.
- One open PR only: #25, branch `recovery/provider-cancellable-transport`.
- Failed exact head inspected this run: `9b930d7804c5117012fd1d815a227731ff75a3aa`; corrective code commit: `22e524648dce60c7020313b9ab39a22969269ffc`.

## Reference APK
- Reference: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- APK reinspection was not required for this isolated provider cancellation-test scheduling defect. No recovered endpoint, token, cookie, credential or bypass material was introduced.

## Work completed this run
1. Re-read default branch, exact main, all branches, open PR and exact head, CI jobs/logs, handoff and provider transport/test code.
2. Exact-head Android CI run `35639026336`: runtime-smoke SUCCESS; build FAILURE at `testDebugUnitTest`. 54 tests ran, one failed: `ProviderTransportPolicyTest.coroutineCancellationCancelsUnderlyingHttpCall` at line 31.
3. Root cause: the test launched `async` with the inherited `runBlocking` event loop and then synchronously blocked the same thread in `CountDownLatch.await`; the child coroutine therefore had no guaranteed chance to reach `ProviderTransport.get()`/`enqueue()` before the assertion timeout. This was a test scheduling defect, not evidence that runtime transport cancellation failed.
4. Fixed the same PR without blind rerun: the cancellation proof now uses `async(start = CoroutineStart.UNDISPATCHED)`, which deterministically executes through `enqueue()` until the fake Call leaves the coroutine suspended. The test then cancels and joins, and verifies `cancel()` on the exact captured Call.
5. Production `ProviderTransport` remains unchanged: SafeHttp normalization, asynchronous `Call.Factory.newCall(...).enqueue(...)`, `invokeOnCancellation { call.cancel() }`, no redirects, and existing timeout policy remain intact.
6. No security boundary, provider endpoint, credential, resolver or playback policy was weakened.

## Acceptance criteria / blockers
### P0
- P0-1: #25 remains OPEN until the corrective exact head has green Unit/Lint/APK/runtime-smoke and is mergeable; merge immediately then re-read main.
- P0-2 Player: deterministic owned-fixture Media3 reaches READY and advances >=250 ms on emulator; rotation/lifecycle are runtime-proven. Physical-device/long-playback remain open.
- P0-3 E2E: deterministic Catalog/Search -> Details/Episodes -> Sources -> decision -> native Player is runtime-proven. Authorized concrete provider/resolver E2E remains OPEN.
- P0-4 Provider: contracts/transport/mapping/pagination/loading-error-empty/retry exist; bounded retry is merged. #25 targets active cancellation of in-flight HTTP work; corrected deterministic regression awaits exact-head CI. Authorized concrete provider runtime evidence remains OPEN.
- P0-5 Resolver: bounded HTTPS decision layer exists/tests pass; authorized runtime resolver evidence remains OPEN.
- P0-6 APK: merged main has build/metadata/secret hygiene and emulator evidence. #25 has no accepted new APK until its build passes.

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
- No new completion credit yet because the corrective head is not CI-proven/merged.

## CI / artifacts
- Failed PR #25 exact head: `9b930d7804c5117012fd1d815a227731ff75a3aa`.
- Android CI run `35639026336`: runtime-smoke SUCCESS; build FAILURE only at unit tests; 54 tests, 1 failure. Lint/APK steps were skipped after the unit-test failure, so there is no accepted new APK artifact from this run.
- Corrective code commit: `22e524648dce60c7020313b9ab39a22969269ffc`; fetch PR exact head after this handoff commit before judging CI.

## Security / licensing
- Clean-room only. No credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking or external-browser playback.
- SafeHttp/redirect/timeouts remain unchanged; injected Call.Factory remains a testability seam defaulting to the production OkHttpClient.

## ما لا يعمل بعد بصراحة
- PR #25 corrective head is not yet CI-proven/merged.
- No verified authorized concrete provider/resolver E2E path yet.
- No physical-device smoke or long-duration playback proof.
- Movies/Series/Anime/Streaming are not all runtime-proven independently.
- Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain incomplete.

## أهداف التشغيل التالي
1. Inspect exact-head CI for #25; merge immediately if Unit/Lint/APK/runtime-smoke are green and mergeable. If it fails, inspect exact logs and fix the same PR.
2. After merge, add deterministic authorized provider HTTP-boundary evidence covering transport -> decode -> domain plus loading/error/empty/retry/cancellation, without recovered hosts/secrets.
3. Connect provider evidence to the proven Catalog/Search -> native Media3 path and prove bounded resolver runtime behavior.
4. Prove Movies/Series/Anime/Streaming independently before lower-value UI polish.

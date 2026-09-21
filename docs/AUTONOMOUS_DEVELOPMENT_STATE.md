# Fasel HD autonomous development state

Last updated: 2026-09-21

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start main SHA: `4de6b94ab341cefbf67a20ed43783e2d55cfe524`.
- PR #24 exact head `759ab734b5e678d4871bcb8a661f007c9c788af1` passed Android CI run `35612607463` and was squash-merged.
- End/current main SHA after merge: `096f1e481cc225b09cd5590723750dafcf0dd226`.
- One open PR only: #25, branch `recovery/provider-cancellable-transport`; code head before this handoff update `9502eae91e19284e573fde70999f432d07268350`.

## Reference APK
- Reference: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- APK reinspection was not required for this provider-lifecycle slice; no recovered host/token/cookie/credential was introduced.

## Work completed this run
1. Re-read GitHub truth and verified #24 exact-head CI was fully green and mergeable, then merged it immediately.
2. Re-read main at `096f1e48...`; bounded exponential retry for transient provider page failures is now merged, with permanent fail-fast, max attempts and coroutine-cancellation regression coverage.
3. Identified the next P0 provider lifecycle gap: `ProviderTransport.get()` used blocking `Call.execute()` inside `Dispatchers.IO`; cancelling the coroutine did not actively cancel the underlying in-flight OkHttp call.
4. Opened only PR #25 from exact main and replaced blocking execute with OkHttp `enqueue()` bridged through `suspendCancellableCoroutine`.
5. Wired coroutine cancellation directly to `Call.cancel()`, while preserving SafeHttp normalization, disabled redirects, timeouts and credential-free requests.
6. Added a deterministic unit regression test with an in-process OkHttp interceptor proving coroutine cancellation is observed by the underlying HTTP call. No real network/provider is contacted by the test.

## Acceptance criteria / blockers
### P0
- P0-1: #24 CLOSED/MERGED. #25 is the only open PR and must pass exact-head CI + be mergeable before merge.
- P0-2 Player: deterministic owned-fixture playback reaches READY and advances >=250 ms on emulator; rotation/lifecycle are runtime-proven. Physical-device/long-playback remain open.
- P0-3 E2E: deterministic Catalog/Search -> Details/Episodes -> Sources -> decision -> native Player is runtime-proven. Authorized concrete provider/resolver E2E remains OPEN.
- P0-4 Provider: contracts/transport/mapping/pagination/loading-error-empty/retry exist; bounded retry is merged. #25 closes active cancellation of in-flight HTTP work if CI proves it. Authorized concrete provider runtime evidence remains OPEN.
- P0-5 Resolver: bounded HTTPS decision layer exists/tests pass; authorized runtime resolver evidence remains OPEN.
- P0-6 APK: build/metadata/secret hygiene and emulator smoke are proven. Physical-device evidence remains OPEN.

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
- #24 improves verified P0 reliability but does not justify raising the weighted provider category above 90% without authorized runtime evidence. #25 receives no completion credit until exact-head CI passes.

## CI / artifacts
- PR #24 green run: `35612607463`; merged main `096f1e481cc225b09cd5590723750dafcf0dd226`.
- Latest previously verified Debug APK/runtime evidence remains from the merged deterministic Media3 run; #25 must produce its own exact-head build artifact before merge.
- PR #25 code head before handoff update: `9502eae91e19284e573fde70999f432d07268350`; CI had not appeared at the observation point.

## Security / licensing
- Clean-room only. No credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking or external-browser playback.
- Provider transport remains fail-closed through SafeHttp; redirects remain disabled; cancellation now targets resource cleanup rather than bypass behavior.

## ما لا يعمل بعد بصراحة
- PR #25 is not yet exact-head CI-proven/merged.
- No verified authorized concrete provider/resolver E2E path yet.
- No physical-device smoke or long-duration playback proof.
- Movies/Series/Anime/Streaming are not all runtime-proven independently.
- Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain incomplete.

## أهداف التشغيل التالي
1. Inspect exact-head CI for #25; if failed, fix from logs on the same branch; if green and mergeable, merge immediately and re-read main.
2. Build deterministic authorized provider HTTP-boundary runtime evidence without recovered endpoints/secrets, covering transport -> decode -> domain and loading/error/empty/retry/cancellation.
3. Connect that provider evidence to the already-proven Catalog/Search -> native Media3 runtime path, then add bounded resolver runtime evidence.
4. Prove Movies/Series/Anime/Streaming independently before lower-value UI polish.

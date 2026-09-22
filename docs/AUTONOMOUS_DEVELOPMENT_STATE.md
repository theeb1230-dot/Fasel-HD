# Fasel HD autonomous development state

Last updated: 2026-09-22

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Exact run-start/end main SHA: `0618f69acba38e9c4936b876588848942fa7f5ef` (merged PR #31).
- The only open PR is #32 `recovery/player-lifecycle-runtime`; exact code head before this handoff update: `bebf3759547fa5cb6ffe91e0286f1c0bf92fbe1d`.
- PR #32 is mergeable and not draft. No second PR was opened.

## Reference APK
- Reference: `FaselhdV20.0.2.apk`; expected SHA-256 `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- No endpoint/token/cookie/credential/bypass material was recovered or introduced in this run.

## Work completed this run
1. Re-read main, the only open PR, exact head and exact-head Actions rather than inheriting prior state.
2. PR #32 head `1646dc64c2815ea89621ad448e95dc9ebcd70acc` ran Android CI `35747785601`: build job fully SUCCESS (Unit tests, Lint, Debug APK, APK verification, artifact upload); runtime-smoke FAILED.
3. Downloaded and inspected the preserved runtime-smoke report artifact instead of guessing. Root cause is explicit: `PlayerLifecycleRuntimeTest` throws `NullPointerException: Cannot run onActivity since Activity has been destroyed already`.
4. Confirmed the cause in production flow: the lifecycle test supplied `file://...`, while `PlayerActivity` constructs `PlaybackRequest` and intentionally fails closed for non-production-safe input, so the activity finishes before the test's `onActivity` call.
5. Fixed the test on the same PR without weakening SafeHttp/PlaybackRequest: it now uses the same public credential-free Shaka HTTPS HLS demo already used by the provider runtime smoke, and `PlaybackKind.HLS`. Removed the local file fixture and all test-only bypass pressure. Commit: `bebf3759547fa5cb6ffe91e0286f1c0bf92fbe1d`.

## Acceptance criteria / blockers
### P0
- Deterministic/project-controlled provider fixture path through transport/decode/domain/details/sources/SafeHttp/decision/UI/Media3: CLOSED by merged evidence.
- Authorized concrete external provider/resolver E2E: OPEN; no invented endpoint, recovered credential, static cookie or bypass permitted.
- Movies/Series/Anime/Streaming independent runtime proof: CLOSED by merged #29 evidence.
- Player lifecycle: implementation exists; #32 runtime acceptance remains OPEN until exact-head CI is fully green and merged. Physical-device and long-playback remain OPEN.
- Provider production quality and resolver external runtime evidence remain OPEN where no authorized concrete endpoint exists.
- APK: #32 prior head produced a verified Debug APK, but #32 is not merge-creditable until runtime-smoke is green.

### P1
Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain OPEN pending reference-backed implementation/evidence.

### P2
Literal IPv4/IPv6 and DNS-rebinding hardening are merged. Dependency/license audit, accessibility, performance and long-playback edge cases remain OPEN.

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
| Movies/Series/Anime/Streaming | 5% | 90% |
| Favorites/History/Resume | 4% | 0% |
| Downloads | 3% | 0% |
| Settings/Profiles | 3% | 0% |
| UI/navigation/Arabic-RTL/reference parity | 3% | 30% |
| Runtime/device smoke + edge cases | 2% | 80% |
| Security/privacy/licenses/dependencies | 1% | 90% |

- **Overall Verified Product Completion: 76.5%**.
- **Current P0 Path Completion: 97.0%**.
- **Runtime-Verified Completion: 51.0%**.
- **Beta Readiness: 79.5%**.
- No percentage increase is granted to #32 until exact-head runtime CI succeeds and the PR is merged.

## CI / artifacts
- PR #32 head `1646dc64c2815ea89621ad448e95dc9ebcd70acc`, Android CI `35747785601`: build SUCCESS; runtime-smoke FAILED only.
- `fasel-hd-debug-apk`: 7,215,698 bytes; artifact digest `sha256:a600014f17990b6ffa71502e6bf5d7a8d56038c1fbde3f37ffe4d58582f84bdc`.
- `runtime-smoke-reports`: 76,790 bytes; digest `sha256:a4bdd787d54e4eebb20221aebb30ff75e353403a8d380befac0f5b5b079a2c05`; report identified the destroyed-Activity NPE above.
- Corrective lifecycle-test commit: `bebf3759547fa5cb6ffe91e0286f1c0bf92fbe1d`; exact-head workflow had not appeared at the last check.

## Security / licensing
- Clean-room only. No credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking or external-browser playback.
- SafeHttp/PlaybackRequest remain fail-closed; the lifecycle test was changed to respect the production HTTPS boundary rather than bypass it.

## ما لا يعمل بعد بصراحة
- No verified authorized concrete external provider/resolver runtime E2E path yet.
- No physical-device smoke or long-duration playback proof.
- PR #32 lifecycle evidence is not credited until corrected exact-head runtime-smoke is green and merged.
- Favorites/History/Resume, Downloads, Settings/Profiles and full Arabic/RTL/reference parity remain incomplete.

## أهداف التشغيل التالي
1. Re-read #32 exact head after this handoff commit and its exact-head Actions; merge immediately only when build + runtime-smoke are green and the head is mergeable.
2. If runtime still fails, download the preserved report artifact and fix the exact root cause on this same branch; no blind rerun and no production security weakening.
3. Keep the authorized concrete provider/resolver E2E blocker explicit unless a credential-free permitted source is available.
4. Continue physical-device/long-playback evidence when an actual device environment is available.
5. Restore reference-backed P1 functions only after confirming them from reference evidence.

# Fasel HD autonomous development state

Last updated: 2026-09-20

## Repository truth
- Repository: `theeb1230-dot/Fasel-HD`; default branch `main`.
- Run-start/current main SHA: `5a76b88d171037c80c4cb5ed357d7f5742a63c05`.
- Single open PR: #12 `recovery/provider-adapter-tests`.
- PR #12 prior exact head `a7cb99b0baaa878a87d4c430aff3555756b81b22` was mergeable but Android CI run `35475652976` failed Unit tests: 38 tests executed, one `ConfiguredContentProviderTest > initializationError` with `InvalidTestClassError`.
- Root cause: `blankSearchIsRejectedBeforeTransport` used an expression-bodied `runBlocking`; Kotlin inferred the domain return type, so JUnit 4 did not see a valid void test method. Fixed on the same PR branch by giving the test a block body and invoking `runBlocking` inside it.
- Code/test fix commit: `232f6568c0a30ff71dc657dccbc5246e39090098`; newest exact head includes the subsequent documentation commit and must pass CI before merge/credit.

## Reference APK
- File: `FaselhdV20.0.2.apk`.
- Expected SHA-256: `c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`.
- Reference APK is not committed; no recovered secrets may enter source/logs/tests/docs.

## Work completed this run
1. Re-read GitHub truth and kept PR #12 as the only open PR.
2. Retrieved the previously unavailable full Actions job log instead of rerunning blindly.
3. Isolated the exact Unit-test failure to JUnit test-class initialization, not provider transport/runtime behavior.
4. Fixed the invalid JUnit signature on the same PR branch; no production behavior or security boundary was weakened.
5. Retained deterministic integration coverage for pagination mapping, malformed-payload non-retryability, HTTP 503 retryability, and blank-search fail-fast behavior.
6. No recovered host/token/cookie/credential, access bypass, ads/tracking or external-browser playback was added.

## Acceptance criteria / blockers
### P0
- P0-1: #12 is the only open PR. Merge remains BLOCKED until newest exact-head Android CI is green and mergeable.
- P0-2 deterministic Catalog/Search -> Details/Episodes -> Sources -> playback decision -> internal Native Player path: BUILD/CI VERIFIED on main; device runtime remains OPEN.
- P0-3 provider transport + decode + mapping + ContentProvider adapter: MERGED/CI VERIFIED through #11. #12 integration regression suite has a root-cause fix pending exact-head CI. Authorized concrete configuration and UI pagination/loading/retry/cancellation remain OPEN.
- P0-4 bounded internal resolver: OPEN; only ResolverRequired decision exists.
- P0-5 Player: build/CI verified; runtime error/retry/rotation/background behavior OPEN.
- P0-6 APK/runtime: CI builds APK; fresh metadata/device smoke remains OPEN.

### P1
Movies/Series/Anime/Streaming complete live flows, Favorites/History/Resume, Downloads, Settings/Profiles, and full Arabic/RTL/reference parity remain OPEN.

### P2
DNS-rebinding/IPv6 SSRF hardening, performance, dependency/security/license audit, accessibility and maintenance remain OPEN.

## Honest weighted completion
Scores are recomputed from merged/exact-head CI evidence only. #12 receives no completion credit until its newest exact head passes CI.

| Area | Weight | Evidence-level completion |
|---|---:|---:|
| Build/Gradle/CI + valid Debug APK | 8% | 90% |
| Architecture/domain/models/contracts | 8% | 75% |
| Catalog/Home | 7% | 55% |
| Search | 7% | 55% |
| Details | 7% | 55% |
| Seasons/Episodes | 7% | 55% |
| Sources/provider/pagination | 8% | 55% |
| Resolver | 7% | 30% |
| Native Media3 Player + UI/lifecycle | 10% | 75% |
| End-to-end Catalog/Search->Play integration | 10% | 75% |
| Movies/Series/Anime/Streaming | 5% | 30% |
| Favorites/History/Resume | 4% | 0% |
| Downloads | 3% | 0% |
| Settings/Profiles | 3% | 0% |
| UI/navigation/Arabic-RTL/reference parity | 3% | 30% |
| Runtime/device smoke + edge cases | 2% | 0% |
| Security/privacy/licenses/dependencies | 1% | 55% |

- **Overall Verified Product Completion: 53.1%**.
- **Current P0 Path Completion: 61.0%**.
- **Runtime-Verified Completion: 0.0%**.
- No score increase: the CI root-cause fix is not credited until newest exact-head CI succeeds; no emulator/device evidence exists.

## CI / artifacts
- Main remains `5a76b88d...` from the last verified merge.
- PR #12 Android CI run `35475652976` on prior exact head `a7cb99b0...`: FAILURE in Unit tests; 38 tests, 1 initialization failure. Lint/build/artifact were skipped.
- Same-branch root-cause fix committed as `232f6568...`; newest exact-head CI pending.
- Build evidence is not runtime evidence.

## Security / licensing
- Clean-room implementation only; no credentials/API tokens/signing secrets/persistent cookies.
- No DRM/CAPTCHA/paywall/access-control bypass, ads/tracking, or external-browser playback.
- Provider transport validates endpoints, disables redirects and bounds timeouts.
- Provider decode caps payload size and fails closed on malformed data.
- Further DNS-resolution/IPv6 SSRF hardening remains a known P2 gap.

## أهداف التشغيل التالي
1. Inspect PR #12 newest exact-head CI; if green and mergeable, merge immediately and re-read main.
2. If CI fails, retrieve the exact job log and fix the root cause on the same branch with regression coverage; never blind-rerun.
3. After #12 merge, add UI/repository pagination state with explicit loading/empty/error/retry/cancellation around the authorized configurable provider boundary.
4. Implement bounded internal resolver only for ResolverRequired with SafeHttp/timeouts/cancellation/lifecycle restrictions and no bypass.
5. Add player error/retry/background/rotation coverage and fresh APK metadata/runtime smoke before any runtime or Stable/Golden claim.

# Fasel HD Recovery

Clean-room recovery of the owner's Android application from the preserved `FaselhdV20.0.2.apk` binary.

## Recovery rules

- GitHub is the source of truth for the recovered source tree.
- Do not commit credentials, API tokens, signing secrets, or persistent session cookies recovered from the binary.
- Preserve third-party license and notice obligations.
- Native Android Media3 playback is preferred for HLS, DASH, and direct media.
- WebView is restricted to bounded resolver duties when necessary; no DRM, CAPTCHA, paywall, or access-control bypass.
- A feature is not considered restored until it builds and is covered by appropriate tests/runtime evidence.

## Reference binary

Expected SHA-256:
`c06ab7a983414c831019a455f2002d0ea1841d9fb5a5efe95b099cec9439a712`

The APK itself is intentionally not committed to this public repository.

## Status

Repository initialized for migration of the recovered Android source and CI evidence. See `docs/AUTONOMOUS_DEVELOPMENT_STATE.md` for the current handoff.

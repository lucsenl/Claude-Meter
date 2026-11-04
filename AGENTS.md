# AGENTS.md

## Repository scope

Claude Meter is **Android phone-only**. Do not recreate iOS, Wear OS, SwiftUI, WidgetKit,
Xcode or companion watch modules without an explicit change in project scope.

| Path | Stack / purpose |
|---|---|
| Repository root | Docs, changelog, CI entrypoints and Android convenience wrappers |
| `android/app/` | Android phone app |
| `android/shared/` | Shared Java models and logic |
| `android/tests/` | Pure-Java regression tests |
| `docs/` | Claude migration evidence and contracts |

The app has no application backend. Credentials remain on-device using Android Keystore-backed
storage.

## Claude migration rule

This fork migrates the original Android interface to Claude Meter. Never perform blind product/provider
renaming in protocol code.

- User/product terminology should follow verified Claude terminology.
- Provider/company terminology should use Anthropic where appropriate.
- Network endpoints, OAuth client IDs, headers, persisted keys and protocol claims are contracts,
  not branding strings. Change them only together with a verified Claude implementation.
- Do not map unsupported reset-credit redemption to Claude Extra Usage.
- Keep historical changelog facts intact.

See `docs/CLAUDE_UPSTREAM_MAPPING.md` and `docs/CLAUDE_TERMINOLOGY_EVIDENCE.md`.

## Release channels & etiquette

- `main`: current simplified baseline until PR #4 passes acceptance.
- `fix/restore-original-android-ui`: full Android UI recovery branch.
- GitHub Actions workflow is **manual-only** (`workflow_dispatch`). Do not add `push`, PR, or tag triggers.
- Keep feature changes in PRs; do not merge unverified Android builds into `main`.
- Never create or push `v*` tags or publish/edit GitHub releases unless explicitly asked.
- Never bump `versionCode` or `versionName` unless explicitly preparing a release.

Root wrappers:

- `./run-tests.sh` -> `android/run-tests.sh`
- `./lint.sh` -> `android/lint.sh`
- `./build.sh` -> `android/build.sh`

## Toolchain

- JDK 21 for Android CI and local builds.
- Android SDK platform 36 for the phone.
- Gradle wrapper is committed under `android/`.
- OneUI/SESL dependencies are vendored under `android/vendor/m2`; GitHub Packages credentials are
  only required when refreshing missing dependencies.

## Validation

For Android changes run:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

The pure-Java test suite is the fast correctness check. Full Android builds verify only the phone APK, including bundled Samsung widget resources.
Live OAuth login, token refresh, Samsung lock widget behavior and visual parity still require
device validation; do not claim that a successful compilation proves them.

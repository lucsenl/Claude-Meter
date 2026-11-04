# AGENTS.md

## Repository scope

Claude Meter is Android-only. Do not add or recreate iOS, SwiftUI, WidgetKit, Xcode, App Store,
or Apple-platform code unless the repository owner explicitly reverses this decision.

| Path | Stack / purpose |
|---|---|
| Repository root | Docs, changelog, CI entrypoints and Android convenience wrappers |
| `android/app/` | Android phone app |
| `android/shared/` | Shared Java models and logic |
| `android/wear/` | Wear OS companion |
| `android/tests/` | Pure-Java regression tests |
| `docs/` | Claude migration evidence and contracts |

The app has no application backend. Credentials remain on-device using Android Keystore-backed
storage.

## Claude migration rule

This fork is being migrated from Claude Meter to Claude. Never perform blind product/provider
renaming in protocol code.

- User/product terminology should follow verified Claude terminology.
- Provider/company terminology should use Anthropic where appropriate.
- Network endpoints, OAuth client IDs, headers, persisted keys and protocol claims are contracts,
  not branding strings. Change them only together with a verified Claude implementation.
- Do not emulate Claude-only reset-credit behavior as Claude usage credits.
- Keep historical changelog facts intact.

See `docs/CLAUDE_UPSTREAM_MAPPING.md` and `docs/CLAUDE_TERMINOLOGY_EVIDENCE.md`.

## Release channels & etiquette

- `main`: stable.
- `alpha`: rapid iteration when the branch exists.
- Target `alpha` for experimental feature work when available unless the owner directs otherwise.
- Never create or push `v*` tags or publish/edit GitHub releases unless explicitly asked.
- Never bump `versionCode` or `versionName` unless explicitly preparing a release.

Root wrappers:

- `./run-tests.sh` -> `android/run-tests.sh`
- `./lint.sh` -> `android/lint.sh`
- `./build.sh` -> `android/build.sh`

## Toolchain

- JDK 17+; CI currently uses JDK 21.
- Android SDK platform 36 for phone and 37.0 for Wear.
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

The pure-Java test suite is the fast correctness check. Full Android builds validate phone and Wear
artifacts. No Apple-platform validation is part of this repository.

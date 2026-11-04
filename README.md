# Claude Meter

Claude Meter is an Android-only fork focused on showing Claude usage information on phones,
Samsung One UI surfaces, and Wear OS.

The project is being migrated from the original Codex Meter implementation to Claude/Anthropic.
Provider-specific behavior is changed only when the Claude equivalent is supported by evidence;
legacy network contracts are removed as their real replacements are implemented.

## Repository layout

| Path | Purpose |
|---|---|
| `android/app/` | Android phone app, dashboard, settings, notifications, widgets and Samsung lock/AOD integration |
| `android/shared/` | Shared Java models and logic used by phone and Wear |
| `android/wear/` | Wear OS companion, tiles and complications |
| `android/tests/` | Pure-Java regression tests |
| `docs/` | Claude migration contracts and terminology evidence |

There is no application backend. Credentials and cached usage state stay on-device.

## Android features retained

- Phone dashboard and usage history
- Home-screen widgets
- Samsung One UI lock-screen / AOD widgets where supported
- Notifications and reset-time alerts
- Optional live usage monitor / Now Bar integration
- Wear OS companion, tiles and complications
- Android Keystore encrypted credential storage
- Local diagnostics, settings transfer and update channels

Features that only existed for iOS have been removed from this fork.

## Build from source

Requirements:

- JDK 17 or newer
- Android SDK Platforms 36 and 37.0
- Android Build Tools 36.x
- `ANDROID_SDK_ROOT` or `ANDROID_HOME`
- `GH_USERNAME` and `GH_ACCESS_TOKEN` with `read:packages` only when the vendored
  OneUI dependencies are insufficient and Gradle must fetch them

From the repository root:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

The root scripts forward to the Android project. Build artifacts are written under
`android/dist/`.

See `android/README.md` for Android-specific module and build details.

## Release channels

- `main`: stable channel
- `alpha`: rapid-iteration / prerelease channel when present

Creating a matching `v*` tag triggers the Android release workflow for the phone and Wear APKs.
Do not create release tags as part of normal development work.

## Claude migration

The migration rules are documented in:

- `docs/CLAUDE_UPSTREAM_MAPPING.md`
- `docs/CLAUDE_TERMINOLOGY_EVIDENCE.md`

The central rule is that Claude-specific names, limits, authentication behavior and provider
contracts must be grounded in actual Anthropic/Claude behavior rather than obtained through blind
string replacement.

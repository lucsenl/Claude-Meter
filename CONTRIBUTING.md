# Contributing

Claude Meter is an Android-only project.

- Android phone code lives in `android/app/`.
- Phone-side shared Java models live in `android/shared/`.
- Pure-Java regression tests live in `android/tests/`.
- Shared documentation, release notes and CI entrypoints live at the repository root.

Do not add iOS, Wear OS, Xcode, SwiftUI, or companion watch code to this fork.

Keep changes focused, update tests when behavior changes, and never commit credentials, tokens or
generated build artifacts.

## Local setup

Install JDK 21 and Android SDK platform 36/build tools. Set
`ANDROID_SDK_ROOT` or `ANDROID_HOME`.

The OneUI-Design dependencies are vendored under `android/vendor/m2`. If Gradle needs to refresh
packages from GitHub Packages, export `GH_USERNAME` and a `GH_ACCESS_TOKEN` with
`read:packages`.

From the repository root:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

See `android/README.md` for module-specific details.

## Provider migration discipline

The fork adapts its original Android UI for Claude Meter. Branding changes and protocol changes are separate:

- use Claude for verified product/account terminology;
- use Anthropic for verified provider/company terminology;
- do not rename endpoints, OAuth identifiers, headers or stored protocol keys until their actual
  Claude replacements are implemented;
- do not fabricate Claude reset-credit redemptions; Extra Usage is separately reported spending;
- preserve historical changelog entries as historical facts.

## Release channels

`main` is stable. `alpha`, when present, is the rapid-iteration prerelease channel.

Do not create release tags, publish releases or bump Android version fields unless the repository
owner explicitly asks for release preparation.

## GitHub Actions

GitHub Actions builds are manual-only (`workflow_dispatch`). Do not restore automatic
push/PR/tag triggers. Run the Android workflow against the recovery branch for
pre-merge validation; only distribute an APK after tests, lint, signature verification,
and a real-device sign-in check. Temporary signing keys are not valid for stable upgrades.

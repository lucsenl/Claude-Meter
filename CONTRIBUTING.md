# Contributing

Claude Meter is an Android-only project.

- Android phone code lives in `android/app/`.
- Shared phone/Wear logic lives in `android/shared/`.
- Wear OS code lives in `android/wear/`.
- Pure-Java regression tests live in `android/tests/`.
- Shared documentation, release notes and CI entrypoints live at the repository root.

Do not add iOS/Xcode/SwiftUI/WidgetKit code to this fork.

Keep changes focused, update tests when behavior changes, and never commit credentials, tokens or
generated build artifacts.

## Local setup

Install JDK 17 or newer and the required Android SDK platforms/build tools. Set
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

The fork is moving from Codex Meter to Claude. Branding changes and protocol changes are separate:

- use Claude for verified product/account terminology;
- use Anthropic for verified provider/company terminology;
- do not rename endpoints, OAuth identifiers, headers or stored protocol keys until their actual
  Claude replacements are implemented;
- do not translate Codex reset credits into Claude usage credits;
- preserve historical changelog entries as historical facts.

## Release channels

`main` is stable. `alpha`, when present, is the rapid-iteration prerelease channel.

Do not create release tags, publish releases or bump Android version fields unless the repository
owner explicitly asks for release preparation.

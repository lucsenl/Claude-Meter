# Claude Meter for Android

Android **phone-only** Gradle project retaining the original app's full One UI
dashboard, history, settings, home-screen widgets, and Samsung lock-screen/AOD
widgets. There are **no Wear OS or iOS modules** in this repository.

## Modules

| Path | Role |
|---|---|
| `app/` | Phone application (`dev.lucsenl.claudemeter` installation ID) |
| `shared/` | Java usage models shared by phone screens and widgets |
| `tests/` | Pure-Java regression and OAuth protocol tests |
| `vendor/m2/` | Vendored One UI / SESL Maven artifacts |

The Java namespace (`dev.bennett.claudemeter`) is a historical internal
implementation detail; the Android installation ID is
`dev.lucsenl.claudemeter`.

## Build

From `android/`:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Requires JDK 21, Android SDK platform 36, Android Build Tools 36.x and
`ANDROID_SDK_ROOT` or `ANDROID_HOME`. The vendored Maven repository contains
the One UI dependencies; `GH_USERNAME` / `GH_ACCESS_TOKEN` may be needed for
optional GitHub Packages resolution.

A successful signed APK is copied into `android/dist/`, along with the SHA-256
checksum. Without a persistent keystore, generated signatures cannot be used
for in-place upgrades across different builds.

See the repository root `README.md` for OAuth, release and validation notes.

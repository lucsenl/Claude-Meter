# Claude Meter for Android

Phone-only Android project for Claude Meter.

## Layout

| Path | Role |
|---|---|
| `app/` | Android app (`dev.lucsenl.claudemeter`) |
| `tests/` | Pure-Java parser/source checks |
| `vendor/m2/` | Cached One UI / SESL Maven artifacts |

There is no Wear OS, iOS, backend, or private Claude authentication module.

## Build and test

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Requirements: JDK 17+, Android SDK Platform 36, Build Tools 36.x, and
`ANDROID_SDK_ROOT` or `ANDROID_HOME`.

Signed local APKs are written to `android/dist/`.

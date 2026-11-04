# Claude Meter

Claude Meter is a phone-only Android app for monitoring Claude subscription usage.

## Sign-in

Claude Meter uses browser-based Claude OAuth with PKCE. The user signs in to Claude, copies the
authorization result shown by the callback page, and pastes it back into the app. Access and
refresh tokens are encrypted locally with Android Keystore. No API key is required.

## What it shows

- Claude account/profile and subscription plan
- 5-hour session utilization and reset
- 7-day allowance utilization and reset
- model-scoped weekly limits when returned by Claude
- whether extra usage is enabled
- a compact home-screen widget with 5h and 7d percentages

The subscription usage/profile endpoints are not a stable public third-party API and may change.
Claude Meter is deliberately defensive about missing and newly-added fields.

## Android

- package: `dev.lucsenl.claudemeter`
- minimum Android: API 26
- target/compile SDK: 36
- interface: One UI / SESL
- credential storage: Android Keystore
- platform: Android phone only

## Build

Requirements:

- JDK 17+
- Android SDK Platform 36
- Android Build Tools 36.x
- `ANDROID_SDK_ROOT` or `ANDROID_HOME`

From the repository root:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Artifacts are written to `android/dist/`.

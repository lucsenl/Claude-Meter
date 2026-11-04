# Claude Meter

Claude Meter is an Android phone app for viewing documented Anthropic API analytics.

The app uses only public Anthropic API contracts. It does not sign in to Claude.ai, impersonate
Claude Code, or use private subscription-quota endpoints.

## What it shows

With an Anthropic credential that has Analytics access:

- token usage today
- token usage over the last 7 days
- request counts
- API cost over the last 30 days

With a regular Anthropic API key:

- connection status
- models available to that key

Claude subscription session/weekly allowance percentages are not exposed through the public
third-party API, so Claude Meter does not fabricate them.

## Android

Claude Meter is phone-only. There is no iOS or Wear OS module.

- package: `dev.lucsenl.claudemeter`
- minimum Android: API 26
- target/compile SDK: 36
- interface: One UI / SESL
- credential storage: Android Keystore
- widget: today + 7-day token totals

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

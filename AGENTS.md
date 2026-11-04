# AGENTS.md

## Scope

Claude Meter is a phone-only Android app.

## Project

- Android app: `android/app/`
- package: `dev.lucsenl.claudemeter`
- tests: `android/tests/`
- UI: One UI / SESL
- provider: documented Anthropic API
- credential storage: Android Keystore

## Provider rules

Use documented Anthropic APIs only.

A regular API key may provide model access without organization Analytics permissions. In that
case the app must show Analytics as unavailable rather than invent usage percentages.

Claude subscription allowance is not a supported third-party metric and must not be represented
as API analytics.

## Validation

Run:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Do not publish releases or tags unless explicitly requested.

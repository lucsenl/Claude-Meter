# AGENTS.md

## Scope

Claude Meter is a phone-only Android app.

Do not add iOS, Wear OS, Claude Code impersonation, private Claude.ai endpoints, browser-session
cookies, or subscription-quota scraping.

## Project

- Android app: `android/app/`
- package: `dev.lucsenl.claudemeter`
- tests: `android/tests/`
- UI: One UI / SESL
- public provider: Anthropic API
- credential storage: Android Keystore

## Provider rules

Only documented Anthropic APIs may be used.

A regular API key may provide model access without organization Analytics permissions. In that
case the app must show Analytics as unavailable rather than invent usage percentages.

Claude subscription session/weekly allowance is not a supported third-party metric and must not be
represented as API analytics.

## Validation

Run:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Do not publish releases or tags unless explicitly requested.

# AGENTS.md

## Scope

Claude Meter is a phone-only Android app for Claude subscription usage.

## Project

- Android app: `android/app/`
- package: `dev.lucsenl.claudemeter`
- tests: `android/tests/`
- UI: One UI / SESL
- credential storage: Android Keystore

## Authentication and usage contract

The product is login-only. Do not introduce API-key onboarding.

The OAuth flow uses authorization-code + PKCE. Store access/refresh tokens only in the encrypted
credential store. Never log credentials, authorization codes, PKCE verifiers, or refresh tokens.

Usage comes from the Claude subscription OAuth profile/usage surfaces. Parse unknown/missing fields
defensively because these endpoints can change.

Do not spoof another application's User-Agent or identity.

## Validation

Run:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Do not publish releases or tags unless explicitly requested.

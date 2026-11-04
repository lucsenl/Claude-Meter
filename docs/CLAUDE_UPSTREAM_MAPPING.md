# Claude Meter API contract

Claude Meter uses documented Anthropic APIs only.

## Supported data

- Models available to the supplied API key
- Organization usage analytics when the credential has Analytics permission
- Organization cost analytics when the credential has Analytics permission

## Unsupported data

Claude subscription allowance percentages are not exposed through the supported third-party API.
Claude Meter therefore does not display or estimate those values.

## Authentication

The user supplies an Anthropic API key and, when required, a workspace ID. The credential is
encrypted locally using Android Keystore and is sent only to Anthropic API endpoints.

## Platform scope

Phone-only Android application.

# Claude Meter API contract

Claude Meter uses documented Anthropic APIs only.

## Supported data

- Models available to the supplied API key
- Organization usage analytics when the credential has Analytics permission
- Organization cost analytics when the credential has Analytics permission

## Unsupported data

Claude subscription session/weekly allowance percentages are not exposed through the supported
third-party API surface. Claude Meter therefore does not display or estimate those values.

## Authentication

The user supplies an Anthropic API key. It is encrypted locally using Android Keystore and is never
sent anywhere except Anthropic API endpoints.

## Platform scope

Android phone only. No Wear OS, iOS, backend service, browser OAuth, or private Claude.ai API.

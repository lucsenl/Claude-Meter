# Claude Meter subscription contract

## Authentication

- Authorization Code + PKCE (S256)
- Claude browser sign-in
- OAuth access + refresh tokens
- local encrypted storage with Android Keystore
- automatic refresh before expiry and once after an HTTP 401

No API key is requested or stored.

## Usage

Claude Meter reads the account profile and subscription usage windows:

- 5-hour session utilization and reset time
- 7-day utilization and reset time
- model-scoped weekly limits when present
- subscription/profile metadata
- extra-usage enabled state

Unknown fields are ignored. Missing windows render as unavailable instead of failing the whole
snapshot.

## Platform

Phone-only Android application.

# Claude upstream contract

Claude Meter is an Android-only usage monitor for Claude account limits.

## Product scope

- Android phone dashboard
- usage history and pace analytics
- home-screen widgets
- Samsung One UI lock-screen / AOD surfaces where supported
- notifications and reset-time alerts
- optional live usage monitor / Now Bar
- Wear OS companion, tiles and complications
- encrypted on-device credential storage

## Provider contracts

### Authorization

The browser authorization flow uses PKCE with an independent random state.
Current interoperable implementations consistently use:

- authorization: `https://claude.ai/oauth/authorize`
- token exchange / refresh: `https://platform.claude.com/v1/oauth/token`
- client id: `9d1c250a-e61b-44d9-88ed-5944d1962f5e`

This is a provider contract, not branding copy. Any future endpoint change must be verified before code is changed.

### Profile

- `GET https://api.anthropic.com/api/oauth/profile`
- bearer authorization
- `anthropic-beta: oauth-2025-04-20`
- `Accept: application/json`

Use profile data for account identity and plan labels. Never infer a plan from quota size.

### Usage

- `GET https://api.anthropic.com/api/oauth/usage`
- bearer authorization
- `anthropic-beta: oauth-2025-04-20`
- `Accept: application/json`

Consumed percentages are reported through `utilization` and scoped `limits[].percent` values. Preserve provider reset timestamps instead of manufacturing window lengths from them.

Recognized quota families include:

- `five_hour`: session allowance when present
- `seven_day`: all-model weekly allowance
- model-scoped weekly allowances such as Fable, Opus and Sonnet when returned
- future valid provider buckets, retained dynamically rather than discarded

### Usage credits

The provider may expose `extra_usage` with fields such as enabled state, monthly limit, used credits, utilization and currency. Treat this as overage/spend information and keep it separate from quota windows.

## Data model

The app should model provider buckets directly:

- session quota
- weekly all-model quota
- named scoped quotas
- unknown future quotas
- optional extra usage
- profile identity stored separately from usage

No UI surface should parse raw provider JSON independently.

## UI behavior

- Missing buckets are omitted, never fabricated.
- Fixed-slot widgets prioritize session, weekly, next reset, then configured scoped quotas.
- History and pace analytics use only real Claude snapshots.
- New provider buckets must not require an app release just to survive parsing.
- Stale data remains distinguishable from live data.

## Acceptance criteria

1. A fresh Android install can authorize a Claude account.
2. Account identity and plan labels come from the profile response.
3. Usage values match the provider usage response for the same account.
4. Session, weekly, scoped limits and extra usage render when present.
5. Missing limits are not synthesized.
6. Unknown valid buckets remain available to the data model.
7. Widgets, Samsung surfaces, Wear, alerts and history consume the same Claude-native snapshot.
8. No unrelated provider endpoints are contacted.
9. Tests cover authorization refresh, profile parsing, flat quota windows, scoped limits, missing session limits, extra usage and malformed payloads.

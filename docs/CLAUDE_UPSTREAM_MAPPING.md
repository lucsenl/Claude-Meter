# Claude Meter — provider contract and migration checklist

Claude Meter preserves the original Android phone UX and maps its data to Claude
account limits. Provider endpoints and implementation details must not be
conflated with public guarantees: **live authorization, account usage, and
vendor-specific widgets require real-device validation before release**.

## Scope

- Android phone dashboard, usage history and pace analytics
- Home-screen widgets, Samsung One UI lock-screen / AOD widgets (where supported)
- Notifications, reset-time alerts and optional Now Bar
- Encrypted on-device credential storage, local preferences and history

**Out of scope:** iOS and Wear OS. No API-key setup, no hosted backend.

## Browser OAuth, not API keys

The Android implementation initiates a browser-based authorization-code flow
with PKCE S256 and an independent short-lived random state. The user pastes the
resulting `code#state` into the app; the pasted state is verified before token
exchange. Implemented values:

- Authorization: `https://claude.com/cai/oauth/authorize`
- Token exchange and refresh: `https://platform.claude.com/v1/oauth/token`
- Redirect: `https://platform.claude.com/oauth/code/callback`
- Client ID: `9d1c250a-e61b-44d9-88ed-5944d1962f5e`
- Scopes: `user:profile user:inference`

These values are tied to Claude client behavior, not to Claude API-key plans;
their compatibility must be confirmed through real authorization. Do not
substitute unrelated endpoints or claim that an untested flow is supported.

## Profile and usage

The current implementation requests:

- `GET https://api.anthropic.com/api/oauth/profile`
- `GET https://api.anthropic.com/api/oauth/usage`
- `Authorization: Bearer <OAuth access token>`
- `anthropic-beta: oauth-2025-04-20`

The parser recognizes five-hour and seven-day utilization, dynamically returned
model/scoped quotas, and optional `extra_usage`. It preserves provider reset
timestamps and never invents missing quotas or a monetary conversion from quota
percentages. The user's plan label must come from profile evidence, not an
inferred quota size. Pro is not Pro 20x: use the OAuth organization\'s
`organization_type` and, for `claude_max` only, `rate_limit_tier` to
distinguish Max from the renamed Ultra tier. Hide unknown plans rather than
guessing from capability flags or quota percentages.

Reset-credit **redemption** has no verified Claude subscription equivalent and
is disabled; **Extra Usage** represents separately reported overage/spend, not
replenishment credits.

## Acceptance checks before a final APK

1. OAuth browser login, state verification and token refresh work on a device.
2. Account profile and usage are correctly associated with that login.
3. Real reported limits match the Claude account, including missing buckets.
4. Widgets, Samsung lock widgets, alerts and local usage history use the same
   snapshot without inventing unsupported metrics.
5. Regression tests, Android lint, APK build, signature verification and
   installation complete successfully.
6. All UI labels, URLs, icons, credits and themes respect Claude Meter identity.
7. GitHub Actions remains manual-only; no iOS/Wear artifacts are built.

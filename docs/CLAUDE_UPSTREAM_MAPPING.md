> Scope: this fork is Android-only. iOS/SwiftUI/WidgetKit code has been removed and is not part of the migration.

# Claude upstream mapping

This document defines how every Codex Meter product surface maps to Claude/Claude Code.
It is the migration contract for the Claude Meter fork. The rule is strict: keep a feature
only when Claude exposes a real equivalent or the feature can be derived locally from real
Claude data. Do not emulate Codex-only behavior.

## Upstream contracts

### Authentication
- Authorization: `https://claude.ai/oauth/authorize`
- OAuth client id: `9d1c250a-e61b-44d9-88ed-5944d1962f5e`
- Token exchange / refresh: `POST https://platform.claude.com/v1/oauth/token`
- PKCE: S256, independent random `state`
- App scope baseline:
  `user:profile user:inference user:sessions:claude_code user:mcp_servers user:file_upload`
- Credentials remain local and encrypted by the platform client.
- Claude does not provide the OpenAI device-code flow used by the current iOS client. Both
  platforms therefore converge on browser OAuth + PKCE.

### Profile
- `GET https://api.anthropic.com/api/oauth/profile`
- Headers:
  - `Authorization: Bearer <access token>`
  - `anthropic-beta: oauth-2025-04-20`
  - `Accept: application/json`
- Use the returned account identity and plan flags where available.
- Do not infer a plan from quota size.

### Usage
- `GET https://api.anthropic.com/api/oauth/usage`
- Headers:
  - `Authorization: Bearer <access token>`
  - `anthropic-beta: oauth-2025-04-20`
  - `Accept: application/json`
- `utilization` and scoped `limits[].percent` are consumed percentages.
- Preserve `resets_at` exactly as reported; do not derive the window length from the reset time.
- Known top-level/scoped buckets include:
  - `five_hour`: current session allowance
  - `seven_day`: weekly all-model allowance
  - `seven_day_fable` / Fable scoped weekly allowance
  - `seven_day_opus` when returned
  - `seven_day_sonnet` when returned
  - other provider-added windows: retain as dynamic additional limits
- Prefer scoped `limits[]` entries over legacy flat model keys when both describe the same bucket.
- Unknown valid windows must remain visible as additional limits rather than being discarded.

### Usage credits / overage
- Claude's `extra_usage` is the equivalent product concept.
- Preserve `is_enabled`, `monthly_limit`, `used_credits`, `utilization`, and `currency`.
- This is spend/overage, not a Codex purchased-credit balance and not a reset-credit inventory.

## Product-surface mapping

| Existing Codex Meter feature | Claude Meter target | Action |
|---|---|---|
| ChatGPT OAuth | Claude OAuth + PKCE | Replace |
| iOS OpenAI device-code login | Claude browser OAuth + PKCE | Replace |
| JWT ChatGPT account extraction | Claude profile endpoint | Replace |
| 5-hour meter | Claude `five_hour` | Keep |
| Weekly meter | Claude `seven_day` | Keep |
| Free-tier monthly Codex meter | No fixed Claude equivalent | Remove hardcoded monthly semantics; support dynamic upstream windows |
| Additional model limits | Claude scoped/flat weekly model limits | Keep and generalize |
| Purchased usage-credit balance | Claude `extra_usage` | Replace semantics/UI |
| Reset-credit inventory | No direct Codex-style equivalent | Remove |
| Reset-credit redemption | No direct Codex-style equivalent | Remove |
| Reset-credit expiry reminders | No direct equivalent | Remove |
| Low-remaining alerts | Derived from Claude usage | Keep |
| Reset-time alerts | Derived from Claude `resets_at` | Keep |
| Unexpected refill detection | Derived locally from Claude utilization/reset | Keep, rename away from Codex wording |
| Usage history | Local history of Claude buckets | Keep |
| Usage pace / burn analytics | Local analytics from Claude history | Keep |
| Adaptive refresh | Local policy based on Claude usage/reset | Keep |
| Live usage monitor | Claude usage snapshots | Keep |
| Android Live Update / Now Bar | Claude usage snapshots | Keep |
| Home-screen widgets | Claude buckets | Keep |
| Samsung lock/AOD widgets | Claude buckets | Keep |
| Wear OS tiles/complications | Claude buckets | Keep |
| Next-reset widget/helper | Earliest future Claude `resets_at` | Keep |
| Plan badge | Claude profile plan | Replace source |
| Codex plan value/pricing estimates | Not an upstream Claude usage contract | Remove unless rebuilt from an explicit current Claude pricing source |
| Settings import/export | Local app feature | Keep; migrate schema |
| Diagnostic logs/export | Local app feature | Keep; remove OpenAI-specific fields/messages |
| GitHub app update channels | Local distribution feature | Keep |
| Alpha/stable release flow | Repository feature | Keep |
| Android Keystore storage | Local security feature | Keep |
| iOS Keychain storage | Local security feature | Keep |
| No backend / direct provider calls | Same architecture | Keep |

## Data-model migration

The old core model is shaped around:
- `fiveHour`
- `weekly`
- `monthly`
- `additionalLimits`
- `usageCredits`
- `resetCreditsAvailable`

This Android-only fork must instead model Claude around provider buckets:
- canonical primary buckets: session and weekly-all-model
- named scoped buckets: Fable/Opus/Sonnet/etc.
- unknown future buckets retained dynamically
- optional `extraUsage`
- profile stored separately from usage

Compatibility fields may remain temporarily while Android surfaces migrate, but the
provider response must be parsed once into a Claude-native snapshot. No UI should parse raw
Anthropic JSON independently.

## Android migration

1. Replace OpenAI constants, originator, account-id header and ChatGPT backend routes.
2. Replace OAuth exchange/refresh flow with Claude OAuth JSON requests.
3. Add profile fetch and make it the identity/plan source.
4. Replace `UsageParser` with a Claude parser supporting flat and scoped quota shapes.
5. Replace reset-credit and purchased-credit concepts with `extra_usage`.
6. Remove reset-credit activities, actions, notifications and widget meters.
7. Make dashboard/model-limit ordering data-driven.
8. Keep history, pace, adaptive refresh, alerts, widgets, Samsung surfaces and Wear by feeding
   them Claude-native windows.
9. Rename user-facing Codex/OpenAI/ChatGPT copy and update diagnostic labels.
10. Keep package/application ids stable during the functional port unless a separate package
    migration is explicitly chosen; renaming ids is a release/install decision, not a provider
    migration requirement.

## Widgets, Wear and notifications

### Keep
- session remaining/used
- weekly remaining/used
- named model-specific weekly limits when the surface has room
- next reset
- stale/fresh state
- low-remaining threshold alerts
- reset-time notifications
- live monitor/Now Bar
- history-derived pace indicators

### Remove
- reset-credit count
- reset-credit redemption controls
- reset-credit expiry alerts
- any text claiming OpenAI/ChatGPT/Codex owns the data

### Dynamic behavior
- A missing `five_hour` bucket is valid (for example, plans/accounts where that limit is absent);
  never synthesize a 5-hour meter.
- Additional provider buckets must not break older widgets. Surfaces with fixed slots should show
  session, weekly, next reset, and then selected scoped limits by configuration.
- The app must tolerate new Claude quota buckets without requiring an app release.

## Historical data and migration safety

- Existing Codex history must not be merged into Claude history as if it were the same provider.
- Existing OpenAI credentials must not be sent to Anthropic.
- On first Claude build, either namespace provider-specific caches or clear incompatible Codex
  snapshots/history after explicit migration logic.
- Settings that are provider-neutral can be preserved.
- Settings for removed features (reset credits, Codex monthly free-tier behavior) are ignored and
  eventually deleted through schema migration.

## Explicit non-goals

- Do not scrape claude.ai HTML when OAuth APIs provide the data.
- Do not estimate quotas from local token logs when the upstream usage endpoint is available.
- Do not invent absolute token/message quotas from utilization percentages.
- Do not expose API-key billing as if it were subscription-plan usage.
- Do not emulate Codex reset credits.
- Do not hardcode today's model set as a closed enum.

## Acceptance criteria

The port is complete only when:
1. A fresh Android install can authenticate directly with Claude.
2. Profile identity/plan comes from Claude.
3. Usage values match Claude Settings > Usage for the same account within refresh timing.
4. Session, weekly, scoped model limits and Extra Usage render correctly when present.
5. Missing limits are omitted rather than fabricated.
6. New/unknown valid upstream quota buckets survive parsing and are displayable.
7. All remaining alerts, history, pace, widgets, Samsung surfaces and Wear surfaces operate only
   on Claude data.
8. No OpenAI/ChatGPT network endpoint is contacted.
9. No reset-credit UI/action remains.
10. Tests contain Claude payload fixtures covering legacy flat buckets, scoped limits, missing
    five-hour windows, Extra Usage, malformed data, token refresh and profile parsing.

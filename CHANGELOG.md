# Changelog

## 0.1.0 — 2026-11-04

### Added

- Initial Claude Meter phone-only Android application under `dev.lucsenl.claudemeter`.
- Anthropic API credential onboarding with encrypted Android Keystore storage.
- Optional workspace ID support for multi-workspace API keys.
- Documented Anthropic Models and Usage/Cost Analytics integration.
- Dashboard cards for token usage today, token usage over the last 7 days, request counts,
  30-day API cost, and available models.
- Compact home-screen widget showing today and 7-day token totals.
- One UI / SESL interface with system, light, and dark themes.
- Independent Claude Meter launcher artwork using the Anthropic-inspired palette.

### Product boundary

Claude subscription allowance percentages are not exposed through the supported third-party API.
Claude Meter does not estimate or fabricate them.

Organization usage and cost analytics require a credential with the appropriate Analytics/Admin
permission. A regular API key can still validate the API connection and list available models.

### Platform scope

Android phone only. No watch companion, iOS client, backend, browser-session authentication,
private Claude.ai endpoints, or subscription-quota scraping.

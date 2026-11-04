# Changelog

## 0.1.0 — 2026-11-04

### Added

- Initial Claude Meter Android phone application under the new package `dev.lucsenl.claudemeter`.
- Anthropic credential onboarding with encrypted Android Keystore storage.
- Documented Anthropic API integration for model discovery and Usage/Cost Analytics.
- Dashboard cards for token usage today, token usage over the last 7 days, request counts, 30-day API cost, and available models.
- Compact Android home-screen widget showing today and 7-day token totals.
- One UI / SESL interface with system, light, and dark theme selection.
- Independent Claude Meter launcher artwork.

### Changed

- Rebuilt the inherited Codex Meter codebase around documented Anthropic API contracts instead of private OpenAI/Codex endpoints.
- Treated Claude Meter as a new application/version line rather than an in-place upgrade of the original package.

### Removed

- OpenAI/Codex authentication, quota, reset-credit, reset-timer, usage-pace, live-monitor, and updater behavior.
- Claude.ai / Claude Code private endpoint emulation.
- Wear OS, Data Layer synchronization, tiles, complications, and the Wear APK.
- Shared phone/watch module and legacy Samsung lock-screen/AOD quota surfaces.

### Product boundary

Claude Pro/Max personal session and weekly allowance percentages are not exposed through Anthropic's documented third-party API. Claude Meter does not fabricate those values. Organization usage and cost analytics require a credential with the appropriate Analytics/Admin permission.

### Attribution

Claude Meter is based on the MIT-licensed Codex Meter project. The original license and attribution are preserved in the repository.

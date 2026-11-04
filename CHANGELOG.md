# Changelog

## 0.1.0 — 2026-11-04

### Added

- Claude Meter phone-only Android application under `dev.lucsenl.claudemeter`.
- Browser-based Claude OAuth login with PKCE.
- Encrypted access/refresh token storage using Android Keystore.
- Automatic OAuth token refresh.
- Claude profile and subscription plan display.
- 5-hour and 7-day utilization with reset times.
- Model-scoped weekly limits when present in Claude's usage response.
- Extra-usage status.
- Compact home-screen widget showing 5h and 7d percentages.
- One UI / SESL interface with system, light, and dark themes.
- Independent Claude Meter launcher artwork.

### Compatibility

The subscription OAuth profile/usage endpoints can change independently of this app. Parsing is
forward-tolerant and the app surfaces authentication/rate-limit/endpoint failures explicitly.

### Platform

Android phone only.

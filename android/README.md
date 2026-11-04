# Claude Meter — Android

The Android phone app owns Claude OAuth credentials and usage refresh. Wear OS receives
only usage snapshots through the Wear Data Layer.

## Authentication

The app uses Claude's public OAuth client with PKCE and the localhost callback on port
53692. Tokens are stored with Android Keystore AES-GCM under a Claude-specific storage
namespace, so an in-place install cannot silently reuse old Codex/OpenAI credentials.

## Usage mapping

The app reads Anthropic's OAuth usage response directly:

- `five_hour.utilization` → 5-hour meter.
- `seven_day.utilization` → weekly / 7-day meter.
- scoped entries under `limits` → additional model limits.
- `resets_at` → the existing reset countdown and scheduling model.

Codex-only reset credits are disabled for Claude.

## Checks

From this directory:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

Or use the repository-root wrappers. The pure-Java self-tests include a Claude usage
fixture and do not require an Android device.

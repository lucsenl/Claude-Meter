# Claude Meter

Claude Meter is an unofficial open-source Android and Wear OS client for viewing the
usage allowance attached to a signed-in Claude account. It is based on the Codex Meter
project and keeps its mature widgets, history, alerts, Samsung One UI surfaces, and Wear
sync while replacing the Android authentication and usage transport with Claude.

## Migration status

| Path | Status | Notes |
| --- | --- | --- |
| `android/` | **Claude-enabled** | OAuth + 5-hour / 7-day usage, widgets, history, alerts and Wear sync |
| `ios/` | **Migration pending** | Still contains the inherited Codex/OpenAI implementation; do not treat it as a Claude client yet |

Internal Java package names, some file/class names, and release artifact names still use
`codexmeter` for compatibility during the migration. They are implementation details and
do not mean the Android app still talks to OpenAI.

## How Android reads Claude usage

1. The phone opens Anthropic's Claude OAuth authorization flow with PKCE.
2. The callback is received on the registered localhost callback
   `http://localhost:53692/callback`.
3. Access and refresh tokens are encrypted with Android Keystore and remain on-device.
4. Usage is read directly from `https://api.anthropic.com/api/oauth/usage` using the
   OAuth beta contract.
5. Claude's `five_hour` and `seven_day` utilization windows are mapped onto the
   existing meter model. Model-scoped weekly limits are exposed as additional limits.

There is no application relay server. The Wear app receives only the cached usage
snapshot from the phone; it never receives the OAuth token.

Claude does not expose Codex-style reset credits, so the Android port does not request
those endpoints and clears inherited reset-credit cache state.

> **Compatibility note:** the Claude OAuth usage route and response fields are not a
> documented stable public SDK contract. Anthropic can change them, so authentication
> and parsing are deliberately isolated behind the existing client/parser boundary.

## Android features retained

- Five-hour and seven-day allowance meters with reset countdowns.
- Usage history, burn-rate/pace analytics, alerts, and live monitor.
- Android home-screen widgets.
- Samsung lock-screen / AOD and Now Bar integrations where supported.
- Wear OS app, Tiles, complications, and phone-to-watch refresh.
- Encrypted local credential storage.
- In-app update discovery from this repository.

## Build Android

Requirements remain the same as the inherited project:

- JDK 17 or newer.
- Android SDK Platforms 36 and 37.0.
- Android Build Tools 36.x.

From the repository root:

```bash
./run-tests.sh
./build.sh
```

The One UI / SESL dependencies are vendored for the normal offline build path. See
[`android/README.md`](android/README.md) for Android-specific commands.

## iOS

The `ios/` tree is intentionally left untouched in this first Claude migration. Its
current device-code authentication and backend are OpenAI-specific and need a separate
Claude-native sign-in design rather than a branding-only change.

## Attribution and disclaimer

This fork retains the MIT-licensed work and attribution of the original Codex Meter
project. Claude and Anthropic are trademarks of Anthropic PBC. OpenAI, ChatGPT and Codex
are trademarks of OpenAI. This project is not affiliated with or endorsed by Anthropic,
OpenAI, or the original upstream maintainers.

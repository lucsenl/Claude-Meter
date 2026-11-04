# Claude Meter

Claude Meter is an **Android-phone-only** adaptation of the original usage-monitor
interface for a Claude account. The Android dashboard, history, notifications,
home-screen widgets, and Samsung One UI lock-screen/AOD widgets are preserved.
There is **no iOS or Wear OS app** in this fork.

This is an independent, unofficial client. It is not affiliated with or endorsed
by Anthropic.

## Status: Android recovery preview (v0.2.1)

The restored Android interface is in [pull request #4](https://github.com/lucsenl/claude-meter/pull/4)
until its build and real-device checks pass. An implementation in source control
is **not** evidence that account sign-in or every Samsung widget works on a phone.

- Claude account sign-in uses browser authorization with a PKCE code verifier
  and state; paste the returned `code#state` into the app. **No API key is
  requested or stored.**
- Usage data is requested with the resulting OAuth token. Claude usage endpoints
  may change, and live login/refresh still require validation with a real account.
- Quotas shown depend on what the provider actually returns; unsupported
  reset-credit redemption is not offered.
- Tokens are stored encrypted with Android Keystore; cached history stays local.
- Existing full Android One UI layouts, home-screen widgets, and Samsung
  lock-screen/AOD widget implementations remain in the project.

## Repository layout

| Path | Purpose |
|---|---|
| `android/app/` | Android phone dashboard, login, settings, widgets, Samsung lock/AOD |
| `android/shared/` | Shared Java data models and formatting |
| `android/tests/` | Pure-Java regression tests |
| `docs/` | Provider mapping and terminology evidence |

## Build locally

Requires JDK 21, Android SDK platform 36, Build Tools 36.x and
`ANDROID_SDK_ROOT` or `ANDROID_HOME`.

```bash
./run-tests.sh
./lint.sh
./build.sh
```

The root wrappers delegate to `android/`. A successful build creates
`android/dist/ClaudeMeter-<version>.apk` and `SHA256SUMS.txt`.

**Signing:** without a persistent release keystore, local/CI builds can be
signed using a freshly generated key. APKs signed with different keys cannot
upgrade each other in place. Keep the signing key secure for any distributable
release; do not treat a temporary CI artifact as a stable upgrade package.

## GitHub Actions

Builds are **manual only**. Open
[Build Claude Meter Android APK](https://github.com/lucsenl/claude-meter/actions/workflows/build-apk.yml),
select **Run workflow**, then select the desired branch. Pushes, pull requests,
and tag creation do not trigger builds.

The workflow uploads the Android APK and checksum only after regression tests,
Android lint and APK signature verification pass. Release publication is a
separate step requiring a version tag and the persistent release-signing secrets.

## Provider and authorship

See `docs/CLAUDE_UPSTREAM_MAPPING.md` and
`docs/CLAUDE_TERMINOLOGY_EVIDENCE.md`. Upstream and dependency attribution is
retained in the application's About screen. Unsupported services must not be
fabricated merely to preserve an old UI card.

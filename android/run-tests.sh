#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
JSON_JAR="${JSON_JAR:-}"

if [[ -z "$JSON_JAR" ]]; then
  for candidate in /mnt/data/toolcache/json-20250517.jar "$ROOT/build/test-libs/json-20250517.jar"; do
    if [[ -f "$candidate" ]]; then JSON_JAR="$candidate"; break; fi
  done
fi

if [[ -z "$JSON_JAR" ]]; then
  CACHE="$ROOT/build/test-libs"
  mkdir -p "$CACHE"
  JSON_JAR="$CACHE/json-20250517.jar"
  if [[ -n "${CAAS_ARTIFACTORY_MAVEN_REGISTRY:-}" && -n "${CAAS_ARTIFACTORY_READER_USERNAME:-}" ]]; then
    curl -fsSL -u "${CAAS_ARTIFACTORY_READER_USERNAME}:${CAAS_ARTIFACTORY_READER_PASSWORD}" \
      "https://${CAAS_ARTIFACTORY_MAVEN_REGISTRY}/org/json/json/20250517/json-20250517.jar" \
      -o "$JSON_JAR"
  else
    curl -fsSL "https://repo1.maven.org/maven2/org/json/json/20250517/json-20250517.jar" -o "$JSON_JAR"
  fi
fi

OUT="$ROOT/build/tests"
rm -rf "$OUT" && mkdir -p "$OUT"

javac -encoding UTF-8 -cp "$JSON_JAR" -d "$OUT" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageWindow.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageCredits.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageLimit.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/DashboardSections.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/HistorySections.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/WidgetMeters.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageSnapshot.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageSample.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageHistory.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsagePace.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/PlanPricing.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/UsageStats.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/AdaptiveRefreshPolicy.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/NowBarAutoStart.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/NowBarDisplayMode.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/NowBarPercentMode.java" \
  "$ROOT/shared/src/main/java/dev/bennett/claudemeter/NowBarCopy.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageParser.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/CelebrationDetector.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/RateLimitResetCredit.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ResetCreditsSnapshot.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ResetCreditExpiryReminder.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/Pkce.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/JwtClaims.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/WidgetOptions.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/LockWidgetOptions.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/OnboardingFlow.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/OAuthBrowserPage.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ReleaseVersion.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/GitHubReleaseSource.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/GitHubRelease.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/GitHubReleaseParser.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/UpdateChannel.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ReleaseIntegrity.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ReleaseNotesMarkdown.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ReleaseUpdatePolicy.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/UpdateCheckFrequency.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/DiagnosticSanitizer.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/SettingsTransfer.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ClaudeOAuthProtocol.java" \
  "$ROOT/app/src/main/java/dev/bennett/claudemeter/ClaudeAccountProfile.java" \
  "$ROOT/tests/ClaudeAccountProfileTest.java" \
  "$ROOT/tests/ClaudeOAuthProtocolTest.java" \
  "$ROOT/tests/ParserSelfTest.java"

java -ea -cp "$OUT:$JSON_JAR" dev.bennett.claudemeter.ParserSelfTest
java -ea -cp "$OUT:$JSON_JAR" dev.bennett.claudemeter.ClaudeOAuthProtocolTest
java -ea -cp "$OUT:$JSON_JAR" dev.bennett.claudemeter.ClaudeAccountProfileTest

# Claude Meter source-contract checks.
grep -q 'VERSION_NAME = "0.2.1"' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
grep -q 'VERSION_CODE = 3' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
grep -q 'applicationId = "dev.lucsenl.claudemeter"' "$ROOT/app/build.gradle.kts"
grep -q 'https://claude.com/cai/oauth/authorize' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
grep -q 'https://platform.claude.com/v1/oauth/token' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
grep -q 'https://api.anthropic.com/api/oauth/usage' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
grep -q 'https://api.anthropic.com/api/oauth/profile' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
# Prevent regressions in brand identity and subscription naming.
grep -q 'ClaudeAccountProfile.parse' "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageApi.java"
grep -q 'ClaudeAccountProfile.displayLabel' "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageFormat.java"
grep -q 'Color.rgb(217, 119, 87)' "$ROOT/app/src/main/java/dev/bennett/claudemeter/Ui.java"
! grep -q 'Color.rgb(3, 129, 254)' "$ROOT/app/src/main/java/dev/bennett/claudemeter/Ui.java"
! grep -q 'Color.rgb(3, 129, 254)' "$ROOT/app/src/main/java/dev/bennett/claudemeter/NowBarManager.java"
! grep -Eq '#0381fe|#5ca9ff' "$ROOT/app/src/main/java/dev/bennett/claudemeter/OAuthBrowserPage.java"
grep -q 'anthropic-beta' "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageApi.java"
grep -q 'extra_usage' "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageParser.java"
grep -q 'seven_day' "$ROOT/app/src/main/java/dev/bennett/claudemeter/UsageParser.java"
grep -q '#D97757' "$ROOT/app/src/main/res/values/colors.xml"
grep -q '#141413' "$ROOT/app/src/main/res/values/colors.xml"
grep -q '@color/claude_orange' "$ROOT/app/src/main/res/values-night/colors.xml"
grep -q 'progress_claude' "$ROOT/app/src/main/java/dev/bennett/claudemeter/WidgetRenderer.java"
grep -q 'workflow_dispatch:' "$ROOT/../.github/workflows/build-apk.yml"
! grep -Eq '^[[:space:]]+(push|pull_request):' "$ROOT/../.github/workflows/build-apk.yml"
grep -q 'claude_meter_adaptive_fg' "$ROOT/app/src/main/res/mipmap-anydpi/ic_launcher.xml"
grep -q 'lucsenl/Claude-Meter/releases?per_page=30' "$ROOT/app/build.gradle.kts"
grep -Fq 'ClaudeMeter-$VERSION_NAME.apk' "$ROOT/../.github/workflows/build-apk.yml"
test ! -d "$ROOT/../ios"
test ! -d "$ROOT/wear"
! grep -Rq 'dev\.bennett\.codexmeter' "$ROOT/app/src/main"
grep -q 'dev.bennett.claudemeter.LockWidgetConfigActivity' "$ROOT/app/src/main/res/xml/samsung_lock_square_info.xml"
test ! -d "$ROOT/app/src/main/java/dev/bennett/claudemeter/wear"
grep -q '"user:profile user:inference"' "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
! grep -q "org:create_api_key" "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"
! grep -q "user:sessions:claude_code" "$ROOT/app/src/main/java/dev/bennett/claudemeter/AppConstants.java"

echo "Claude Meter source-contract checks passed."

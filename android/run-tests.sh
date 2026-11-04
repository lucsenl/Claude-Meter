#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
JSON_JAR="${JSON_JAR:-}"

if [[ -z "$JSON_JAR" ]]; then
  CACHE="$ROOT/build/test-libs"
  mkdir -p "$CACHE"
  JSON_JAR="$CACHE/json-20250517.jar"
  if [[ ! -f "$JSON_JAR" ]]; then
    curl -fsSL "https://repo1.maven.org/maven2/org/json/json/20250517/json-20250517.jar" -o "$JSON_JAR"
  fi
fi

OUT="$ROOT/build/tests"
rm -rf "$OUT"
mkdir -p "$OUT"

javac -encoding UTF-8 -cp "$JSON_JAR" -d "$OUT" \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/UsageSnapshot.java" \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/UsageParser.java" \
  "$ROOT/tests/ParserSelfTest.java"

java -ea -cp "$OUT:$JSON_JAR" dev.lucsenl.claudemeter.ParserSelfTest

grep -q 'applicationId = "dev.lucsenl.claudemeter"' "$ROOT/app/build.gradle.kts"
grep -q 'rootProject.name = "Claude-Meter"' "$ROOT/settings.gradle.kts"
grep -q 'include(":app")' "$ROOT/settings.gradle.kts"
grep -q 'OAUTH_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'OAUTH_AUTHORIZE_URL = "https://claude.com/cai/oauth/authorize"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'OAUTH_TOKEN_URL = "https://platform.claude.com/v1/oauth/token"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'OAUTH_BETA = "oauth-2025-04-20"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'USAGE_URL = "https://api.anthropic.com/api/oauth/usage"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'PROFILE_URL = "https://api.anthropic.com/api/oauth/profile"' \
  "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"

! grep -R -E -q 'x-api-key|API key|api_key|analytics/usage|analytics/cost|workspace[_ -]?id' \
  "$ROOT/app/src/main"
! grep -R -E -q 'User-Agent.*claude-code|claude-code/[0-9]' "$ROOT/app/src/main"
! grep -R -E -q 'auth\.anthropic\.com|claude\.com/backend-api' "$ROOT/app/src/main"
! grep -q 'play-services-wearable' "$ROOT/app/build.gradle.kts"
! test -d "$ROOT/wear"
! test -d "$ROOT/shared"

echo "Claude Meter login-only source checks passed."

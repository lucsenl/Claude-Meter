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
grep -q 'versionCode = 1' "$ROOT/app/build.gradle.kts"
grep -q 'versionName = "0.1.0"' "$ROOT/app/build.gradle.kts"
grep -q 'VERSION_CODE = BuildConfig.VERSION_CODE' "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'VERSION_NAME = BuildConfig.VERSION_NAME' "$ROOT/app/src/main/java/dev/lucsenl/claudemeter/AppConstants.java"
grep -q 'rootProject.name = "Claude-Meter"' "$ROOT/settings.gradle.kts"
! grep -q 'include(":wear")' "$ROOT/settings.gradle.kts"
! grep -q 'include(":shared")' "$ROOT/settings.gradle.kts"
! grep -R -E -q 'dev\.bennett|OpenAI|ChatGPT|Codex' "$ROOT/app/src/main" "$ROOT/settings.gradle.kts" "$ROOT/app/build.gradle.kts"
! grep -R -E -q 'auth\.anthropic\.com|claude\.com/backend-api' "$ROOT/app/src/main"
! grep -R -E -q 'fiveHour|resetCredits|weekly allowance|5-hour allowance' "$ROOT/app/src/main/java" "$ROOT/app/src/main/res"
! test -d "$ROOT/shared"

echo "Claude Meter phone-only source checks passed."

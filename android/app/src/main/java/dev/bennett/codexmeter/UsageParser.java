package dev.bennett.codexmeter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class UsageParser {
    private static final long FIVE_HOURS = 18000;
    private static final long WEEK = 604800;
    private static final long MONTH = 2592000;
    // Free-tier accounts report a single ~30-day Codex window; accept 10-45 days so calendar
    // months and drifting billing periods still classify while staying clear of the weekly
    // window's 9-day ceiling.
    private static final long MONTH_MIN = 864000;
    private static final long MONTH_MAX = 3888000;

    private UsageParser() {
    }

    public static UsageSnapshot parse(String str, long j) throws JSONException {
        boolean z;
        boolean z2;
        UsageWindow usageWindow;
        JSONObject jSONObject = new JSONObject(str);
        if (isClaudeUsage(jSONObject)) {
            return parseClaude(jSONObject, j);
        }
        String strOptString = jSONObject.optString("plan_type", "");
        JSONObject jSONObjectNullableObject = nullableObject(jSONObject, "rate_limit");
        ArrayList arrayList = new ArrayList();
        ArrayList<UsageLimit> additionalLimits = new ArrayList<>();
        UsageWindow usageWindowFromJson = null;
        if (jSONObjectNullableObject == null) {
            z = true;
            z2 = false;
            usageWindow = null;
        } else {
            boolean zOptBoolean = jSONObjectNullableObject.optBoolean("allowed", true);
            boolean zOptBoolean2 = jSONObjectNullableObject.optBoolean("limit_reached", false);
            UsageWindow usageWindowFromJson2 = UsageWindow.fromJson(nullableObject(jSONObjectNullableObject, "primary_window"));
            usageWindowFromJson = UsageWindow.fromJson(nullableObject(jSONObjectNullableObject, "secondary_window"));
            if (usageWindowFromJson2 != null) {
                arrayList.add(usageWindowFromJson2);
            }
            if (usageWindowFromJson != null) {
                arrayList.add(usageWindowFromJson);
            }
            z = zOptBoolean;
            z2 = zOptBoolean2;
            usageWindow = usageWindowFromJson2;
        }
        JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("additional_rate_limits");
        if (jSONArrayOptJSONArray != null) {
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject != null) {
                    JSONObject jSONObjectNullableObject2 = nullableObject(jSONObjectOptJSONObject, "rate_limit");
                    if (jSONObjectNullableObject2 == null) {
                        jSONObjectNullableObject2 = jSONObjectOptJSONObject;
                    }
                    UsageWindow usageWindowFromJson3 = UsageWindow.fromJson(nullableObject(jSONObjectNullableObject2, "primary_window"));
                    UsageWindow usageWindowFromJson4 = UsageWindow.fromJson(nullableObject(jSONObjectNullableObject2, "secondary_window"));
                    if (usageWindowFromJson3 != null || usageWindowFromJson4 != null) {
                        String name = jSONObjectOptJSONObject.optString("limit_name", "");
                        String feature = jSONObjectOptJSONObject.optString("metered_feature", "");
                        String id = firstNonEmpty(
                                jSONObjectOptJSONObject.optString("limit_id", ""),
                                name, feature, "additional");
                        additionalLimits.add(new UsageLimit(
                                id + "-" + i,
                                name,
                                feature,
                                jSONObjectNullableObject2.optBoolean("allowed", true),
                                jSONObjectNullableObject2.optBoolean("limit_reached", false),
                                usageWindowFromJson3,
                                usageWindowFromJson4));
                    }
                }
            }
        }
        UsageWindow usageWindowNearest = nearest(arrayList, FIVE_HOURS, 10800L, 28800L);
        UsageWindow usageWindowNearestExcluding = nearestExcluding(arrayList, WEEK, 432000L, 777600L, usageWindowNearest);
        UsageWindow usageWindowMonthly = nearestExcluding(arrayList, MONTH, MONTH_MIN, MONTH_MAX,
                usageWindowNearest, usageWindowNearestExcluding);
        JSONObject jSONObjectNullableObject3 = nullableObject(jSONObject, "rate_limit_reset_credits");
        UsageCredits usageCredits = UsageCredits.fromJson(nullableObject(jSONObject, "credits"));
        return new UsageSnapshot(
                strOptString,
                z,
                z2,
                usageWindowNearest,
                usageWindowNearestExcluding,
                usageWindowMonthly,
                additionalLimits,
                usageCredits,
                jSONObjectNullableObject3 == null
                        ? -1 : jSONObjectNullableObject3.optInt("available_count", -1),
                j);
    }

    private static boolean isClaudeUsage(JSONObject root) {
        return root.has("five_hour") || root.has("seven_day") || root.has("limits");
    }

    private static UsageSnapshot parseClaude(JSONObject root, long fetchedAtMillis) {
        UsageWindow fiveHour = claudeWindow(nullableObject(root, "five_hour"), FIVE_HOURS,
                fetchedAtMillis);
        UsageWindow weekly = claudeWindow(nullableObject(root, "seven_day"), WEEK,
                fetchedAtMillis);
        ArrayList<UsageLimit> additionalLimits = new ArrayList<>();

        // Older and newer Claude usage schemas can coexist in one response. Keep the
        // named top-level model windows, then merge the newer scoped limits below.
        addLegacyClaudeLimit(additionalLimits, root, "seven_day_sonnet",
                "Sonnet weekly", "sonnet", fetchedAtMillis);
        addLegacyClaudeLimit(additionalLimits, root, "seven_day_opus",
                "Opus weekly", "opus", fetchedAtMillis);
        addLegacyClaudeLimit(additionalLimits, root, "seven_day_oauth_apps",
                "OAuth apps weekly", "oauth_apps", fetchedAtMillis);
        addLegacyClaudeLimit(additionalLimits, root, "seven_day_cowork",
                "Cowork weekly", "cowork", fetchedAtMillis);
        addLegacyClaudeLimit(additionalLimits, root, "seven_day_overage_included",
                "Overage weekly", "overage", fetchedAtMillis);

        JSONArray scopedLimits = root.optJSONArray("limits");
        if (scopedLimits != null && scopedLimits.length() > 0) {
            for (int i = 0; i < scopedLimits.length(); i++) {
                JSONObject item = scopedLimits.optJSONObject(i);
                if (item == null) continue;
                double percent = item.optDouble("percent", Double.NaN);
                if (Double.isNaN(percent) || Double.isInfinite(percent)) continue;

                String group = item.optString("group", "");
                String kind = item.optString("kind", "");
                String normalized = (group + " " + kind).toLowerCase(java.util.Locale.ROOT);
                long windowSeconds = normalized.contains("5h")
                        || normalized.contains("five_hour") || normalized.contains("5-hour")
                        ? FIVE_HOURS : WEEK;
                UsageWindow window = claudeWindowFromPercent(percent,
                        item.optString("resets_at", ""), windowSeconds, fetchedAtMillis);
                if (window == null) continue;

                JSONObject scope = item.optJSONObject("scope");
                JSONObject model = scope == null ? null : scope.optJSONObject("model");
                String modelName = model == null ? "" : model.optString("display_name", "");
                String cadence = windowSeconds == FIVE_HOURS ? "5-hour" : "weekly";
                String name = modelName.isEmpty()
                        ? firstNonEmpty(group, kind, "Additional Claude limit")
                        : modelName + " " + cadence;
                if (!containsClaudeLimit(additionalLimits, modelName, name)) {
                    additionalLimits.add(new UsageLimit(
                            firstNonEmpty(item.optString("id", ""), modelName, group, kind,
                                    "claude-limit") + "-" + i,
                            name,
                            firstNonEmpty(group, kind),
                            window.usedPercent < 100,
                            window.usedPercent >= 100,
                            window,
                            null));
                }
            }
        }

        boolean limitReached = (fiveHour != null && fiveHour.usedPercent >= 100)
                || (weekly != null && weekly.usedPercent >= 100);
        for (UsageLimit limit : additionalLimits) {
            limitReached = limitReached || limit.limitReached;
        }
        return new UsageSnapshot(
                "claude",
                !limitReached,
                limitReached,
                fiveHour,
                weekly,
                null,
                additionalLimits,
                null,
                -1,
                fetchedAtMillis);
    }

    private static boolean containsClaudeLimit(List<UsageLimit> limits, String modelName,
            String displayName) {
        String normalizedModel = modelName == null ? "" : modelName.trim();
        String normalizedDisplay = displayName == null ? "" : displayName.trim();
        for (UsageLimit existing : limits) {
            if (!normalizedDisplay.isEmpty()
                    && existing.displayName().equalsIgnoreCase(normalizedDisplay)) {
                return true;
            }
            if (!normalizedModel.isEmpty()
                    && existing.displayName().toLowerCase(java.util.Locale.ROOT)
                            .startsWith(normalizedModel.toLowerCase(java.util.Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static void addLegacyClaudeLimit(List<UsageLimit> target, JSONObject root,
            String key, String name, String feature, long fetchedAtMillis) {
        UsageWindow window = claudeWindow(nullableObject(root, key), WEEK, fetchedAtMillis);
        if (window == null) return;
        target.add(new UsageLimit(
                "claude-" + feature,
                name,
                feature,
                window.usedPercent < 100,
                window.usedPercent >= 100,
                window,
                null));
    }

    private static UsageWindow claudeWindow(JSONObject object, long windowSeconds,
            long fetchedAtMillis) {
        if (object == null || object.isNull("utilization")) return null;
        double utilization = object.optDouble("utilization", Double.NaN);
        if (Double.isNaN(utilization) || Double.isInfinite(utilization)) return null;
        return claudeWindowFromPercent(utilization, object.optString("resets_at", ""),
                windowSeconds, fetchedAtMillis);
    }

    private static UsageWindow claudeWindowFromPercent(double utilization, String resetsAt,
            long windowSeconds, long fetchedAtMillis) {
        long resetAt = parseResetEpochSeconds(resetsAt);
        long fetchedAtSeconds = Math.max(0L, fetchedAtMillis / 1000L);
        long resetAfter = resetAt > fetchedAtSeconds ? resetAt - fetchedAtSeconds : 0L;
        return new UsageWindow((int) Math.round(utilization), windowSeconds, resetAfter, resetAt);
    }

    private static long parseResetEpochSeconds(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Instant.parse(value.trim()).getEpochSecond();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static JSONObject nullableObject(JSONObject jSONObject, String str) {
        if (jSONObject == null || jSONObject.isNull(str)) {
            return null;
        }
        return jSONObject.optJSONObject(str);
    }

    private static UsageWindow nearest(List<UsageWindow> list, long j, long j2, long j3) {
        UsageWindow usageWindow;
        long j4;
        UsageWindow usageWindow2 = null;
        long j5 = Long.MAX_VALUE;
        for (UsageWindow usageWindow3 : list) {
            if (usageWindow3.windowSeconds < j2 || usageWindow3.windowSeconds > j3) {
                long j6 = j5;
                usageWindow = usageWindow2;
                j4 = j6;
            } else {
                long jAbs = Math.abs(usageWindow3.windowSeconds - j);
                if (jAbs < j5) {
                    usageWindow = usageWindow3;
                    j4 = jAbs;
                } else {
                    long j7 = j5;
                    usageWindow = usageWindow2;
                    j4 = j7;
                }
            }
            usageWindow2 = usageWindow;
            j5 = j4;
        }
        return usageWindow2;
    }

    private static UsageWindow nearestExcluding(List<UsageWindow> list, long target,
            long minimumSeconds, long maximumSeconds, UsageWindow... excluded) {
        UsageWindow best = null;
        long bestDistance = Long.MAX_VALUE;
        for (UsageWindow candidate : list) {
            if (isExcluded(candidate, excluded)
                    || candidate.windowSeconds < minimumSeconds
                    || candidate.windowSeconds > maximumSeconds) {
                continue;
            }
            long distance = Math.abs(candidate.windowSeconds - target);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean isExcluded(UsageWindow candidate, UsageWindow[] excluded) {
        for (UsageWindow window : excluded) {
            if (candidate == window) {
                return true;
            }
        }
        return false;
    }

}

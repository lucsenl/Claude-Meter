package dev.bennett.claudemeter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Parses the Claude OAuth usage response into the app's provider-neutral snapshot. */
public final class UsageParser {
    private static final long FIVE_HOURS = 5L * 60L * 60L;
    private static final long WEEK = 7L * 24L * 60L * 60L;

    private UsageParser() {}

    public static UsageSnapshot parse(String json, long fetchedAtMillis) throws JSONException {
        return parse(json, fetchedAtMillis, "");
    }

    public static UsageSnapshot parse(String json, long fetchedAtMillis, String planType)
            throws JSONException {
        JSONObject root = new JSONObject(json);
        UsageWindow fiveHour = parseWindow(root.optJSONObject("five_hour"), FIVE_HOURS);
        UsageWindow weekly = parseWindow(root.optJSONObject("seven_day"), WEEK);

        List<UsageLimit> additional = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        parseScopedLimits(root.optJSONArray("limits"), additional, ids);
        parseFlatWindows(root, additional, ids);

        UsageCredits extraUsage = UsageCredits.fromExtraUsage(root.optJSONObject("extra_usage"));
        return new UsageSnapshot(
                planType,
                true,
                false,
                fiveHour,
                weekly,
                null,
                additional,
                extraUsage,
                -1,
                fetchedAtMillis);
    }

    private static void parseScopedLimits(JSONArray limits, List<UsageLimit> output,
            Set<String> ids) {
        if (limits == null) return;
        for (int i = 0; i < limits.length(); i++) {
            JSONObject limit = limits.optJSONObject(i);
            if (limit == null) continue;
            double percent = finiteNumber(limit.opt("percent"));
            if (Double.isNaN(percent) || percent < 0.0d) continue;

            JSONObject scope = limit.optJSONObject("scope");
            JSONObject model = scope == null ? null : scope.optJSONObject("model");
            String displayName = model == null ? "" : model.optString("display_name", "").trim();
            if (displayName.isEmpty()) {
                displayName = limit.optString("name", "").trim();
            }
            if (displayName.isEmpty()) continue;

            String id = "weekly_" + slug(displayName);
            if (!ids.add(id)) continue;
            long duration = "weekly".equalsIgnoreCase(limit.optString("group", "")) ? WEEK : 0L;
            UsageWindow window = new UsageWindow(
                    (int) Math.round(percent),
                    duration,
                    0L,
                    parseResetSeconds(limit.optString("resets_at", "")));
            output.add(new UsageLimit(id, displayName, "model", true,
                    window.usedPercent >= 100, window, null));
        }
    }

    private static void parseFlatWindows(JSONObject root, List<UsageLimit> output, Set<String> ids) {
        Iterator<String> keys = root.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("five_hour".equals(key) || "seven_day".equals(key)
                    || "extra_usage".equals(key) || "limits".equals(key)) {
                continue;
            }
            JSONObject object = root.optJSONObject(key);
            if (object == null || Double.isNaN(finiteNumber(object.opt("utilization")))) continue;

            String id = normalizeFlatId(key);
            if (!ids.add(id)) continue;
            UsageWindow window = parseWindow(object, durationForKey(key));
            if (window == null) continue;
            output.add(new UsageLimit(id, labelForKey(key), "model", true,
                    window.usedPercent >= 100, window, null));
        }
    }

    private static UsageWindow parseWindow(JSONObject object, long durationSeconds) {
        if (object == null) return null;
        double utilization = finiteNumber(object.opt("utilization"));
        if (Double.isNaN(utilization) || utilization < 0.0d) return null;
        return new UsageWindow(
                (int) Math.round(utilization),
                durationSeconds,
                0L,
                parseResetSeconds(object.optString("resets_at", "")));
    }

    private static double finiteNumber(Object raw) {
        if (raw instanceof Number) {
            double value = ((Number) raw).doubleValue();
            return Double.isFinite(value) ? value : Double.NaN;
        }
        if (raw instanceof String) {
            try {
                double value = Double.parseDouble((String) raw);
                return Double.isFinite(value) ? value : Double.NaN;
            } catch (NumberFormatException ignored) {}
        }
        return Double.NaN;
    }

    private static long parseResetSeconds(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            return Instant.parse(value.trim()).getEpochSecond();
        } catch (Exception ignored) {
            try {
                return OffsetDateTime.parse(value.trim()).toInstant().getEpochSecond();
            } catch (Exception ignoredAgain) {
                return 0L;
            }
        }
    }

    private static long durationForKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("seven_day") || "iguana_necktie".equals(normalized)) {
            return WEEK;
        }
        if (normalized.startsWith("five_hour")) return FIVE_HOURS;
        return 0L;
    }

    private static String normalizeFlatId(String key) {
        if ("iguana_necktie".equalsIgnoreCase(key)) return "weekly_fable";
        return slug(key);
    }

    private static String labelForKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if ("iguana_necktie".equals(normalized) || normalized.contains("fable")) return "Fable";
        if (normalized.contains("opus")) return "Opus";
        if (normalized.contains("sonnet")) return "Sonnet";
        if (normalized.contains("cowork")) return "Cowork";
        return titleCase(key.replace("seven_day_", "").replace("five_hour_", ""));
    }

    private static String slug(String value) {
        String result = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return result.isEmpty() ? "usage" : result;
    }

    private static String titleCase(String value) {
        String normalized = value.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isEmpty()) return "Additional usage";
        StringBuilder out = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(c);
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }
}

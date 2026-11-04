package dev.lucsenl.claudemeter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Defensive parser for Claude OAuth profile and subscription usage responses. */
public final class UsageParser {
    private UsageParser() {}

    public static UsageSnapshot parse(String usageJson, String profileJson,
            long fetchedAtMillis) throws JSONException {
        JSONObject usage = new JSONObject(usageJson == null ? "{}" : usageJson);
        JSONObject profile = new JSONObject(profileJson == null ? "{}" : profileJson);

        JSONObject account = profile.optJSONObject("account");
        JSONObject organization = profile.optJSONObject("organization");
        if (account == null) account = new JSONObject();
        if (organization == null) organization = new JSONObject();

        String email = firstNonEmpty(
                account.optString("email", ""),
                account.optString("email_address", ""));
        String displayName = firstNonEmpty(
                account.optString("display_name", ""),
                account.optString("full_name", ""));
        String organizationType = organization.optString("organization_type", "");
        String rateLimitTier = organization.optString("rate_limit_tier", "");
        String subscriptionStatus = organization.optString("subscription_status", "");
        String plan = planLabel(account, organizationType, rateLimitTier);

        Window five = window(usage.optJSONObject("five_hour"));
        Window seven = window(usage.optJSONObject("seven_day"));

        ArrayList<UsageSnapshot.ScopedLimit> scoped = parseScopedLimits(usage);
        addLegacyScoped(scoped, usage, "seven_day_opus", "Opus · Weekly");
        addLegacyScoped(scoped, usage, "seven_day_sonnet", "Sonnet · Weekly");
        addLegacyScoped(scoped, usage, "seven_day_cowork", "Cowork · Weekly");
        addLegacyScoped(scoped, usage, "seven_day_oauth_apps", "OAuth apps · Weekly");

        JSONObject extra = usage.optJSONObject("extra_usage");
        boolean extraEnabled = extra != null && extra.optBoolean("is_enabled", false);

        return new UsageSnapshot(
                email, displayName, plan, organizationType, rateLimitTier, subscriptionStatus,
                five.utilization, five.resetsAt,
                seven.utilization, seven.resetsAt,
                scoped, extraEnabled,
                usage.optBoolean("member_dashboard_available", false),
                fetchedAtMillis);
    }

    static Window window(JSONObject json) {
        if (json == null) return new Window(-1d, "");
        double utilization = json.has("utilization")
                && !json.isNull("utilization")
                ? json.optDouble("utilization", -1d) : -1d;
        return new Window(utilization, json.optString("resets_at", ""));
    }

    static ArrayList<UsageSnapshot.ScopedLimit> parseScopedLimits(JSONObject usage) {
        ArrayList<UsageSnapshot.ScopedLimit> values = new ArrayList<>();
        JSONArray limits = usage.optJSONArray("limits");
        if (limits == null) return values;

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < limits.length(); i++) {
            JSONObject item = limits.optJSONObject(i);
            if (item == null) continue;
            JSONObject scope = item.optJSONObject("scope");
            JSONObject model = scope == null ? null : scope.optJSONObject("model");
            String modelName = model == null ? "" : model.optString("display_name", "").trim();
            if (modelName.isEmpty() || !item.has("percent") || item.isNull("percent")) continue;

            double percent = item.optDouble("percent", -1d);
            if (percent < 0d) continue;
            String label = modelName + " · Weekly";
            if (!seen.add(label.toLowerCase(Locale.US))) continue;
            values.add(new UsageSnapshot.ScopedLimit(
                    label, percent, item.optString("resets_at", ""),
                    item.optBoolean("is_active", false)));
        }
        return values;
    }

    private static void addLegacyScoped(List<UsageSnapshot.ScopedLimit> values,
            JSONObject usage, String key, String label) {
        JSONObject object = usage.optJSONObject(key);
        if (object == null) return;
        Window value = window(object);
        if (value.utilization < 0d) return;
        for (UsageSnapshot.ScopedLimit existing : values) {
            if (existing.label.equalsIgnoreCase(label)) return;
        }
        values.add(new UsageSnapshot.ScopedLimit(label, value.utilization, value.resetsAt, false));
    }

    static String planLabel(JSONObject account, String organizationType, String rateLimitTier) {
        String type = organizationType == null ? "" : organizationType.toLowerCase(Locale.US);
        String tier = rateLimitTier == null ? "" : rateLimitTier.toLowerCase(Locale.US);

        if (type.contains("max") || account.optBoolean("has_claude_max", false)) {
            if (tier.contains("20x")) return "Max 20×";
            if (tier.contains("5x")) return "Max 5×";
            return "Max";
        }
        if (type.contains("pro") || account.optBoolean("has_claude_pro", false)) return "Pro";
        if (type.contains("team")) return "Team";
        if (type.contains("enterprise")) return "Enterprise";
        if (!organizationType.isEmpty()) return organizationType;
        return "Claude";
    }

    static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    static final class Window {
        final double utilization;
        final String resetsAt;

        Window(double utilization, String resetsAt) {
            this.utilization = utilization;
            this.resetsAt = resetsAt == null ? "" : resetsAt.trim();
        }
    }
}

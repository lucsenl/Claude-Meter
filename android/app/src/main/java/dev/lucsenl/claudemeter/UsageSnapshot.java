package dev.lucsenl.claudemeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Snapshot of Claude subscription profile and usage windows. */
public final class UsageSnapshot {
    public final String email;
    public final String displayName;
    public final String plan;
    public final String organizationType;
    public final String rateLimitTier;
    public final String subscriptionStatus;
    public final double fiveHourUtilization;
    public final String fiveHourResetsAt;
    public final double sevenDayUtilization;
    public final String sevenDayResetsAt;
    public final List<ScopedLimit> scopedLimits;
    public final boolean extraUsageEnabled;
    public final boolean memberDashboardAvailable;
    public final long fetchedAtMillis;

    public UsageSnapshot(String email, String displayName, String plan,
            String organizationType, String rateLimitTier, String subscriptionStatus,
            double fiveHourUtilization, String fiveHourResetsAt,
            double sevenDayUtilization, String sevenDayResetsAt,
            List<ScopedLimit> scopedLimits, boolean extraUsageEnabled,
            boolean memberDashboardAvailable, long fetchedAtMillis) {
        this.email = safe(email);
        this.displayName = safe(displayName);
        this.plan = safe(plan);
        this.organizationType = safe(organizationType);
        this.rateLimitTier = safe(rateLimitTier);
        this.subscriptionStatus = safe(subscriptionStatus);
        this.fiveHourUtilization = normalizePercent(fiveHourUtilization);
        this.fiveHourResetsAt = safe(fiveHourResetsAt);
        this.sevenDayUtilization = normalizePercent(sevenDayUtilization);
        this.sevenDayResetsAt = safe(sevenDayResetsAt);
        this.scopedLimits = scopedLimits == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(scopedLimits));
        this.extraUsageEnabled = extraUsageEnabled;
        this.memberDashboardAvailable = memberDashboardAvailable;
        this.fetchedAtMillis = Math.max(0L, fetchedAtMillis);
    }

    public boolean hasFiveHour() {
        return fiveHourUtilization >= 0d;
    }

    public boolean hasSevenDay() {
        return sevenDayUtilization >= 0d;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject()
                .put("email", email)
                .put("display_name", displayName)
                .put("plan", plan)
                .put("organization_type", organizationType)
                .put("rate_limit_tier", rateLimitTier)
                .put("subscription_status", subscriptionStatus)
                .put("five_hour_utilization", fiveHourUtilization)
                .put("five_hour_resets_at", fiveHourResetsAt)
                .put("seven_day_utilization", sevenDayUtilization)
                .put("seven_day_resets_at", sevenDayResetsAt)
                .put("extra_usage_enabled", extraUsageEnabled)
                .put("member_dashboard_available", memberDashboardAvailable)
                .put("fetched_at", fetchedAtMillis);
        JSONArray limits = new JSONArray();
        for (ScopedLimit limit : scopedLimits) limits.put(limit.toJson());
        json.put("scoped_limits", limits);
        return json;
    }

    public static UsageSnapshot fromJson(JSONObject json) {
        if (json == null || !json.has("fetched_at")) return null;
        ArrayList<ScopedLimit> limits = new ArrayList<>();
        JSONArray array = json.optJSONArray("scoped_limits");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                ScopedLimit limit = ScopedLimit.fromJson(array.optJSONObject(i));
                if (limit != null) limits.add(limit);
            }
        }
        return new UsageSnapshot(
                json.optString("email", ""),
                json.optString("display_name", ""),
                json.optString("plan", ""),
                json.optString("organization_type", ""),
                json.optString("rate_limit_tier", ""),
                json.optString("subscription_status", ""),
                json.has("five_hour_utilization")
                        ? json.optDouble("five_hour_utilization", -1d) : -1d,
                json.optString("five_hour_resets_at", ""),
                json.has("seven_day_utilization")
                        ? json.optDouble("seven_day_utilization", -1d) : -1d,
                json.optString("seven_day_resets_at", ""),
                limits,
                json.optBoolean("extra_usage_enabled", false),
                json.optBoolean("member_dashboard_available", false),
                json.optLong("fetched_at", 0L));
    }

    private static double normalizePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0d) return -1d;
        return Math.max(0d, value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ScopedLimit {
        public final String label;
        public final double utilization;
        public final String resetsAt;
        public final boolean active;

        public ScopedLimit(String label, double utilization, String resetsAt, boolean active) {
            this.label = safe(label);
            this.utilization = normalizePercent(utilization);
            this.resetsAt = safe(resetsAt);
            this.active = active;
        }

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("label", label)
                    .put("utilization", utilization)
                    .put("resets_at", resetsAt)
                    .put("active", active);
        }

        static ScopedLimit fromJson(JSONObject json) {
            if (json == null) return null;
            String label = json.optString("label", "").trim();
            double utilization = json.optDouble("utilization", -1d);
            if (label.isEmpty() || utilization < 0d) return null;
            return new ScopedLimit(label, utilization,
                    json.optString("resets_at", ""),
                    json.optBoolean("active", false));
        }
    }
}

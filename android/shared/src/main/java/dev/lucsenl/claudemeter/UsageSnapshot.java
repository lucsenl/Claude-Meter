package dev.lucsenl.claudemeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Immutable snapshot of documented Anthropic API analytics. */
public final class UsageSnapshot {
    public final boolean analyticsAvailable;
    public final String analyticsMessage;
    public final String organizationId;
    public final String organizationName;
    public final long todayTokens;
    public final long todayRequests;
    public final long weekTokens;
    public final long weekRequests;
    public final String monthCostUsd;
    public final List<String> activeModels;
    public final long fetchedAtMillis;

    public UsageSnapshot(boolean analyticsAvailable, String analyticsMessage,
            String organizationId, String organizationName,
            long todayTokens, long todayRequests, long weekTokens, long weekRequests,
            String monthCostUsd, List<String> activeModels, long fetchedAtMillis) {
        this.analyticsAvailable = analyticsAvailable;
        this.analyticsMessage = safe(analyticsMessage);
        this.organizationId = safe(organizationId);
        this.organizationName = safe(organizationName);
        this.todayTokens = Math.max(0L, todayTokens);
        this.todayRequests = Math.max(0L, todayRequests);
        this.weekTokens = Math.max(0L, weekTokens);
        this.weekRequests = Math.max(0L, weekRequests);
        this.monthCostUsd = safe(monthCostUsd);
        this.activeModels = activeModels == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(activeModels));
        this.fetchedAtMillis = Math.max(0L, fetchedAtMillis);
    }

    public boolean hasDisplayableData() {
        return analyticsAvailable || todayTokens > 0L || weekTokens > 0L
                || !monthCostUsd.isEmpty() || !activeModels.isEmpty()
                || !analyticsMessage.isEmpty();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("analytics_available", analyticsAvailable);
        json.put("analytics_message", analyticsMessage);
        json.put("organization_id", organizationId);
        json.put("organization_name", organizationName);
        json.put("today_tokens", todayTokens);
        json.put("today_requests", todayRequests);
        json.put("week_tokens", weekTokens);
        json.put("week_requests", weekRequests);
        json.put("month_cost_usd", monthCostUsd);
        JSONArray models = new JSONArray();
        for (String model : activeModels) models.put(model);
        json.put("active_models", models);
        json.put("fetched_at", fetchedAtMillis);
        return json;
    }

    public static UsageSnapshot fromJson(JSONObject json) {
        if (json == null || !json.has("analytics_available")) return null;
        ArrayList<String> models = new ArrayList<>();
        JSONArray values = json.optJSONArray("active_models");
        if (values != null) {
            for (int i = 0; i < values.length(); i++) {
                String value = values.optString(i, "").trim();
                if (!value.isEmpty()) models.add(value);
            }
        }
        return new UsageSnapshot(
                json.optBoolean("analytics_available", false),
                json.optString("analytics_message", ""),
                json.optString("organization_id", ""),
                json.optString("organization_name", ""),
                json.optLong("today_tokens", 0L),
                json.optLong("today_requests", 0L),
                json.optLong("week_tokens", 0L),
                json.optLong("week_requests", 0L),
                json.optString("month_cost_usd", ""),
                models,
                json.optLong("fetched_at", 0L));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

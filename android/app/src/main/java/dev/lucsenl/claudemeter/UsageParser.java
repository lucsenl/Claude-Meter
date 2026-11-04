package dev.lucsenl.claudemeter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Parsers for Anthropic public Analytics, Cost and Models API responses. */
public final class UsageParser {
    private UsageParser() {}

    public static UsageSnapshot parseAnalytics(String todayJson, String weekJson,
            String costJson, String modelsJson, String organizationJson,
            long fetchedAtMillis) throws JSONException {
        Totals today = usageTotals(todayJson);
        Totals week = usageTotals(weekJson);
        String cost = costUsd(costJson);
        List<String> models = models(modelsJson);
        String orgId = "";
        String orgName = "";
        if (organizationJson != null && !organizationJson.trim().isEmpty()) {
            JSONObject org = new JSONObject(organizationJson);
            JSONObject data = org.optJSONObject("data");
            JSONObject source = data == null ? org : data;
            orgId = source.optString("id", "");
            orgName = firstNonEmpty(source.optString("name", ""),
                    source.optString("display_name", ""));
        }
        return new UsageSnapshot(true, "", orgId, orgName,
                today.tokens, today.requests, week.tokens, week.requests,
                cost, models, fetchedAtMillis);
    }

    public static UsageSnapshot analyticsUnavailable(String message, String modelsJson,
            String organizationJson, long fetchedAtMillis) throws JSONException {
        List<String> modelList = models(modelsJson);
        String orgId = "";
        String orgName = "";
        if (organizationJson != null && !organizationJson.trim().isEmpty()) {
            JSONObject org = new JSONObject(organizationJson);
            JSONObject data = org.optJSONObject("data");
            JSONObject source = data == null ? org : data;
            orgId = source.optString("id", "");
            orgName = firstNonEmpty(source.optString("name", ""),
                    source.optString("display_name", ""));
        }
        return new UsageSnapshot(false, message, orgId, orgName,
                0, 0, 0, 0, "", modelList, fetchedAtMillis);
    }

    static Totals usageTotals(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw == null ? "{}" : raw);
        JSONArray data = root.optJSONArray("data");
        long tokens = 0L;
        long requests = 0L;
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.optJSONObject(i);
                JSONObject usage = row == null ? null : row.optJSONObject("usage");
                if (usage == null) continue;
                tokens += Math.max(0L, usage.optLong("total_tokens", 0L));
                requests += Math.max(0L, usage.optLong("requests", 0L));
            }
        }
        return new Totals(tokens, requests);
    }

    static String costUsd(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw == null ? "{}" : raw);
        JSONArray data = root.optJSONArray("data");
        BigDecimal cents = BigDecimal.ZERO;
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.optJSONObject(i);
                JSONObject cost = row == null ? null : row.optJSONObject("cost");
                if (cost == null) continue;
                try {
                    cents = cents.add(new BigDecimal(cost.optString("amount", "0")));
                } catch (NumberFormatException ignored) {}
            }
        }
        return cents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
    }

    static List<String> models(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw == null ? "{}" : raw);
        JSONArray data = root.optJSONArray("data");
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                JSONObject model = data.optJSONObject(i);
                if (model == null) continue;
                String value = firstNonEmpty(model.optString("display_name", ""),
                        model.optString("id", ""));
                if (!value.isEmpty()) values.add(value);
            }
        }
        return new ArrayList<>(values);
    }

    static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    static final class Totals {
        final long tokens;
        final long requests;
        Totals(long tokens, long requests) {
            this.tokens = tokens;
            this.requests = requests;
        }
    }
}

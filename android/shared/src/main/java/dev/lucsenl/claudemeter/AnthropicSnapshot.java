package dev.lucsenl.claudemeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AnthropicSnapshot {
    public final long fetchedAtMillis;
    public final List<AnthropicModel> models;
    public final boolean analyticsAvailable;
    public final long inputTokens;
    public final long outputTokens;
    public final long totalTokens;
    public final long requests;
    public final String costUsd;
    public final String analyticsMessage;

    public AnthropicSnapshot(long fetchedAtMillis, List<AnthropicModel> models,
            boolean analyticsAvailable, long inputTokens, long outputTokens,
            long totalTokens, long requests, String costUsd, String analyticsMessage) {
        this.fetchedAtMillis = fetchedAtMillis;
        this.models = Collections.unmodifiableList(new ArrayList<>(
                models == null ? Collections.emptyList() : models));
        this.analyticsAvailable = analyticsAvailable;
        this.inputTokens = Math.max(0L, inputTokens);
        this.outputTokens = Math.max(0L, outputTokens);
        this.totalTokens = Math.max(0L, totalTokens);
        this.requests = Math.max(0L, requests);
        this.costUsd = costUsd == null ? "" : costUsd;
        this.analyticsMessage = analyticsMessage == null ? "" : analyticsMessage;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("fetched_at", fetchedAtMillis);
        JSONArray array = new JSONArray();
        for (AnthropicModel model : models) array.put(model.toJson());
        object.put("models", array);
        object.put("analytics_available", analyticsAvailable);
        object.put("input_tokens", inputTokens);
        object.put("output_tokens", outputTokens);
        object.put("total_tokens", totalTokens);
        object.put("requests", requests);
        object.put("cost_usd", costUsd);
        object.put("analytics_message", analyticsMessage);
        return object;
    }

    public static AnthropicSnapshot fromJson(JSONObject object) {
        if (object == null) return null;
        List<AnthropicModel> models = new ArrayList<>();
        JSONArray array = object.optJSONArray("models");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                AnthropicModel model = AnthropicModel.fromJson(array.optJSONObject(i));
                if (model != null) models.add(model);
            }
        }
        return new AnthropicSnapshot(
                object.optLong("fetched_at", 0L),
                models,
                object.optBoolean("analytics_available", false),
                object.optLong("input_tokens", 0L),
                object.optLong("output_tokens", 0L),
                object.optLong("total_tokens", 0L),
                object.optLong("requests", 0L),
                object.optString("cost_usd", ""),
                object.optString("analytics_message", ""));
    }
}

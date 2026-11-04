package dev.lucsenl.claudemeter;

import org.json.JSONException;
import org.json.JSONObject;

public final class AnthropicModel {
    public final String id;
    public final String displayName;
    public final String lifecycle;
    public final String createdAt;

    public AnthropicModel(String id, String displayName, String lifecycle, String createdAt) {
        this.id = safe(id);
        this.displayName = safe(displayName);
        this.lifecycle = safe(lifecycle);
        this.createdAt = safe(createdAt);
    }

    public String label() {
        return displayName.isEmpty() ? id : displayName;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("display_name", displayName);
        object.put("lifecycle", lifecycle);
        object.put("created_at", createdAt);
        return object;
    }

    public static AnthropicModel fromJson(JSONObject object) {
        if (object == null) return null;
        String id = object.optString("id", "");
        if (id.isEmpty()) return null;
        return new AnthropicModel(
                id,
                object.optString("display_name", ""),
                object.optString("lifecycle", ""),
                object.optString("created_at", ""));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

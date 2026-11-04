package dev.lucsenl.claudemeter;

import org.json.JSONException;
import org.json.JSONObject;

/** User-supplied Anthropic API credential stored only on-device. */
public final class AnthropicCredential {
    public final String apiKey;
    public final String workspaceId;

    public AnthropicCredential(String apiKey, String workspaceId) {
        this.apiKey = safe(apiKey);
        this.workspaceId = safe(workspaceId);
    }

    public boolean isUsable() {
        return !apiKey.isEmpty();
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("api_key", apiKey)
                .put("workspace_id", workspaceId);
    }

    public static AnthropicCredential fromJson(JSONObject json) {
        if (json == null || !json.has("api_key")) return null;
        AnthropicCredential value = new AnthropicCredential(
                json.optString("api_key", ""),
                json.optString("workspace_id", ""));
        return value.isUsable() ? value : null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

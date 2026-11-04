package dev.lucsenl.claudemeter;

import org.json.JSONException;
import org.json.JSONObject;

/** A user-supplied Anthropic credential. Secrets never leave the device except to Anthropic. */
public final class AnthropicCredential {
    public enum Kind {
        API_KEY("api_key"),
        ORGANIZATION_KEY("organization_key");

        public final String wireName;

        Kind(String wireName) {
            this.wireName = wireName;
        }

        static Kind fromWireName(String value) {
            return "organization_key".equals(value) ? ORGANIZATION_KEY : API_KEY;
        }
    }

    public final String secret;
    public final String workspaceId;
    public final Kind kind;

    public AnthropicCredential(String secret, String workspaceId, Kind kind) {
        this.secret = safe(secret).trim();
        this.workspaceId = safe(workspaceId).trim();
        this.kind = kind == null ? Kind.API_KEY : kind;
    }

    public boolean isUsable() {
        return !secret.isEmpty();
    }

    public boolean supportsOrganizationAnalytics() {
        return kind == Kind.ORGANIZATION_KEY;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("secret", secret);
        object.put("workspace_id", workspaceId);
        object.put("kind", kind.wireName);
        return object;
    }

    public static AnthropicCredential fromJson(JSONObject object) {
        if (object == null) return null;
        AnthropicCredential credential = new AnthropicCredential(
                object.optString("secret", ""),
                object.optString("workspace_id", ""),
                Kind.fromWireName(object.optString("kind", "")));
        return credential.isUsable() ? credential : null;
    }

    public String displayType() {
        return kind == Kind.ORGANIZATION_KEY ? "Organization analytics" : "Claude API";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

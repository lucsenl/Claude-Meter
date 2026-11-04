package dev.lucsenl.claudemeter;

import org.json.JSONException;
import org.json.JSONObject;

/** OAuth credentials obtained after the user signs in to Claude. */
public final class ClaudeOAuthCredential {
    public final String accessToken;
    public final String refreshToken;
    public final long expiresAtMillis;
    public final String scope;

    public ClaudeOAuthCredential(String accessToken, String refreshToken,
            long expiresAtMillis, String scope) {
        this.accessToken = safe(accessToken);
        this.refreshToken = safe(refreshToken);
        this.expiresAtMillis = Math.max(0L, expiresAtMillis);
        this.scope = safe(scope);
    }

    public boolean isUsable() {
        return !accessToken.isEmpty() && !refreshToken.isEmpty();
    }

    public boolean shouldRefresh(long nowMillis) {
        return expiresAtMillis <= 0L || nowMillis + 5L * 60L * 1000L >= expiresAtMillis;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("access_token", accessToken)
                .put("refresh_token", refreshToken)
                .put("expires_at", expiresAtMillis)
                .put("scope", scope);
    }

    public static ClaudeOAuthCredential fromJson(JSONObject json) {
        if (json == null) return null;
        ClaudeOAuthCredential value = new ClaudeOAuthCredential(
                json.optString("access_token", ""),
                json.optString("refresh_token", ""),
                json.optLong("expires_at", 0L),
                json.optString("scope", ""));
        return value.isUsable() ? value : null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

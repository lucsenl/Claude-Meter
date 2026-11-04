package dev.lucsenl.claudemeter;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Local Anthropic credential container.
 *
 * The historical class name is kept temporarily to avoid a broad binary/source churn, but the
 * payload contains only a user-supplied Anthropic API key. There is no OAuth access/refresh token.
 */
public final class AuthTokens {
    public final String accessToken;
    public final String refreshToken = "";
    public final String idToken = "";
    public final long expiresAtMillis = Long.MAX_VALUE;
    public final String accountId = "";
    public final String email = "";

    public AuthTokens(String apiKey) {
        this.accessToken = safe(apiKey);
    }

    /** Compatibility constructor for old call sites; only the first value is retained. */
    public AuthTokens(String apiKey, String ignoredRefresh, String ignoredId, long ignoredExpiry,
            String ignoredAccount, String ignoredEmail) {
        this(apiKey);
    }

    public boolean isUsable() {
        return !accessToken.isEmpty();
    }

    public boolean shouldRefresh(long nowMillis) {
        return false;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("api_key", accessToken);
    }

    public static AuthTokens fromJson(JSONObject json) {
        if (json == null || !json.has("api_key")) return null;
        AuthTokens value = new AuthTokens(json.optString("api_key", ""));
        return value.isUsable() ? value : null;
    }

    public AuthTokens mergeRefresh(AuthTokens other) {
        return other != null && other.isUsable() ? other : this;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

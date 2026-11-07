package dev.bennett.claudemeter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONObject;

/**
 * Minimal JWT helper retained for compatibility with locally stored tokens.
 *
 * Claude account identity is fetched from the profile endpoint; JWT contents are not used as the
 * authoritative account source.
 */
public final class JwtClaims {
    public final String accountId;
    public final String email;

    public JwtClaims(String accountId, String email) {
        this.accountId = accountId == null ? "" : accountId;
        this.email = email == null ? "" : email;
    }

    public static JwtClaims fromTokens(String idToken, String accessToken) {
        JSONObject id = decodePayload(idToken);
        JSONObject access = decodePayload(accessToken);
        String email = firstNonEmpty(
                id == null ? "" : id.optString("email", ""),
                access == null ? "" : access.optString("email", ""));
        String accountId = firstNonEmpty(
                id == null ? "" : id.optString("sub", ""),
                access == null ? "" : access.optString("sub", ""));
        return new JwtClaims(accountId, email);
    }

    private static JSONObject decodePayload(String token) {
        if (token == null || token.isEmpty()) return null;
        String[] parts = token.split("\\.");
        if (parts.length < 2) return null;
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(parts[1]);
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}

package dev.lucsenl.claudemeter;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Claude subscription OAuth (authorization-code + PKCE). */
public final class ClaudeOAuth {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaudeOAuth() {}

    public static LoginRequest beginLogin() throws Exception {
        String verifier = randomUrlSafe(32);
        String state = randomUrlSafe(24);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256")
                        .digest(verifier.getBytes(StandardCharsets.US_ASCII)));

        String url = AppConstants.OAUTH_AUTHORIZE_URL
                + "?code=true"
                + "&client_id=" + encode(AppConstants.OAUTH_CLIENT_ID)
                + "&response_type=code"
                + "&redirect_uri=" + encode(AppConstants.OAUTH_REDIRECT_URI)
                + "&scope=" + encode(AppConstants.OAUTH_SCOPES)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256"
                + "&state=" + encode(state);
        return new LoginRequest(url, verifier, state);
    }

    public static ClaudeOAuthCredential exchangeCode(Context context, String input,
            String verifier, String expectedState) throws Exception {
        AuthorizationResult result = parseAuthorizationResult(input);
        if (result.code.isEmpty()) throw new Exception("Paste the authorization code from Claude.");
        if (!result.state.isEmpty() && !constantTimeEquals(result.state, expectedState)) {
            throw new Exception("Claude sign-in state did not match. Start sign-in again.");
        }

        JSONObject body = new JSONObject()
                .put("grant_type", "authorization_code")
                .put("client_id", AppConstants.OAUTH_CLIENT_ID)
                .put("code", result.code)
                .put("redirect_uri", AppConstants.OAUTH_REDIRECT_URI)
                .put("code_verifier", verifier)
                .put("state", result.state.isEmpty() ? expectedState : result.state);

        ClaudeOAuthCredential credential = tokenRequest(context, body, null);
        SecureTokenStore.save(context, credential);
        return credential;
    }

    public static ClaudeOAuthCredential validCredential(Context context, boolean forceRefresh)
            throws Exception {
        ClaudeOAuthCredential current = SecureTokenStore.load(context);
        if (current == null || !current.isUsable()) {
            throw new Exception("Sign in with Claude first.");
        }
        if (!forceRefresh && !current.shouldRefresh(System.currentTimeMillis())) return current;

        JSONObject body = new JSONObject()
                .put("grant_type", "refresh_token")
                .put("client_id", AppConstants.OAUTH_CLIENT_ID)
                .put("refresh_token", current.refreshToken);
        if (!current.scope.isEmpty()) body.put("scope", current.scope);

        ClaudeOAuthCredential refreshed = tokenRequest(context, body, current);
        SecureTokenStore.save(context, refreshed);
        return refreshed;
    }

    private static ClaudeOAuthCredential tokenRequest(Context context, JSONObject body,
            ClaudeOAuthCredential previous) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)
                URI.create(AppConstants.OAUTH_TOKEN_URL).toURL().openConnection();
        try {
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", AppConstants.userAgent());
            connection.setFixedLengthStreamingMode(payload.length);
            connection.getOutputStream().write(payload);

            int status = connection.getResponseCode();
            String response = readBody(connection, status);
            if (status < 200 || status >= 300) {
                String detail = safeOAuthError(response);
                throw new Exception("Claude sign-in failed (HTTP " + status + ")"
                        + (detail.isEmpty() ? "." : ": " + detail));
            }

            JSONObject json = new JSONObject(response);
            String access = json.optString("access_token", "");
            String refresh = json.optString("refresh_token",
                    previous == null ? "" : previous.refreshToken);
            long expiresIn = Math.max(60L, json.optLong("expires_in", 3600L));
            String scope = json.optString("scope", previous == null ? "" : previous.scope);
            ClaudeOAuthCredential value = new ClaudeOAuthCredential(
                    access, refresh, System.currentTimeMillis() + expiresIn * 1000L, scope);
            if (!value.isUsable()) throw new Exception("Claude returned incomplete OAuth tokens.");
            return value;
        } finally {
            connection.disconnect();
        }
    }

    public static AuthorizationResult parseAuthorizationResult(String input) {
        String raw = input == null ? "" : input.trim();
        if (raw.isEmpty()) return new AuthorizationResult("", "");

        try {
            if (raw.contains("://")) {
                URI uri = URI.create(raw);
                String query = uri.getRawQuery();
                return new AuthorizationResult(
                        queryValue(query, "code"), queryValue(query, "state"));
            }
        } catch (Exception ignored) {}

        if (raw.contains("#")) {
            String[] parts = raw.split("#", 2);
            return new AuthorizationResult(parts[0].trim(),
                    parts.length > 1 ? parts[1].trim() : "");
        }
        if (raw.contains("code=")) {
            return new AuthorizationResult(
                    queryValue(raw, "code"), queryValue(raw, "state"));
        }
        return new AuthorizationResult(raw, "");
    }

    private static String queryValue(String query, String name) {
        if (query == null) return "";
        for (String pair : query.split("[&#]")) {
            int index = pair.indexOf('=');
            if (index <= 0) continue;
            if (!pair.substring(0, index).equals(name)) continue;
            try {
                return java.net.URLDecoder.decode(
                        pair.substring(index + 1), StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                return pair.substring(index + 1);
            }
        }
        return "";
    }

    private static String safeOAuthError(String raw) {
        try {
            JSONObject json = new JSONObject(raw == null ? "{}" : raw);
            String value = json.optString("error_description", "");
            if (value.isEmpty()) {
                Object error = json.opt("error");
                if (error instanceof String) value = (String) error;
                else if (error instanceof JSONObject) {
                    value = ((JSONObject) error).optString("message", "");
                }
            }
            value = value.replaceAll("[\\r\\n\\t]+", " ").trim();
            return value.length() > 180 ? value.substring(0, 180) : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String readBody(HttpsURLConnection connection, int status) throws Exception {
        InputStream input = status >= 200 && status < 400
                ? connection.getInputStream() : connection.getErrorStream();
        if (input == null) return "";
        try (InputStream stream = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > 2 * 1024 * 1024) {
                    throw new Exception("Claude OAuth response was unexpectedly large.");
                }
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String randomUrlSafe(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static final class LoginRequest {
        public final String url;
        public final String verifier;
        public final String state;

        LoginRequest(String url, String verifier, String state) {
            this.url = url;
            this.verifier = verifier;
            this.state = state;
        }
    }

    public static final class AuthorizationResult {
        public final String code;
        public final String state;

        AuthorizationResult(String code, String state) {
            this.code = code == null ? "" : code.trim();
            this.state = state == null ? "" : state.trim();
        }
    }
}

package dev.lucsenl.claudemeter;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Android token exchange/refresh for Claude subscription OAuth. */
public final class ClaudeOAuth {
    private ClaudeOAuth() {}

    public static ClaudeOAuthCredential exchangeCode(Context context, String input,
            String verifier, String expectedState) throws Exception {
        ClaudeOAuthProtocol.AuthorizationResult result =
                ClaudeOAuthProtocol.parseAuthorizationResult(input);
        if (result.code.isEmpty()) throw new Exception("Paste the authorization code from Claude.");
        if (!ClaudeOAuthProtocol.stateMatches(result.state, expectedState)) {
            throw new Exception("Claude sign-in state did not match. Start sign-in again.");
        }

        JSONObject body = new JSONObject()
                .put("grant_type", "authorization_code")
                .put("client_id", ClaudeOAuthProtocol.CLIENT_ID)
                .put("code", result.code)
                .put("redirect_uri", ClaudeOAuthProtocol.REDIRECT_URI)
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
                .put("client_id", ClaudeOAuthProtocol.CLIENT_ID)
                .put("refresh_token", current.refreshToken);
        body.put("scope", current.scope.isEmpty()
                ? ClaudeOAuthProtocol.SCOPES : current.scope);

        ClaudeOAuthCredential refreshed = tokenRequest(context, body, current);
        SecureTokenStore.save(context, refreshed);
        return refreshed;
    }

    private static ClaudeOAuthCredential tokenRequest(Context context, JSONObject body,
            ClaudeOAuthCredential previous) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection)
                URI.create(ClaudeOAuthProtocol.TOKEN_URL).toURL().openConnection();
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
            String fallbackScope = previous == null || previous.scope.isEmpty()
                    ? ClaudeOAuthProtocol.SCOPES : previous.scope;
            String scope = json.optString("scope", fallbackScope);
            if (scope == null || scope.trim().isEmpty()) scope = fallbackScope;
            ClaudeOAuthCredential value = new ClaudeOAuthCredential(
                    access, refresh, System.currentTimeMillis() + expiresIn * 1000L, scope);
            if (!value.isUsable()) throw new Exception("Claude returned incomplete OAuth tokens.");
            return value;
        } finally {
            connection.disconnect();
        }
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
}

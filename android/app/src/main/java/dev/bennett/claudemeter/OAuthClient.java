package dev.bennett.claudemeter;

import android.content.Context;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public final class OAuthClient {
    private OAuthClient() {}

    /** A consumed or revoked OAuth grant needs browser reauthorization, not job retries. */
    public static final class InvalidGrantException extends Exception {
        public InvalidGrantException(String message) {
            super(message);
        }
    }

    public static AuthTokens exchangeCode(Context context, String code, String redirectUri,
            String verifier, String state) throws Exception {
        JSONObject body = new JSONObject();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("state", state);
        body.put("redirect_uri", redirectUri);
        body.put("client_id", AppConstants.OAUTH_CLIENT_ID);
        body.put("code_verifier", verifier);
        return parseTokens(postJson(context, "oauth_code_exchange", AppConstants.TOKEN_URL, body),
                null);
    }

    public static AuthTokens refresh(Context context, AuthTokens previous) throws Exception {
        if (previous == null || previous.refreshToken.isEmpty()) {
            throw new Exception("Claude authorization must be renewed.");
        }
        JSONObject body = new JSONObject();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", previous.refreshToken);
        body.put("client_id", AppConstants.OAUTH_CLIENT_ID);
        body.put("scope", previous.scope.isEmpty() ? AppConstants.OAUTH_SCOPE : previous.scope);
        return previous.mergeRefresh(parseTokens(
                postJson(context, "oauth_token_refresh", AppConstants.TOKEN_URL, body), previous));
    }

    /**
     * Claude's documented account controls are the source of truth for revocation.
     * Local sign-out removes this app's tokens; no unverified revocation route is called.
     */
    public static void revokeBestEffort(Context context, AuthTokens tokens) {
        DiagnosticLog.info(context, "auth", "local_sign_out_no_remote_revoke");
    }

    private static AuthTokens parseTokens(String json, AuthTokens previous) throws Exception {
        JSONObject object = new JSONObject(json);
        String accessToken = object.optString("access_token", "");
        String refreshToken = object.optString("refresh_token", "");
        if (previous != null && refreshToken.isEmpty()) {
            refreshToken = previous.refreshToken;
        }
        long expiresIn = Math.max(60L, object.optLong("expires_in", 3600L));
        AuthTokens result = new AuthTokens(
                accessToken,
                refreshToken,
                "",
                System.currentTimeMillis() + expiresIn * 1000L,
                previous == null ? "" : previous.accountId,
                previous == null ? "" : previous.email,
                object.optString("scope", previous == null ? AppConstants.OAUTH_SCOPE : previous.scope));
        if (!result.isUsable()) {
            throw new Exception("The Claude authorization server returned incomplete credentials.");
        }
        return result;
    }

    private static String postJson(Context context, String operation, String url, JSONObject object)
            throws Exception {
        byte[] bytes = object.toString().getBytes(StandardCharsets.UTF_8);
        HttpsURLConnection connection =
                (HttpsURLConnection) URI.create(url).toURL().openConnection();
        long started = SystemClock.elapsedRealtime();
        DiagnosticLog.info(context, "network", "request_started",
                "operation", operation, "method", "POST",
                "url", DiagnosticSanitizer.safeUrl(url), "request_bytes", bytes.length);
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(25000);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", AppConstants.userAgent());
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            String body = readBody(connection, status);
            DiagnosticLog.info(context, "network", "request_finished",
                    "operation", operation, "status", status,
                    "duration_ms", SystemClock.elapsedRealtime() - started,
                    "response_bytes", body.getBytes(StandardCharsets.UTF_8).length);
            if (status < 200 || status >= 300) {
                if (isInvalidGrant(body)) {
                    String message = "oauth_token_refresh".equals(operation)
                            ? "Claude session expired (invalid_grant). Sign in with Claude again."
                            : "Claude authorization code was rejected (invalid_grant). "
                                    + "Start a NEW Claude sign-in and paste its new code.";
                    throw new InvalidGrantException(message);
                }
                throw new Exception(readError(body,
                        "Claude authorization failed (HTTP " + status + ")."));
            }
            return body;
        } catch (Exception exception) {
            DiagnosticLog.error(context, "network", "request_failed", exception,
                    "operation", operation,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            throw exception;
        } finally {
            connection.disconnect();
        }
    }

    static String readBody(HttpURLConnection connection, int status) throws Exception {
        InputStream stream = (status < 200 || status >= 400)
                ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) return "";
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                if (output.size() > 2 * 1024 * 1024) {
                    throw new Exception("Server response was unexpectedly large.");
                }
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    static boolean isInvalidGrant(String body) {
        try {
            JSONObject json = new JSONObject(body == null ? "{}" : body);
            Object error = json.opt("error");
            if (error instanceof String) return "invalid_grant".equals(error);
            if (error instanceof JSONObject) {
                JSONObject detail = (JSONObject) error;
                return "invalid_grant".equals(detail.optString("type", ""))
                        || "invalid_grant".equals(detail.optString("code", ""));
            }
            return "invalid_grant".equals(json.optString("error_code", ""));
        } catch (Exception ignored) {
            return false;
        }
    }

    static String readError(String body, String fallback) {
        try {
            JSONObject object = new JSONObject(body == null ? "" : body);
            Object error = object.opt("error");
            if (error instanceof JSONObject) {
                String message = ((JSONObject) error).optString("message", "");
                if (!message.isEmpty()) return message;
            }
            if (error instanceof String && !((String) error).isEmpty()) return (String) error;
            String message = object.optString("message", "");
            if (!message.isEmpty()) return message;
            String description = object.optString("error_description", "");
            if (!description.isEmpty()) return description;
        } catch (Exception ignored) {}
        return fallback;
    }
}

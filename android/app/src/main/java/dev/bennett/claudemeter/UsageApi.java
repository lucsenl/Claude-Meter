package dev.bennett.claudemeter;

import android.content.Context;
import android.os.SystemClock;
import dev.bennett.claudemeter.wear.PhoneWearSync;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

public final class UsageApi {
    static final Object NETWORK_LOCK = new Object();

    private UsageApi() {}

    public static UsageSnapshot refreshAndCache(Context context) throws Exception {
        long started = SystemClock.elapsedRealtime();
        DiagnosticLog.info(context, "refresh", "usage_refresh_started");
        try {
            synchronized (NETWORK_LOCK) {
                AuthTokens tokens = usableTokens(context);
                Response usage = request(context, AppConstants.USAGE_URL, "usage", tokens);
                if (usage.status == 401) {
                    DiagnosticLog.warn(context, "auth", "usage_token_rejected_refreshing");
                    tokens = OAuthClient.refresh(context, tokens);
                    SecureTokenStore.save(context, tokens);
                    usage = request(context, AppConstants.USAGE_URL, "usage", tokens);
                }
                if (usage.status < 200 || usage.status >= 300) {
                    String fallback = usage.status == 403
                            ? "Claude usage access was denied for this account."
                            : usage.status == 404
                            ? "The Claude usage endpoint is unavailable or has changed."
                            : "Claude usage refresh failed (HTTP " + usage.status + ").";
                    throw new Exception(OAuthClient.readError(usage.body, fallback));
                }

                Profile profile = requestProfile(context, tokens);
                if (profile != null) {
                    tokens = new AuthTokens(
                            tokens.accessToken,
                            tokens.refreshToken,
                            tokens.idToken,
                            tokens.expiresAtMillis,
                            profile.accountId,
                            profile.email,
                            tokens.scope);
                    SecureTokenStore.save(context, tokens);
                }

                UsageSnapshot snapshot = UsageParser.parse(
                        usage.body,
                        System.currentTimeMillis(),
                        profile == null ? "" : profile.plan);
                if (!snapshot.hasDisplayableData()) {
                    throw new Exception("Anthropic returned no recognizable Claude usage data.");
                }

                UsageSnapshot previous = AppPreferences.loadSnapshot(context);
                if (!AppPreferences.saveSnapshot(context, snapshot)) {
                    throw new Exception(
                            "Claude usage was received, but it could not be saved on this device.");
                }
                UsageHistoryRecorder.record(context, snapshot);
                PhoneWearSync.pushUsage(context, snapshot);
                NowBarManager.onUsageUpdated(context, snapshot);
                ResetNotificationManager.onUsageUpdated(context, previous, snapshot);
                try {
                    ResetAlertScheduler.scheduleFromSnapshot(context, snapshot);
                } catch (RuntimeException exception) {
                    DiagnosticLog.error(context, "scheduler", "reset_alert_schedule_failed",
                            exception);
                }

                DiagnosticLog.info(context, "refresh", "usage_refresh_succeeded",
                        "duration_ms", SystemClock.elapsedRealtime() - started,
                        "five_hour", snapshot.fiveHour != null,
                        "weekly", snapshot.weekly != null,
                        "additional_limits", snapshot.additionalLimits.size(),
                        "extra_usage", snapshot.usageCredits != null);
                return snapshot;
            }
        } catch (Exception exception) {
            DiagnosticLog.error(context, "refresh", "usage_refresh_failed", exception,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            throw exception;
        }
    }

    static AuthTokens usableTokens(Context context) throws Exception {
        AuthTokens tokens = SecureTokenStore.load(context);
        if (tokens == null) {
            throw new Exception("Sign in to Claude first.");
        }
        if (tokens.shouldRefresh(System.currentTimeMillis())) {
            DiagnosticLog.info(context, "auth", "token_refresh_due");
            tokens = OAuthClient.refresh(context, tokens);
            SecureTokenStore.save(context, tokens);
        }
        return tokens;
    }

    private static Profile requestProfile(Context context, AuthTokens tokens) {
        try {
            Response response = request(context, AppConstants.PROFILE_URL, "profile", tokens);
            if (response.status < 200 || response.status >= 300) {
                DiagnosticLog.warn(context, "profile", "profile_request_rejected",
                        "status", response.status);
                return null;
            }
            JSONObject root = new JSONObject(response.body);
            JSONObject account = root.optJSONObject("account");
            if (account == null) return null;
            String email = account.optString("email", "").trim();
            String accountId = firstNonEmpty(
                    account.optString("uuid", ""),
                    account.optString("id", ""),
                    tokens.accountId);
            String plan = account.optBoolean("has_claude_max", false) ? "Max"
                    : account.optBoolean("has_claude_pro", false) ? "Pro" : "";
            return new Profile(accountId, email, plan);
        } catch (Exception exception) {
            DiagnosticLog.error(context, "profile", "profile_request_failed", exception);
            return null;
        }
    }

    private static Response request(Context context, String url, String operation,
            AuthTokens tokens) throws Exception {
        HttpsURLConnection connection =
                (HttpsURLConnection) URI.create(url).toURL().openConnection();
        long started = SystemClock.elapsedRealtime();
        DiagnosticLog.info(context, "network", "request_started",
                "operation", operation,
                "method", "GET",
                "url", DiagnosticSanitizer.safeUrl(url));
        try {
            applyHeaders(connection, tokens);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            String body = OAuthClient.readBody(connection, status);
            DiagnosticLog.info(context, "network", "request_finished",
                    "operation", operation,
                    "status", status,
                    "duration_ms", SystemClock.elapsedRealtime() - started,
                    "response_bytes", body.getBytes(StandardCharsets.UTF_8).length);
            return new Response(status, body);
        } catch (Exception exception) {
            DiagnosticLog.error(context, "network", "request_failed", exception,
                    "operation", operation,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            throw exception;
        } finally {
            connection.disconnect();
        }
    }

    static void applyHeaders(HttpsURLConnection connection, AuthTokens tokens) {
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + tokens.accessToken);
        connection.setRequestProperty("anthropic-beta", AppConstants.OAUTH_BETA);
        connection.setRequestProperty("User-Agent", AppConstants.userAgent());
    }

    // Compatibility hooks for code that still shares the old refresh lock.
    static void installCookieManager() {
        // Claude OAuth API calls use bearer authorization and do not require a cookie jar.
    }

    static String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (message == null || message.trim().isEmpty()) return "Claude usage refresh failed.";
        String clean = message.trim();
        return clean.length() > 240 ? clean.substring(0, 240) : clean;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static final class Profile {
        final String accountId;
        final String email;
        final String plan;

        Profile(String accountId, String email, String plan) {
            this.accountId = accountId == null ? "" : accountId;
            this.email = email == null ? "" : email;
            this.plan = plan == null ? "" : plan;
        }
    }

    private static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }
}

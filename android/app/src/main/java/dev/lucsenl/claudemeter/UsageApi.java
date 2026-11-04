package dev.lucsenl.claudemeter;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import dev.lucsenl.claudemeter.wear.PhoneWearSync;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.net.ssl.HttpsURLConnection;

/**
 * Anthropic public API client.
 *
 * Claude Meter never calls claude.ai/Claude Code private quota endpoints. A normal API key can
 * validate against /v1/models. Organization analytics require an Admin/Analytics-capable key; when
 * that capability is absent the app keeps the key connected and reports analytics as unavailable.
 */
public final class UsageApi {
    private UsageApi() {}

    public static void validateAndStoreApiKey(Context context, String apiKey) throws Exception {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isEmpty()) throw new Exception("Enter an Anthropic API key.");
        Response response = request(context, AppConstants.MODELS_URL + "?limit=1", key, false);
        if (response.status == 401 || response.status == 403) {
            throw new Exception("Anthropic rejected this API key.");
        }
        if (response.status < 200 || response.status >= 300) {
            throw new Exception("Could not validate the API key (HTTP " + response.status + ").");
        }
        SecureTokenStore.save(context, new AuthTokens(key));
    }

    public static UsageSnapshot refreshAndCacheLocked(Context context) throws Exception {
        Context app = context.getApplicationContext();
        AuthTokens credential = SecureTokenStore.load(app);
        if (credential == null || !credential.isUsable()) {
            throw new Exception("Add an Anthropic API key first.");
        }

        long started = SystemClock.elapsedRealtime();
        String key = credential.accessToken;
        String modelsJson = "";
        String organizationJson = "";
        try {
            Response models = request(app, AppConstants.MODELS_URL + "?limit=100", key, false);
            requireCredentialResponse(models);
            if (models.status >= 200 && models.status < 300) modelsJson = models.body;

            Response organization = request(app, AppConstants.ORGANIZATION_URL, key, false);
            if (organization.status >= 200 && organization.status < 300) {
                organizationJson = organization.body;
            }

            Instant now = Instant.now();
            ZonedDateTime utc = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
            Instant todayStart = utc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant weekStart = now.minusSeconds(7L * 24L * 60L * 60L);
            Instant monthStart = now.minusSeconds(30L * 24L * 60L * 60L);

            Response today = analytics(app, AppConstants.ANALYTICS_USAGE_URL,
                    todayStart, now, key);
            if (today.status == 401) throw new Exception("Anthropic rejected this API key.");
            if (today.status == 403) {
                UsageSnapshot snapshot = UsageParser.analyticsUnavailable(
                        "This key can use the Claude API, but it does not have organization "
                                + "Analytics access. Use an Admin API key or an Enterprise "
                                + "read:analytics credential to view usage and cost.",
                        modelsJson, organizationJson, System.currentTimeMillis());
                persist(app, snapshot);
                return snapshot;
            }
            requireSuccess(today, "usage");

            Response week = analytics(app, AppConstants.ANALYTICS_USAGE_URL,
                    weekStart, now, key);
            requireSuccess(week, "7-day usage");

            Response cost = analytics(app, AppConstants.ANALYTICS_COST_URL,
                    monthStart, now, key);
            requireSuccess(cost, "cost");

            UsageSnapshot snapshot = UsageParser.parseAnalytics(
                    today.body, week.body, cost.body, modelsJson, organizationJson,
                    System.currentTimeMillis());
            persist(app, snapshot);
            DiagnosticLog.info(app, "refresh", "anthropic_analytics_refresh_succeeded",
                    "duration_ms", SystemClock.elapsedRealtime() - started,
                    "today_tokens", snapshot.todayTokens,
                    "week_tokens", snapshot.weekTokens,
                    "models", snapshot.activeModels.size());
            return snapshot;
        } catch (Exception exception) {
            AppPreferences.setLastError(app, safeMessage(exception));
            DiagnosticLog.error(app, "refresh", "anthropic_analytics_refresh_failed", exception,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            PhoneWearSync.pushStatus(app, false, safeMessage(exception));
            throw exception;
        }
    }

    private static void persist(Context context, UsageSnapshot snapshot) {
        AppPreferences.saveSnapshot(context, snapshot);
        AppPreferences.setLastError(context, "");
        WidgetRenderer.updateAll(context);
        PhoneWearSync.pushUsage(context, snapshot);
        context.sendBroadcast(new Intent(AppConstants.ACTION_USAGE_UPDATED)
                .setPackage(context.getPackageName()), AppConstants.INTERNAL_PERMISSION);
    }

    private static Response analytics(Context context, String base, Instant start, Instant end,
            String key) throws Exception {
        String url = base
                + "?starting_at=" + encode(start.toString())
                + "&ending_at=" + encode(end.toString());
        return request(context, url, key, true);
    }

    private static Response request(Context context, String url, String key, boolean analytics)
            throws Exception {
        HttpsURLConnection connection =
                (HttpsURLConnection) URI.create(url).toURL().openConnection();
        long started = SystemClock.elapsedRealtime();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(25000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-api-key", key);
            connection.setRequestProperty("anthropic-version", AppConstants.API_VERSION);
            connection.setRequestProperty("User-Agent", AppConstants.userAgent());
            if (analytics) {
                connection.setRequestProperty("anthropic-beta", AppConstants.ANALYTICS_BETA);
            }
            int status = connection.getResponseCode();
            String body = OAuthClient.readBody(connection, status);
            DiagnosticLog.info(context, "network", "request_finished",
                    "host", "api.anthropic.com",
                    "status", status,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            return new Response(status, body);
        } finally {
            connection.disconnect();
        }
    }

    private static void requireCredentialResponse(Response response) throws Exception {
        if (response.status == 401 || response.status == 403) {
            throw new Exception("Anthropic rejected this API key.");
        }
    }

    private static void requireSuccess(Response response, String operation) throws Exception {
        if (response.status >= 200 && response.status < 300) return;
        if (response.status == 401 || response.status == 403) {
            throw new Exception("The Anthropic credential cannot read " + operation + ".");
        }
        throw new Exception("Anthropic " + operation + " request failed (HTTP "
                + response.status + ").");
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    static String safeMessage(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        if (message == null || message.trim().isEmpty()) return "Anthropic request failed.";
        String value = message.trim();
        return value.length() > 240 ? value.substring(0, 240) : value;
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

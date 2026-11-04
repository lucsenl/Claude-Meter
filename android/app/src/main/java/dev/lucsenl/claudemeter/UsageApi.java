package dev.lucsenl.claudemeter;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.net.ssl.HttpsURLConnection;

/** Public Anthropic API transport. No claude.ai or Claude Code private endpoints. */
public final class UsageApi {
    private UsageApi() {}

    public static void validateAndStoreApiKey(Context context, String apiKey) throws Exception {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isEmpty()) throw new Exception("Enter an Anthropic credential.");

        Response models = request(context, AppConstants.MODELS_URL + "?limit=1",
                key, AuthMode.BEARER, false);
        if (isSuccess(models)) {
            SecureTokenStore.save(context, new AuthTokens(key));
            return;
        }

        Instant end = Instant.now();
        Instant start = end.minusSeconds(24L * 60L * 60L);
        Response analytics = analytics(context, AppConstants.ANALYTICS_USAGE_URL, start, end, key);
        if (isSuccess(analytics)) {
            SecureTokenStore.save(context, new AuthTokens(key));
            return;
        }

        if (models.status == 401 || models.status == 403
                || analytics.status == 401 || analytics.status == 403) {
            throw new Exception(
                    "Anthropic rejected this credential or it lacks access to the selected API.");
        }
        throw new Exception("Could not validate the Anthropic credential.");
    }

    public static UsageSnapshot refreshAndCacheLocked(Context context) throws Exception {
        Context app = context.getApplicationContext();
        AuthTokens credential = SecureTokenStore.load(app);
        if (credential == null || !credential.isUsable()) {
            throw new Exception("Add an Anthropic credential first.");
        }

        long started = SystemClock.elapsedRealtime();
        String key = credential.accessToken;
        String modelsJson = "";
        try {
            Response models = request(app, AppConstants.MODELS_URL + "?limit=100",
                    key, AuthMode.BEARER, false);
            if (isSuccess(models)) modelsJson = models.body;

            Instant now = Instant.now();
            ZonedDateTime utc = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
            Instant todayStart = utc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant weekStart = now.minusSeconds(7L * 24L * 60L * 60L);
            Instant monthStart = now.minusSeconds(30L * 24L * 60L * 60L);

            Response today = analytics(app, AppConstants.ANALYTICS_USAGE_URL,
                    todayStart, now, key);
            if (today.status == 401 || today.status == 403) {
                if (modelsJson.isEmpty()) {
                    throw new Exception(
                            "Anthropic rejected this credential or it cannot read Models/Analytics.");
                }
                UsageSnapshot snapshot = UsageParser.analyticsUnavailable(
                        "The credential can use the Claude API, but organization Analytics "
                                + "is unavailable. Use an eligible Admin/Analytics credential "
                                + "to view token usage and cost.",
                        modelsJson, "", System.currentTimeMillis());
                persist(app, snapshot);
                return snapshot;
            }
            requireSuccess(today, "usage analytics");

            Response week = analytics(app, AppConstants.ANALYTICS_USAGE_URL,
                    weekStart, now, key);
            requireSuccess(week, "7-day usage analytics");

            Response cost = analytics(app, AppConstants.ANALYTICS_COST_URL,
                    monthStart, now, key);
            requireSuccess(cost, "cost analytics");

            UsageSnapshot snapshot = UsageParser.parseAnalytics(
                    today.body, week.body, cost.body, modelsJson, "",
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
            throw exception;
        }
    }

    private static void persist(Context context, UsageSnapshot snapshot) {
        AppPreferences.saveSnapshot(context, snapshot);
        AppPreferences.setLastError(context, "");
        WidgetRenderer.updateAll(context);
        context.sendBroadcast(new Intent(AppConstants.ACTION_USAGE_UPDATED)
                .setPackage(context.getPackageName()), AppConstants.INTERNAL_PERMISSION);
    }

    private static Response analytics(Context context, String base, Instant start, Instant end,
            String key) throws Exception {
        String url = base
                + "?starting_at=" + encode(start.toString())
                + "&ending_at=" + encode(end.toString())
                + "&limit=1000";
        Response response = request(context, url, key, AuthMode.API_KEY, true);
        if (response.status == 401 || response.status == 403) {
            response = request(context, url, key, AuthMode.BEARER, true);
        }
        return response;
    }

    private static Response request(Context context, String url, String key, AuthMode authMode,
            boolean analytics) throws Exception {
        HttpsURLConnection connection =
                (HttpsURLConnection) URI.create(url).toURL().openConnection();
        long started = SystemClock.elapsedRealtime();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("anthropic-version", AppConstants.API_VERSION);
            connection.setRequestProperty("User-Agent", AppConstants.userAgent());
            if (authMode == AuthMode.BEARER) {
                connection.setRequestProperty("Authorization", "Bearer " + key);
            } else {
                connection.setRequestProperty("x-api-key", key);
            }
            if (analytics) {
                connection.setRequestProperty("anthropic-beta", AppConstants.ANALYTICS_BETA);
            }

            int status = connection.getResponseCode();
            String body = readBody(connection, status);
            DiagnosticLog.info(context, "network", "anthropic_request_finished",
                    "host", "api.anthropic.com",
                    "status", status,
                    "analytics", analytics,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            return new Response(status, body);
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(HttpsURLConnection connection, int status) throws Exception {
        InputStream input = status >= 200 && status < 400
                ? connection.getInputStream() : connection.getErrorStream();
        if (input == null) return "";
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                if (output.size() > 4 * 1024 * 1024) {
                    throw new Exception("Anthropic response exceeded the supported size.");
                }
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static boolean isSuccess(Response response) {
        return response.status >= 200 && response.status < 300;
    }

    private static void requireSuccess(Response response, String operation) throws Exception {
        if (isSuccess(response)) return;
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

    private enum AuthMode {
        API_KEY,
        BEARER
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

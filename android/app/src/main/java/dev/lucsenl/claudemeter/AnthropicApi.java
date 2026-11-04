package dev.lucsenl.claudemeter;

import android.content.Context;
import android.os.SystemClock;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpsURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Public Anthropic API client.
 *
 * This class intentionally does not use claude.ai or Claude Code private endpoints.
 */
public final class AnthropicApi {
    public static final String BASE_URL = "https://api.anthropic.com";
    public static final String API_VERSION = "2026-10-21";
    public static final String ANALYTICS_BETA = "analytics-usage-cost-2026-08-24";

    private AnthropicApi() {}

    public static AnthropicSnapshot refreshAndCache(Context context) throws Exception {
        AnthropicCredential credential = SecureCredentialStore.load(context);
        if (credential == null) {
            throw new Exception("Configure an Anthropic credential first.");
        }
        long started = SystemClock.elapsedRealtime();
        DiagnosticLog.info(context, "refresh", "anthropic_refresh_started",
                "credential_type", credential.kind.wireName);

        List<AnthropicModel> models = new ArrayList<>();
        AnalyticsTotals analytics = null;
        String analyticsMessage;

        if (credential.supportsOrganizationAnalytics()) {
            analytics = fetchAnalytics(context, credential);
            analyticsMessage = "Organization analytics for the last 30 days.";
            try {
                models = fetchModels(context, credential);
            } catch (HttpFailure ignored) {
                // Admin/analytics credentials are not necessarily inference credentials.
            }
        } else {
            models = fetchModels(context, credential);
            analyticsMessage = "Organization usage/cost analytics require an Admin or Analytics credential.";
        }

        AnthropicSnapshot snapshot = new AnthropicSnapshot(
                System.currentTimeMillis(),
                models,
                analytics != null,
                analytics == null ? 0L : analytics.inputTokens,
                analytics == null ? 0L : analytics.outputTokens,
                analytics == null ? 0L : analytics.totalTokens,
                analytics == null ? 0L : analytics.requests,
                analytics == null ? "" : analytics.costUsd,
                analyticsMessage);

        if (!AnthropicStateStore.save(context, snapshot)) {
            throw new Exception("Anthropic data was received but could not be saved on this device.");
        }

        DiagnosticLog.info(context, "refresh", "anthropic_refresh_succeeded",
                "duration_ms", SystemClock.elapsedRealtime() - started,
                "models", snapshot.models.size(),
                "analytics", snapshot.analyticsAvailable);
        return snapshot;
    }

    public static void validate(Context context, AnthropicCredential credential) throws Exception {
        if (credential == null || !credential.isUsable()) {
            throw new Exception("Enter an Anthropic credential.");
        }
        if (credential.supportsOrganizationAnalytics()) {
            fetchAnalytics(context, credential);
        } else {
            fetchModels(context, credential);
        }
    }

    private static List<AnthropicModel> fetchModels(Context context,
            AnthropicCredential credential) throws Exception {
        Response response = get(context, credential, BASE_URL + "/v1/models?limit=100", false);
        requireSuccess(response, "Claude API credential validation failed");
        JSONObject root = new JSONObject(response.body);
        JSONArray data = root.optJSONArray("data");
        ArrayList<AnthropicModel> models = new ArrayList<>();
        if (data != null) {
            for (int i = 0; i < data.length(); i++) {
                AnthropicModel model = AnthropicModel.fromJson(data.optJSONObject(i));
                if (model != null) models.add(model);
            }
        }
        return models;
    }

    private static AnalyticsTotals fetchAnalytics(Context context,
            AnthropicCredential credential) throws Exception {
        String startingAt = Instant.now().minus(30, ChronoUnit.DAYS).toString();
        String endingAt = Instant.now().toString();
        String range = "?starting_at=" + encode(startingAt)
                + "&ending_at=" + encode(endingAt)
                + "&limit=1000";

        Response usageResponse = get(context, credential,
                BASE_URL + "/v1/organizations/analytics/usage" + range
                        + "&order_by=total_tokens",
                true);
        requireSuccess(usageResponse, "Anthropic usage analytics request failed");

        Response costResponse = get(context, credential,
                BASE_URL + "/v1/organizations/analytics/cost" + range
                        + "&order_by=amount",
                true);
        requireSuccess(costResponse, "Anthropic cost analytics request failed");

        AnalyticsTotals totals = parseUsage(new JSONObject(usageResponse.body));
        totals.costUsd = parseCostUsd(new JSONObject(costResponse.body));
        return totals;
    }

    static AnalyticsTotals parseUsage(JSONObject root) {
        AnalyticsTotals totals = new AnalyticsTotals();
        JSONArray data = root == null ? null : root.optJSONArray("data");
        if (data == null) return totals;
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.optJSONObject(i);
            JSONObject usage = row == null ? null : row.optJSONObject("usage");
            if (usage == null) continue;
            long input = firstLong(usage,
                    "input_tokens", "uncached_input_tokens");
            input += usage.optLong("cache_read_input_tokens", 0L);
            input += usage.optLong("cache_creation_input_tokens", 0L);
            long output = usage.optLong("output_tokens", 0L);
            long total = usage.optLong("total_tokens", input + output);
            totals.inputTokens += Math.max(0L, input);
            totals.outputTokens += Math.max(0L, output);
            totals.totalTokens += Math.max(0L, total);
            totals.requests += Math.max(0L, usage.optLong("requests", 0L));
        }
        return totals;
    }

    static String parseCostUsd(JSONObject root) {
        BigDecimal cents = BigDecimal.ZERO;
        JSONArray data = root == null ? null : root.optJSONArray("data");
        if (data == null) return "0.00";
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.optJSONObject(i);
            JSONObject cost = row == null ? null : row.optJSONObject("cost");
            if (cost == null) continue;
            String amount = cost.optString("amount", "0");
            try {
                cents = cents.add(new BigDecimal(amount));
            } catch (NumberFormatException ignored) {
            }
        }
        return cents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
    }

    private static long firstLong(JSONObject object, String first, String second) {
        if (object.has(first)) return object.optLong(first, 0L);
        return object.optLong(second, 0L);
    }

    private static Response get(Context context, AnthropicCredential credential,
            String url, boolean analytics) throws Exception {
        HttpsURLConnection connection =
                (HttpsURLConnection) URI.create(url).toURL().openConnection();
        long started = SystemClock.elapsedRealtime();
        DiagnosticLog.info(context, "network", "anthropic_request_started",
                "url", DiagnosticSanitizer.safeUrl(url),
                "analytics", analytics);
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("anthropic-version", API_VERSION);
            connection.setRequestProperty("User-Agent", AppConstants.userAgent());

            if (analytics || credential.kind == AnthropicCredential.Kind.ORGANIZATION_KEY) {
                connection.setRequestProperty("x-api-key", credential.secret);
            } else {
                connection.setRequestProperty("Authorization", "Bearer " + credential.secret);
                if (!credential.workspaceId.isEmpty()) {
                    connection.setRequestProperty("anthropic-workspace-id", credential.workspaceId);
                }
            }
            if (analytics) {
                connection.setRequestProperty("anthropic-beta", ANALYTICS_BETA);
            }

            int status = connection.getResponseCode();
            String body = readBody(connection, status);
            DiagnosticLog.info(context, "network", "anthropic_request_finished",
                    "status", status,
                    "duration_ms", SystemClock.elapsedRealtime() - started);
            return new Response(status, body);
        } finally {
            connection.disconnect();
        }
    }

    private static void requireSuccess(Response response, String fallback) throws Exception {
        if (response.status >= 200 && response.status < 300) return;
        String message = errorMessage(response.body);
        if (response.status == 401) {
            throw new HttpFailure(response.status,
                    message.isEmpty() ? "Anthropic rejected the credential." : message);
        }
        if (response.status == 403) {
            throw new HttpFailure(response.status,
                    message.isEmpty() ? "This credential does not have access to that Anthropic API." : message);
        }
        throw new HttpFailure(response.status,
                message.isEmpty() ? fallback + " (HTTP " + response.status + ")." : message);
    }

    private static String errorMessage(String body) {
        try {
            JSONObject root = new JSONObject(body == null ? "" : body);
            JSONObject error = root.optJSONObject("error");
            if (error != null) return error.optString("message", "");
            return root.optString("message", "");
        } catch (Exception ignored) {
            return "";
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

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    static final class AnalyticsTotals {
        long inputTokens;
        long outputTokens;
        long totalTokens;
        long requests;
        String costUsd = "0.00";
    }

    private static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }
    }

    static final class HttpFailure extends Exception {
        final int status;

        HttpFailure(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}

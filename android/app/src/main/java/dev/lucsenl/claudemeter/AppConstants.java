package dev.lucsenl.claudemeter;

import android.os.Build;

/** Public Anthropic API contracts used by Claude Meter. */
public final class AppConstants {
    public static final String ACTION_USAGE_UPDATED =
            "dev.lucsenl.claudemeter.action.USAGE_UPDATED";
    public static final String ACTION_RELEASES_UPDATED =
            "dev.lucsenl.claudemeter.action.RELEASES_UPDATED";
    public static final String ACTION_INSTALL_STATUS =
            "dev.lucsenl.claudemeter.action.INSTALL_STATUS";
    public static final String ACTION_REFRESH_WIDGET =
            "dev.lucsenl.claudemeter.action.REFRESH_WIDGET";
    public static final String INTERNAL_PERMISSION =
            "dev.lucsenl.claudemeter.permission.INTERNAL";

    public static final String API_BASE = "https://api.anthropic.com";
    public static final String MODELS_URL = API_BASE + "/v1/models";
    public static final String ORGANIZATION_URL = API_BASE + "/v1/organizations/me";
    public static final String ANALYTICS_USAGE_URL =
            API_BASE + "/v1/organizations/analytics/usage";
    public static final String ANALYTICS_COST_URL =
            API_BASE + "/v1/organizations/analytics/cost";
    public static final String API_VERSION = "2026-10-21";
    public static final String ANALYTICS_BETA = "analytics-usage-cost-2026-08-24";
    public static final String CONSOLE_KEYS_URL = "https://platform.claude.com/settings/keys";
    public static final String API_DOCS_URL = "https://platform.claude.com/docs/en/api/overview";

    public static final int VERSION_CODE = 30;
    public static final String VERSION_NAME = "2.8.0";

    private AppConstants() {}

    public static String userAgent() {
        return "claude-meter-android/" + VERSION_NAME + " (Android "
                + (Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE)
                + "; " + (Build.MODEL == null ? "Android" : Build.MODEL) + ")";
    }

    public static String updaterUserAgent() {
        return "claude-meter-android/" + VERSION_NAME + " updater";
    }
}

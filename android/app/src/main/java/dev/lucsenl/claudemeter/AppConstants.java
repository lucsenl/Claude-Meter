package dev.lucsenl.claudemeter;

import android.os.Build;

/** Claude subscription OAuth and usage contracts used by Claude Meter. */
public final class AppConstants {
    public static final String ACTION_USAGE_UPDATED =
            "dev.lucsenl.claudemeter.action.USAGE_UPDATED";
    public static final String ACTION_REFRESH_WIDGET =
            "dev.lucsenl.claudemeter.action.REFRESH_WIDGET";
    public static final String INTERNAL_PERMISSION =
            "dev.lucsenl.claudemeter.permission.INTERNAL";

    public static final String OAUTH_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
    public static final String OAUTH_AUTHORIZE_URL = "https://claude.com/cai/oauth/authorize";
    public static final String OAUTH_TOKEN_URL = "https://platform.claude.com/v1/oauth/token";
    public static final String OAUTH_REDIRECT_URI =
            "https://platform.claude.com/oauth/code/callback";
    public static final String OAUTH_SCOPES = "user:profile user:inference";
    public static final String OAUTH_BETA = "oauth-2025-04-20";
    public static final String USAGE_URL = "https://api.anthropic.com/api/oauth/usage";
    public static final String PROFILE_URL = "https://api.anthropic.com/api/oauth/profile";
    public static final String PROJECT_URL = "https://github.com/lucsenl/Claude-Meter";

    public static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;

    private AppConstants() {}

    public static String userAgent() {
        return "claude-meter-android/" + VERSION_NAME + " (Android "
                + (Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE)
                + "; " + (Build.MODEL == null ? "Android" : Build.MODEL) + ")";
    }
}

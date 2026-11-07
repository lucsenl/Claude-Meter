package dev.bennett.claudemeter;

import android.os.Build;

public final class AppConstants {
    public static final String ACTION_OAUTH_READY = "dev.bennett.claudemeter.action.OAUTH_READY";
    public static final String ACTION_OAUTH_RESULT = "dev.bennett.claudemeter.action.OAUTH_RESULT";
    public static final String ACTION_INSTALL_STATUS = "dev.bennett.claudemeter.action.INSTALL_STATUS";
    public static final String ACTION_RELEASES_UPDATED = "dev.bennett.claudemeter.action.RELEASES_UPDATED";
    public static final String ACTION_REFRESH_WIDGET = "dev.bennett.claudemeter.action.REFRESH_WIDGET";
    public static final String ACTION_RESET_ALERT = "dev.bennett.claudemeter.action.RESET_ALERT";
    public static final String ACTION_USAGE_UPDATED = "dev.bennett.claudemeter.action.USAGE_UPDATED";
    // Local compatibility actions while legacy UI code is being removed.
    public static final String ACTION_RESET_CREDITS_UPDATED =
            "dev.bennett.claudemeter.action.RESET_CREDITS_UPDATED";
    public static final String APP_LINK = "claudemeter://auth/complete";
    public static final String EXTRA_CREDIT_ID = "credit_id";
    public static final String EXTRA_PROMPT_USE_RESET = "prompt_use_reset";

    public static final String AUTHORIZE_URL = "https://claude.com/cai/oauth/authorize";
    public static final String TOKEN_URL = "https://platform.claude.com/v1/oauth/token";
    public static final String USAGE_URL = "https://api.anthropic.com/api/oauth/usage";
    public static final String PROFILE_URL = "https://api.anthropic.com/api/oauth/profile";
    public static final String OAUTH_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
    public static final String OAUTH_SCOPE =
            "user:profile user:inference";
    public static final String OAUTH_BETA = "oauth-2025-04-20";
    public static final String REDIRECT_URI = "https://platform.claude.com/oauth/code/callback";

    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_SUCCESS = "success";
    public static final String INTERNAL_PERMISSION = "dev.bennett.claudemeter.permission.INTERNAL";

    public static final int VERSION_CODE = 4;
    public static final String VERSION_NAME = "0.2.2";

    private AppConstants() {}

    public static String userAgent() {
        return "claude-meter-android/" + VERSION_NAME + " (Android "
                + (Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE) + "; "
                + (Build.MODEL == null ? "Android" : Build.MODEL) + ")";
    }

    public static String updaterUserAgent() {
        return "claude-meter-android/" + VERSION_NAME + " updater";
    }
}

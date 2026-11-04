package dev.lucsenl.claudemeter;

import android.os.Build;

/* JADX INFO: loaded from: classes.dex */
public final class AppConstants {
    public static final String ACTION_OAUTH_READY = "dev.lucsenl.claudemeter.action.OAUTH_READY";
    public static final String ACTION_OAUTH_RESULT = "dev.lucsenl.claudemeter.action.OAUTH_RESULT";
    public static final String ACTION_INSTALL_STATUS = "dev.lucsenl.claudemeter.action.INSTALL_STATUS";
    public static final String ACTION_RELEASES_UPDATED = "dev.lucsenl.claudemeter.action.RELEASES_UPDATED";
    public static final String ACTION_REFRESH_WIDGET = "dev.lucsenl.claudemeter.action.REFRESH_WIDGET";
    public static final String ACTION_RESET_ALERT = "dev.lucsenl.claudemeter.action.RESET_ALERT";
    public static final String ACTION_RESET_CREDIT_EXPIRY_ALERT =
            "dev.lucsenl.claudemeter.action.RESET_CREDIT_EXPIRY_ALERT";
    public static final String ACTION_RESET_CREDITS_UPDATED = "dev.lucsenl.claudemeter.action.RESET_CREDITS_UPDATED";
    public static final String ACTION_USAGE_UPDATED = "dev.lucsenl.claudemeter.action.USAGE_UPDATED";
    public static final String APP_LINK = "claudemeter://auth/complete";
    public static final String AUTHORIZE_URL = "https://auth.anthropic.com/oauth/authorize";
    public static final String AUTH_BASE = "https://auth.anthropic.com";
    public static final String CHATGPT_BACKEND = "https://claude.com/backend-api";
    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_CREDIT_ID = "credit_id";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_PROMPT_USE_RESET = "prompt_use_reset";
    public static final String EXTRA_SUCCESS = "success";
    public static final String INTERNAL_PERMISSION = "dev.lucsenl.claudemeter.permission.INTERNAL";
    public static final String OAUTH_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann";
    public static final int[] OAUTH_PORTS = {1455, 1457};
    public static final String OAUTH_SCOPE = "openid profile email offline_access";
    public static final String ORIGINATOR = "claude-meter-android";
    public static final String RESET_CREDITS_CONSUME_URL = "https://claude.com/backend-api/wham/rate-limit-reset-credits/consume";
    public static final String RESET_CREDITS_URL = "https://claude.com/backend-api/wham/rate-limit-reset-credits";
    public static final String REVOKE_URL = "https://auth.anthropic.com/oauth/revoke";
    public static final String TOKEN_URL = "https://auth.anthropic.com/oauth/token";
    public static final String USAGE_URL = "https://claude.com/backend-api/wham/usage";
    public static final int VERSION_CODE = 30;
    public static final String VERSION_NAME = "2.8.0";

    private AppConstants() {
    }

    public static String userAgent() {
        return "claude-meter-android/2.8.0 (Android " + (Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE) + "; " + (Build.MODEL == null ? "Android" : Build.MODEL) + ")";
    }

    public static String updaterUserAgent() {
        return "claude-meter-android/" + VERSION_NAME + " updater";
    }
}

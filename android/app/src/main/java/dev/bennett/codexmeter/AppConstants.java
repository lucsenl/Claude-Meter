package dev.bennett.codexmeter;

import android.os.Build;

/* JADX INFO: loaded from: classes.dex */
public final class AppConstants {
    public static final String ACTION_OAUTH_READY = "dev.bennett.codexmeter.action.OAUTH_READY";
    public static final String ACTION_OAUTH_RESULT = "dev.bennett.codexmeter.action.OAUTH_RESULT";
    public static final String ACTION_INSTALL_STATUS = "dev.bennett.codexmeter.action.INSTALL_STATUS";
    public static final String ACTION_RELEASES_UPDATED = "dev.bennett.codexmeter.action.RELEASES_UPDATED";
    public static final String ACTION_REFRESH_WIDGET = "dev.bennett.codexmeter.action.REFRESH_WIDGET";
    public static final String ACTION_RESET_ALERT = "dev.bennett.codexmeter.action.RESET_ALERT";
    public static final String ACTION_RESET_CREDIT_EXPIRY_ALERT =
            "dev.bennett.codexmeter.action.RESET_CREDIT_EXPIRY_ALERT";
    public static final String ACTION_RESET_CREDITS_UPDATED = "dev.bennett.codexmeter.action.RESET_CREDITS_UPDATED";
    public static final String ACTION_USAGE_UPDATED = "dev.bennett.codexmeter.action.USAGE_UPDATED";
    public static final String APP_LINK = "codexmeter://auth/complete";
    public static final String AUTHORIZE_URL = "https://claude.com/cai/oauth/authorize";
    public static final String AUTH_BASE = "https://platform.claude.com";
    public static final String CHATGPT_BACKEND = "https://api.anthropic.com";
    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_CREDIT_ID = "credit_id";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_PROMPT_USE_RESET = "prompt_use_reset";
    public static final String EXTRA_SUCCESS = "success";
    public static final String INTERNAL_PERMISSION = "dev.bennett.codexmeter.permission.INTERNAL";
    public static final String OAUTH_CLIENT_ID = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
    public static final int[] OAUTH_PORTS = {53692};
    public static final String OAUTH_SCOPE = "user:profile user:inference user:sessions:claude_code user:mcp_servers user:file_upload";
    public static final String OAUTH_BETA = "oauth-2025-04-20";
    // The private usage route rate-limits unknown clients more aggressively.
    // Keep this isolated to quota reads; normal OAuth traffic uses Claude Meter's own UA.
    public static final String CLAUDE_CODE_USAGE_USER_AGENT = "claude-code/2.1.303";
    public static final String ORIGINATOR = "claude-meter-android";
    public static final String RESET_CREDITS_CONSUME_URL = "";
    public static final String RESET_CREDITS_URL = "";
    public static final String REVOKE_URL = "";
    public static final String TOKEN_URL = "https://platform.claude.com/v1/oauth/token";
    public static final String USAGE_URL = "https://api.anthropic.com/api/oauth/usage";
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

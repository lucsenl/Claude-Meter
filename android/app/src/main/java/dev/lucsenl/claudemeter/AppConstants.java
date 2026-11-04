package dev.lucsenl.claudemeter;

import android.os.Build;

public final class AppConstants {
    public static final String ACTION_USAGE_UPDATED =
            "dev.lucsenl.claudemeter.action.USAGE_UPDATED";
    public static final String ACTION_REFRESH_WIDGET =
            "dev.lucsenl.claudemeter.action.REFRESH_WIDGET";
    public static final String INTERNAL_PERMISSION =
            "dev.lucsenl.claudemeter.permission.INTERNAL";
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

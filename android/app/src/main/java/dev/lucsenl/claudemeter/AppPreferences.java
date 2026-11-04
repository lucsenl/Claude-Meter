package dev.lucsenl.claudemeter;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public final class AppPreferences {
    private static final String PREFS = "claude_meter_v2";
    private static final String KEY_SNAPSHOT = "subscription_snapshot";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String KEY_ONBOARDING = "onboarding_complete";
    private static final String KEY_THEME = "theme";
    private static final String KEY_MATERIAL_YOU = "material_you";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private AppPreferences() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static UsageSnapshot loadSnapshot(Context context) {
        String raw = prefs(context).getString(KEY_SNAPSHOT, "");
        if (raw == null || raw.isEmpty()) return null;
        try { return UsageSnapshot.fromJson(new JSONObject(raw)); }
        catch (Exception ignored) { return null; }
    }

    public static void saveSnapshot(Context context, UsageSnapshot snapshot) {
        SharedPreferences.Editor edit = prefs(context).edit();
        if (snapshot == null) {
            edit.remove(KEY_SNAPSHOT).apply();
            return;
        }
        try { edit.putString(KEY_SNAPSHOT, snapshot.toJson().toString()).apply(); }
        catch (Exception ignored) {}
    }

    public static String getLastError(Context context) {
        return prefs(context).getString(KEY_LAST_ERROR, "");
    }

    public static void setLastError(Context context, String value) {
        prefs(context).edit().putString(KEY_LAST_ERROR,
                value == null ? "" : value.trim()).apply();
    }

    public static boolean isOnboardingComplete(Context context) {
        return prefs(context).getBoolean(KEY_ONBOARDING, false);
    }

    public static void completeOnboarding(Context context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING, true).apply();
    }

    public static void resetOnboarding(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_ONBOARDING, false)
                .remove(KEY_SNAPSHOT)
                .remove(KEY_LAST_ERROR)
                .apply();
    }

    public static String getAppTheme(Context context) {
        String value = prefs(context).getString(KEY_THEME, THEME_SYSTEM);
        if (THEME_LIGHT.equals(value) || THEME_DARK.equals(value)) return value;
        return THEME_SYSTEM;
    }

    public static void setAppTheme(Context context, String value) {
        String normalized = THEME_LIGHT.equals(value) || THEME_DARK.equals(value)
                ? value : THEME_SYSTEM;
        prefs(context).edit().putString(KEY_THEME, normalized).apply();
    }

    public static boolean isMaterialYouEnabled(Context context) {
        return prefs(context).getBoolean(KEY_MATERIAL_YOU, false);
    }

    public static void setMaterialYouEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MATERIAL_YOU, enabled).apply();
    }
}

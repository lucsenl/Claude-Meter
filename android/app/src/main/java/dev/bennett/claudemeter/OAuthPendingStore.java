package dev.bennett.claudemeter;

import android.content.Context;
import android.content.SharedPreferences;

/** Short-lived local PKCE verifier/state; never contains credentials or cookies. */
public final class OAuthPendingStore {
    private static final String PREFS = "claude_pending_pkce_v1";
    private static final long MAX_AGE_MS = 10L * 60L * 1000L;
    private OAuthPendingStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void save(Context context, ClaudeOAuthProtocol.LoginRequest request) {
        prefs(context).edit().putString("verifier", request.verifier)
                .putString("state", request.state)
                .putLong("created_at", System.currentTimeMillis()).commit();
    }

    public static boolean isValid(Context context) {
        long age = System.currentTimeMillis() - prefs(context).getLong("created_at", 0L);
        if (age < 0 || age > MAX_AGE_MS || verifier(context).isEmpty()
                || state(context).isEmpty()) {
            clear(context);
            return false;
        }
        return true;
    }

    public static String verifier(Context context) {
        return prefs(context).getString("verifier", "");
    }

    public static String state(Context context) {
        return prefs(context).getString("state", "");
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().commit();
    }
}

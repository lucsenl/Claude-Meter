package dev.lucsenl.claudemeter;

import android.content.Context;
import org.json.JSONObject;

public final class AnthropicStateStore {
    private static final String PREFS = "anthropic_state_v1";
    private static final String KEY_SNAPSHOT = "snapshot";

    private AnthropicStateStore() {}

    public static boolean save(Context context, AnthropicSnapshot snapshot) {
        if (snapshot == null) return false;
        try {
            return context.getSharedPreferences(PREFS, 0).edit()
                    .putString(KEY_SNAPSHOT, snapshot.toJson().toString())
                    .commit();
        } catch (Exception exception) {
            return false;
        }
    }

    public static AnthropicSnapshot load(Context context) {
        String encoded = context.getSharedPreferences(PREFS, 0)
                .getString(KEY_SNAPSHOT, null);
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            return AnthropicSnapshot.fromJson(new JSONObject(encoded));
        } catch (Exception exception) {
            context.getSharedPreferences(PREFS, 0).edit().remove(KEY_SNAPSHOT).apply();
            return null;
        }
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS, 0).edit().clear().apply();
    }
}

package dev.lucsenl.claudemeter;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import dev.lucsenl.claudemeter.wear.WearSyncPaths;
import org.json.JSONObject;

public final class WearPreferences {
    private static final String PREFS = "claude_meter_wear_v2";
    private static final String KEY_SNAPSHOT = "snapshot";
    private static final String KEY_CONNECTED = "connected";
    private static final String KEY_ERROR = "error";

    private WearPreferences() {}

    public static UsageSnapshot loadSnapshot(Context context) {
        String raw = prefs(context).getString(KEY_SNAPSHOT, "");
        if (raw == null || raw.isEmpty()) return null;
        try { return UsageSnapshot.fromJson(new JSONObject(raw)); }
        catch (Exception ignored) { return null; }
    }

    public static void apply(DataItem item, Context context) {
        if (item == null || item.getUri() == null
                || !WearSyncPaths.PATH_USAGE.equals(item.getUri().getPath())) return;
        try {
            com.google.android.gms.wearable.DataMap map =
                    DataMapItem.fromDataItem(item).getDataMap();
            String payload = map.getString("payload", "");
            prefs(context).edit()
                    .putString(KEY_SNAPSHOT, payload)
                    .putBoolean(KEY_CONNECTED, map.getBoolean("connected", false))
                    .putString(KEY_ERROR, map.getString("error", ""))
                    .apply();
        } catch (Exception ignored) {}
    }

    public static boolean isConnected(Context context) {
        return prefs(context).getBoolean(KEY_CONNECTED, false);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

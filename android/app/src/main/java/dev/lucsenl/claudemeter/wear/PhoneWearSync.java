package dev.lucsenl.claudemeter.wear;

import android.content.Context;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import dev.lucsenl.claudemeter.AppPreferences;
import dev.lucsenl.claudemeter.DiagnosticLog;
import dev.lucsenl.claudemeter.SecureTokenStore;
import dev.lucsenl.claudemeter.UsageSnapshot;

public final class PhoneWearSync {
    private PhoneWearSync() {}

    public static void pushUsage(Context context, UsageSnapshot snapshot) {
        if (context == null) return;
        try {
            PutDataMapRequest map = PutDataMapRequest.create(WearSyncPaths.PATH_USAGE);
            map.getDataMap().putBoolean("connected", SecureTokenStore.isSignedIn(context));
            map.getDataMap().putString("payload",
                    snapshot == null ? "" : snapshot.toJson().toString());
            map.getDataMap().putString("error", AppPreferences.getLastError(context));
            map.getDataMap().putLong("sent_at", System.currentTimeMillis());
            Wearable.getDataClient(context.getApplicationContext())
                    .putDataItem(map.asPutDataRequest().setUrgent())
                    .addOnFailureListener(error -> DiagnosticLog.error(context, "wear",
                            "analytics_sync_failed", error));
        } catch (Exception exception) {
            DiagnosticLog.error(context, "wear", "analytics_sync_encode_failed", exception);
        }
    }

    public static void pushStatus(Context context, boolean ignored, String error) {
        pushUsage(context, AppPreferences.loadSnapshot(context));
    }

    public static void pushAll(Context context) {
        pushUsage(context, AppPreferences.loadSnapshot(context));
    }
}

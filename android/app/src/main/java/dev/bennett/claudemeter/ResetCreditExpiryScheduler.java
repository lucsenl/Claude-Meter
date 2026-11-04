package dev.bennett.claudemeter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Schedules one alarm for each configured lead time and available reset credit. */
public final class ResetCreditExpiryScheduler {
    static final String EXTRA_CREDIT_ID = "expiry_credit_id";
    static final String EXTRA_EXPIRES_AT = "expiry_expires_at";
    static final String EXTRA_LEAD_TIME = "expiry_lead_time";
    private static final long DELIVERY_GRACE_MS = 1000L;
    private static final String KEY_ALARM_URIS = "alarm_uris";
    private static final String PREFS = "claude_meter_reset_expiry_alarms_v1";
    private static final int REQUEST_CODE = 74209;
    // Preserve the old action solely to cancel alarms left by earlier builds.
    private static final String LEGACY_ALARM_ACTION =
            "dev.bennett.claudemeter.action.RESET_CREDIT_EXPIRY_ALERT";

    private ResetCreditExpiryScheduler() {
    }

    /** Retained for preference compatibility; only clears obsolete pending alarms. */
    public static void scheduleFromSnapshot(Context context, ResetCreditsSnapshot snapshot) {
        cancelAll(context);
    }

    public static void cancelAll(Context context) {
        Context app = appContext(context);
        if (app == null) return;
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        Set<String> uris = prefs(app).getStringSet(KEY_ALARM_URIS, null);
        if (manager != null && uris != null) {
            for (String uri : new HashSet<>(uris)) {
                PendingIntent pendingIntent = existingPending(app, Uri.parse(uri));
                if (pendingIntent != null) {
                    manager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
        prefs(app).edit().remove(KEY_ALARM_URIS).apply();
    }

    private static PendingIntent pending(Context context, Uri data,
            ResetCreditExpiryReminder reminder) {
        Intent intent = baseIntent(context, data)
                .putExtra(EXTRA_CREDIT_ID, reminder.creditId)
                .putExtra(EXTRA_EXPIRES_AT, reminder.expiresAtMillis)
                .putExtra(EXTRA_LEAD_TIME, reminder.leadTimeMillis);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent existingPending(Context context, Uri data) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE, baseIntent(context, data),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Intent baseIntent(Context context, Uri data) {
        return new Intent(context, ResetCreditExpiryReceiver.class)
                .setAction(LEGACY_ALARM_ACTION)
                .setData(data);
    }

    private static Uri data(String creditId, long expiresAt, long leadTime) {
        return new Uri.Builder()
                .scheme("claudemeter")
                .authority("reset-credit-expiry")
                .appendPath(creditId == null || creditId.isEmpty() ? "_" : creditId)
                .appendQueryParameter("expires", String.valueOf(expiresAt))
                .appendQueryParameter("lead", String.valueOf(leadTime))
                .build();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Context appContext(Context context) {
        if (context == null) return null;
        Context applicationContext = context.getApplicationContext();
        return applicationContext == null ? context : applicationContext;
    }
}

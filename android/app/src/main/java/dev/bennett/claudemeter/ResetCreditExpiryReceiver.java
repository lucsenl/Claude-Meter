package dev.bennett.claudemeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Revalidates a scheduled reset-credit reminder before showing it. */
public final class ResetCreditExpiryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // No Claude reset-credit events exist. Never show a legacy reminder.
        if (context != null) ResetCreditExpiryScheduler.cancelAll(context);
    }

    static boolean isStillAvailable(Context context, String creditId, long expiresAt) {
        if (expiresAt <= System.currentTimeMillis()) return false;
        ResetCreditsSnapshot snapshot = AppPreferences.loadResetCredits(context);
        if (snapshot == null) return false;
        for (RateLimitResetCredit credit : snapshot.credits) {
            if (credit == null || !credit.isAvailable()
                    || credit.expiresAtMillis != expiresAt) {
                continue;
            }
            if (creditId == null || creditId.isEmpty() || credit.id.equals(creditId)) {
                return true;
            }
        }
        return false;
    }
}

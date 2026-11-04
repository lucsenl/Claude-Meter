package dev.lucsenl.claudemeter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        RefreshScheduler.schedulePeriodic(context);
        ReleaseUpdateScheduler.ensureScheduled(context);
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            UpdateNotificationManager.dismiss(context);
            UpdatePreferences.clearNotifiedVersion(context);
            WidgetUpgradeRepair.afterPackageReplaced(context);
        } else {
            WidgetUpgradeRepair.runIfNeeded(context);
        }
    }
}

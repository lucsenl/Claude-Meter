package dev.lucsenl.claudemeter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

/** Background refresh scheduling for documented Anthropic APIs. */
public final class RefreshScheduler {
    private static final int PERIODIC_JOB_ID = 73100;
    private static final int IMMEDIATE_JOB_ID = 73101;

    private RefreshScheduler() {}

    public static boolean schedulePeriodic(Context context) {
        Context app = appContext(context);
        if (app == null) return false;
        if (!SecureTokenStore.isSignedIn(app)) {
            cancelAll(app);
            return true;
        }
        JobScheduler scheduler = scheduler(app);
        if (scheduler == null) return false;
        scheduler.cancel(PERIODIC_JOB_ID);
        int minutes = effectiveRefreshMinutes(app);
        JobInfo info = base(app, PERIODIC_JOB_ID, "periodic")
                .setPeriodic((long) minutes * 60L * 1000L)
                .setPersisted(true)
                .build();
        return scheduler.schedule(info) == JobScheduler.RESULT_SUCCESS;
    }

    public static boolean scheduleImmediate(Context context) {
        Context app = appContext(context);
        if (app == null || !SecureTokenStore.isSignedIn(app)) return false;
        JobScheduler scheduler = scheduler(app);
        if (scheduler == null) return false;
        JobInfo info = base(app, IMMEDIATE_JOB_ID, "immediate")
                .setMinimumLatency(0L)
                .setOverrideDeadline(5000L)
                .build();
        return scheduler.schedule(info) == JobScheduler.RESULT_SUCCESS;
    }

    public static int effectiveRefreshMinutes(Context context) {
        int configured = AppPreferences.getRefreshMinutes(context);
        if (AppPreferences.getAutomaticRefresh(context)) configured = 30;
        return Math.max(15, configured);
    }

    /**
     * Transitional compatibility for inherited callers. Public Anthropic Analytics has no
     * subscription-reset timestamp, so this only ensures the normal periodic job exists.
     */
    @Deprecated
    public static boolean scheduleAtNextReset(Context context, UsageSnapshot ignored) {
        return schedulePeriodic(context);
    }

    public static boolean scheduleNextShort(Context context, int ignored) {
        return schedulePeriodic(context);
    }

    public static void cancelAll(Context context) {
        Context app = appContext(context);
        if (app == null) return;
        JobScheduler scheduler = scheduler(app);
        if (scheduler != null) {
            scheduler.cancel(PERIODIC_JOB_ID);
            scheduler.cancel(IMMEDIATE_JOB_ID);
        }
    }

    private static JobInfo.Builder base(Context context, int id, String reason) {
        PersistableBundle extras = new PersistableBundle();
        extras.putString("reason", reason);
        return new JobInfo.Builder(id,
                new ComponentName(context, UsageRefreshJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(extras);
    }

    private static JobScheduler scheduler(Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private static Context appContext(Context context) {
        if (context == null) return null;
        Context app = context.getApplicationContext();
        return app == null ? context : app;
    }
}

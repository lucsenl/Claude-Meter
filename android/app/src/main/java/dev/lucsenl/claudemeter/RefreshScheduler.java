package dev.lucsenl.claudemeter;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public final class RefreshScheduler {
    private static final int JOB_PERIODIC = 7310;
    private static final int JOB_IMMEDIATE = 7311;

    private RefreshScheduler() {}

    public static void schedulePeriodic(Context context) {
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        if (!SecureTokenStore.isSignedIn(context)) {
            scheduler.cancel(JOB_PERIODIC);
            return;
        }
        JobInfo job = new JobInfo.Builder(JOB_PERIODIC,
                new ComponentName(context, UsageRefreshJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(false)
                .setPeriodic(15L * 60L * 1000L)
                .build();
        scheduler.schedule(job);
    }

    public static void scheduleImmediate(Context context) {
        if (!SecureTokenStore.isSignedIn(context)) return;
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(JOB_IMMEDIATE,
                new ComponentName(context, UsageRefreshJobService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(1000L)
                .build();
        scheduler.schedule(job);
    }
}

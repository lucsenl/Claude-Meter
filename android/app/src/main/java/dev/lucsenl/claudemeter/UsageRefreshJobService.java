package dev.lucsenl.claudemeter;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class UsageRefreshJobService extends JobService {
    private volatile Thread worker;

    @Override public boolean onStartJob(JobParameters params) {
        worker = new Thread(() -> {
            boolean retry = false;
            try {
                UsageApi.refreshAndCacheLocked(getApplicationContext());
            } catch (Exception exception) {
                retry = true;
            }
            jobFinished(params, retry);
        }, "claude-meter-refresh");
        worker.start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        Thread thread = worker;
        if (thread != null) thread.interrupt();
        return true;
    }
}

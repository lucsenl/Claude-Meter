package dev.lucsenl.claudemeter;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class UsageRefreshJobService extends JobService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> running;

    @Override
    public boolean onStartJob(JobParameters params) {
        if (!SecureTokenStore.isSignedIn(this)) return false;
        running = executor.submit(() -> {
            boolean retry = false;
            try {
                UsageApi.refreshAndCacheLocked(getApplicationContext());
                AppPreferences.recordRefreshSuccess(getApplicationContext());
            } catch (Exception exception) {
                retry = true;
                AppPreferences.recordRefreshFailure(getApplicationContext());
                AppPreferences.setLastError(getApplicationContext(),
                        UsageApi.safeMessage(exception));
            } finally {
                WidgetRenderer.updateAll(getApplicationContext());
                boolean finalRetry = retry;
                runOnUiThreadCompat(() -> jobFinished(params, finalRetry));
            }
        });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (running != null) running.cancel(true);
        return true;
    }

    @Override
    public void onDestroy() {
        if (running != null) running.cancel(true);
        executor.shutdownNow();
        super.onDestroy();
    }

    private void runOnUiThreadCompat(Runnable runnable) {
        new android.os.Handler(getMainLooper()).post(runnable);
    }
}

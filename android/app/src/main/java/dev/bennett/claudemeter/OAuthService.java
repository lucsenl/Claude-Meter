package dev.bennett.claudemeter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Compatibility entry point: login must be completed in the app with PKCE. */
public final class OAuthService extends Service {
    public static final String ACTION_START = "dev.bennett.claudemeter.oauth.START";
    public static final String ACTION_CANCEL = "dev.bennett.claudemeter.oauth.CANCEL";
    public static final String ACTION_CANCEL_SILENT = "dev.bennett.claudemeter.oauth.CANCEL_SILENT";

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_START.equals(intent.getAction())) {
            Intent onboarding = new Intent(this, OnboardingActivity.class)
                    .putExtra(OnboardingActivity.EXTRA_RECONNECT, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(onboarding);
        } else {
            OAuthPendingStore.clear(this);
            AppPreferences.setOAuthPending(this, false, "");
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}

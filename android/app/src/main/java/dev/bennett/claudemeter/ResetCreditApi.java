package dev.bennett.claudemeter;

import android.content.Context;

/**
 * Compatibility facade retained while old call sites are removed.
 * Claude has no reset-credit redemption contract. No network request is made here.
 */
public final class ResetCreditApi {
    private ResetCreditApi() {}

    public static ResetCreditsSnapshot refreshAndCache(Context context) {
        return emptySnapshot();
    }

    static ResetCreditsSnapshot refreshAndCacheLocked(Context context, AuthTokens tokens) {
        return emptySnapshot();
    }

    public static ResetConsumeResult consumeBestAvailable(Context context) {
        return new ResetConsumeResult(ResetConsumeResult.NO_CREDIT, 0,
                "Claude does not provide reset-credit redemption.");
    }

    private static ResetCreditsSnapshot emptySnapshot() {
        return ResetCreditsSnapshot.summary(0, System.currentTimeMillis());
    }
}

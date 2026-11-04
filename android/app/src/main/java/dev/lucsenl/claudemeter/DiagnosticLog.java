package dev.lucsenl.claudemeter;

import android.content.Context;
import android.util.Log;

/** Privacy-safe local log facade. Values must never contain credentials. */
public final class DiagnosticLog {
    private static final String TAG = "ClaudeMeter";
    private DiagnosticLog() {}

    public static void install(Context context) {}

    public static void info(Context context, String area, String event, Object... fields) {
        Log.i(TAG, area + "/" + event + format(fields));
    }

    public static void warn(Context context, String area, String event, Object... fields) {
        Log.w(TAG, area + "/" + event + format(fields));
    }

    public static void error(Context context, String area, String event, Throwable error,
            Object... fields) {
        Log.e(TAG, area + "/" + event + format(fields),
                error instanceof Exception ? (Exception) error : new RuntimeException(error));
    }

    private static String format(Object... fields) {
        if (fields == null || fields.length == 0) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < fields.length; i += 2) {
            String key = String.valueOf(fields[i]);
            if (key.toLowerCase().contains("key") || key.toLowerCase().contains("token")
                    || key.toLowerCase().contains("authorization")) continue;
            out.append(' ').append(key).append('=').append(String.valueOf(fields[i + 1]));
        }
        return out.toString();
    }
}

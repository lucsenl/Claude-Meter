package dev.lucsenl.claudemeter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.NumberFormat;
import java.util.Locale;

public final class WidgetRenderer {
    private WidgetRenderer() {}

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClaudeUsageWidget.class));
        for (int id : ids) update(context, manager, id);
    }

    public static void update(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_micro);
        UsageSnapshot snapshot = AppPreferences.loadSnapshot(context);

        views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE);
        views.setTextViewText(R.id.widget_title, "CLAUDE");
        views.setViewVisibility(R.id.plan_label, android.view.View.VISIBLE);
        views.setTextViewText(R.id.plan_label,
                snapshot != null && snapshot.analyticsAvailable ? "ANALYTICS" : "API");

        views.setTextViewText(R.id.primary_label, "Today");
        views.setTextViewText(R.id.secondary_label, "7d");
        views.setTextViewText(R.id.primary_percent,
                snapshot == null ? "—" : compact(snapshot.todayTokens));
        views.setTextViewText(R.id.secondary_percent,
                snapshot == null ? "—" : compact(snapshot.weekTokens));

        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(android.R.id.background,
                PendingIntent.getActivity(context, id, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent refresh = new Intent(context, WidgetRefreshReceiver.class)
                .setAction(AppConstants.ACTION_REFRESH_WIDGET)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        views.setOnClickPendingIntent(R.id.refresh_button,
                PendingIntent.getBroadcast(context, 50000 + id, refresh,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(id, views);
    }

    private static String compact(long value) {
        if (value >= 1_000_000_000L) return String.format(Locale.US, "%.1fB", value / 1e9);
        if (value >= 1_000_000L) return String.format(Locale.US, "%.1fM", value / 1e6);
        if (value >= 1_000L) return String.format(Locale.US, "%.1fK", value / 1e3);
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }
}

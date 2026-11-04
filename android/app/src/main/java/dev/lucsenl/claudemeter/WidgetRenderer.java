package dev.lucsenl.claudemeter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
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

        views.setTextViewText(R.id.widget_title, "CLAUDE");
        views.setTextViewText(R.id.plan_label,
                snapshot == null || snapshot.plan.isEmpty() ? "—" : snapshot.plan.toUpperCase(Locale.US));
        views.setTextViewText(R.id.primary_label, "5h");
        views.setTextViewText(R.id.secondary_label, "7d");
        views.setTextViewText(R.id.primary_percent,
                snapshot == null ? "—" : percent(snapshot.fiveHourUtilization));
        views.setTextViewText(R.id.secondary_percent,
                snapshot == null ? "—" : percent(snapshot.sevenDayUtilization));

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

    private static String percent(double value) {
        return value < 0d ? "—" : String.format(Locale.US, "%.0f%%", value);
    }
}

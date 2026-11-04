package dev.lucsenl.claudemeter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public final class ClaudeUsageWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        if (ids != null) {
            for (int id : ids) WidgetRenderer.update(context, manager, id);
        }
        RefreshScheduler.schedulePeriodic(context);
    }

    @Override public void onEnabled(Context context) {
        RefreshScheduler.schedulePeriodic(context);
        RefreshScheduler.scheduleImmediate(context);
    }
}

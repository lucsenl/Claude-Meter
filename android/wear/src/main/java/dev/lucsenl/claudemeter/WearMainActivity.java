package dev.lucsenl.claudemeter;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import java.util.Locale;

public final class WearMainActivity extends Activity implements DataClient.OnDataChangedListener {
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(20, 20, 19));
        setContentView(root);
        render();
    }

    @Override protected void onStart() {
        super.onStart();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override protected void onStop() {
        Wearable.getDataClient(this).removeListener(this);
        super.onStop();
    }

    @Override public void onDataChanged(DataEventBuffer events) {
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem() != null) {
                WearPreferences.apply(event.getDataItem(), this);
            }
        }
        render();
    }

    private void render() {
        root.removeAllViews();
        add("Claude Meter", 20, Color.rgb(250, 249, 245));
        UsageSnapshot snapshot = WearPreferences.loadSnapshot(this);
        if (snapshot == null) {
            add("Waiting for phone sync", 14, Color.rgb(176, 174, 165));
            return;
        }
        add("Today", 12, Color.rgb(176, 174, 165));
        add(compact(snapshot.todayTokens) + " tokens", 19, Color.WHITE);
        add("7 days", 12, Color.rgb(176, 174, 165));
        add(compact(snapshot.weekTokens) + " tokens", 19, Color.WHITE);
        if (snapshot.analyticsAvailable && !snapshot.monthCostUsd.isEmpty()) {
            add("30d cost", 12, Color.rgb(176, 174, 165));
            add("$" + snapshot.monthCostUsd, 18, Color.rgb(217, 119, 87));
        } else if (!snapshot.analyticsMessage.isEmpty()) {
            add("Analytics unavailable", 12, Color.rgb(176, 174, 165));
        }
    }

    private void add(String value, float size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(5), 0, dp(5));
        root.addView(text, new LinearLayout.LayoutParams(-1, -2));
    }

    private static String compact(long value) {
        if (value >= 1_000_000_000L) return String.format(Locale.US, "%.1fB", value / 1e9);
        if (value >= 1_000_000L) return String.format(Locale.US, "%.1fM", value / 1e6);
        if (value >= 1_000L) return String.format(Locale.US, "%.1fK", value / 1e3);
        return Long.toString(value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

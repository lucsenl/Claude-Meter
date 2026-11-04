package dev.lucsenl.claudemeter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private static final int MENU_SETTINGS = 8101;
    private static final int MENU_ABOUT = 8102;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private SwipeRefreshLayout swipeRefresh;
    private boolean dark;
    private boolean receiverRegistered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            rebuild();
        }
    };

    @Override protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        if (!AppPreferences.isOnboardingComplete(this) && !SecureTokenStore.isSignedIn(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        dark = Ui.isDark(this);
        content = Ui.installPage(this, "Claude Meter", false).content;
        swipeRefresh = findViewById(R.id.dashboard_refresh);
        swipeRefresh.setColorSchemeColors(Ui.accent(this, dark));
        swipeRefresh.setProgressBackgroundColorSchemeColor(Ui.cardColor(this, dark));
        swipeRefresh.setOnRefreshListener(this::refresh);
        rebuild();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ABOUT, 0, "About")
                .setIcon(R.drawable.ic_oui_info_outline)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, MENU_SETTINGS, 1, "Settings")
                .setIcon(R.drawable.ic_oui_settings_outline)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_SETTINGS) {
            Ui.startSecondaryActivity(this, SettingsActivity.class);
            return true;
        }
        if (item.getItemId() == MENU_ABOUT) {
            Ui.startSecondaryActivity(this, AboutActivity.class);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override @SuppressLint("UnspecifiedRegisterReceiverFlag")
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AppConstants.ACTION_USAGE_UPDATED);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(receiver, filter, AppConstants.INTERNAL_PERMISSION, null,
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, filter, AppConstants.INTERNAL_PERMISSION, null);
            }
            receiverRegistered = true;
        } catch (RuntimeException ignored) {}

        if (SecureTokenStore.isSignedIn(this)) {
            RefreshScheduler.schedulePeriodic(this);
            UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
            if (snapshot == null
                    || System.currentTimeMillis() - snapshot.fetchedAtMillis > 15L * 60L * 1000L) {
                refresh();
            }
        }
    }

    @Override protected void onStop() {
        if (receiverRegistered) {
            try { unregisterReceiver(receiver); } catch (RuntimeException ignored) {}
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) rebuild();
    }

    private void rebuild() {
        if (content == null) return;
        content.removeAllViews();

        if (!SecureTokenStore.isSignedIn(this)) {
            LinearLayout card = Ui.card(this, dark);
            TextView title = Ui.text(this, "Sign in to Claude", 22, Ui.mainText(dark));
            title.setTypeface(Ui.mediumTypeface(this));
            card.addView(title);
            TextView body = Ui.text(this,
                    "Connect the Claude account whose subscription usage you want to monitor.",
                    15, Ui.secondaryText(dark));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
            bp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 18));
            card.addView(body, bp);
            Button connect = Ui.nativePrimaryButton(this, "Sign in with Claude");
            connect.setOnClickListener(v ->
                    startActivity(new Intent(this, OnboardingActivity.class)));
            card.addView(connect, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));
            content.addView(card);
            return;
        }

        UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
        if (snapshot == null) {
            addInfoCard("Connected to Claude", "Pull to refresh subscription usage.");
            return;
        }

        String identity = !snapshot.displayName.isEmpty()
                ? snapshot.displayName
                : (!snapshot.email.isEmpty() ? snapshot.email : "Claude account");
        String subtitle = snapshot.plan;
        if (!snapshot.email.isEmpty() && !snapshot.email.equals(identity)) {
            subtitle += " · " + snapshot.email;
        }
        if (!snapshot.subscriptionStatus.isEmpty()) {
            subtitle += " · " + snapshot.subscriptionStatus;
        }
        addInfoCard(identity, subtitle);

        addUsageCard("5-hour session", snapshot.fiveHourUtilization,
                snapshot.fiveHourResetsAt);
        addUsageCard("7-day allowance", snapshot.sevenDayUtilization,
                snapshot.sevenDayResetsAt);

        for (UsageSnapshot.ScopedLimit limit : snapshot.scopedLimits) {
            addUsageCard(limit.label, limit.utilization, limit.resetsAt);
        }

        if (snapshot.extraUsageEnabled) {
            addInfoCard("Extra usage", "Enabled on this Claude account.");
        }

        String error = AppPreferences.getLastError(this);
        if (!error.isEmpty()) addInfoCard("Last refresh issue", error);
    }

    private void addUsageCard(String title, double utilization, String resetsAt) {
        LinearLayout card = Ui.card(this, dark);
        TextView label = Ui.text(this, title, 14, Ui.secondaryText(dark));
        card.addView(label);

        String value = utilization < 0d ? "—"
                : String.format(Locale.US, "%.0f%% used", utilization);
        TextView number = Ui.text(this, value, 30, Ui.mainText(dark));
        number.setTypeface(Ui.mediumTypeface(this));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, Ui.dp(this, 7), 0, 0);
        card.addView(number, np);

        String detail;
        if (utilization < 0d) {
            detail = "Usage unavailable";
        } else {
            double remaining = Math.max(0d, 100d - utilization);
            detail = String.format(Locale.US, "%.0f%% remaining", remaining);
            String reset = resetLabel(resetsAt);
            if (!reset.isEmpty()) detail += " · " + reset;
        }
        TextView sub = Ui.text(this, detail, 13, Ui.secondaryText(dark));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, Ui.dp(this, 6), 0, 0);
        card.addView(sub, sp);
        content.addView(card);
        Ui.addSpacer(content, 14);
    }

    private void addInfoCard(String title, String detail) {
        LinearLayout card = Ui.card(this, dark);
        TextView label = Ui.text(this, title, 18, Ui.mainText(dark));
        label.setTypeface(Ui.mediumTypeface(this));
        card.addView(label);
        TextView sub = Ui.text(this, detail, 14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(sub, sp);
        content.addView(card);
        Ui.addSpacer(content, 14);
    }

    private void refresh() {
        if (!SecureTokenStore.isSignedIn(this)) {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            String error = "";
            try { UsageApi.refreshAndCacheLocked(getApplicationContext()); }
            catch (Exception exception) { error = UsageApi.safeMessage(exception); }
            String finalError = error;
            runOnUiThread(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                rebuild();
                if (!finalError.isEmpty()) {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    static String resetLabel(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        try {
            OffsetDateTime reset = OffsetDateTime.parse(value);
            Duration duration = Duration.between(
                    OffsetDateTime.now(ZoneId.systemDefault()).toInstant(), reset.toInstant());
            if (duration.isNegative() || duration.isZero()) return "resetting now";
            long minutes = duration.toMinutes();
            long days = minutes / (24L * 60L);
            long hours = (minutes % (24L * 60L)) / 60L;
            long mins = minutes % 60L;
            if (days > 0) return "resets in " + days + "d " + hours + "h";
            if (hours > 0) return "resets in " + hours + "h " + mins + "m";
            return "resets in " + Math.max(1L, mins) + "m";
        } catch (Exception ignored) {
            try {
                return "resets "
                        + OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE HH:mm"));
            } catch (Exception ignoredAgain) {
                return "";
            }
        }
    }
}

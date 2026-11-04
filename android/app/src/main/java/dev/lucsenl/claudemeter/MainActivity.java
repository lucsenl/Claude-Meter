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
import java.text.NumberFormat;
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
        if (!AppPreferences.isOnboardingComplete(this)) {
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
            UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
            if (snapshot == null || System.currentTimeMillis() - snapshot.fetchedAtMillis > 900000L) {
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
            TextView title = Ui.text(this, "Connect the Anthropic API", 22, Ui.mainText(dark));
            title.setTypeface(Ui.mediumTypeface(this));
            card.addView(title);
            TextView body = Ui.text(this,
                    "Claude Meter uses a user-supplied API key. It does not sign in to Claude.ai "
                            + "or imitate Claude Code. The key stays encrypted on this device.",
                    15, Ui.secondaryText(dark));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
            bp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 18));
            card.addView(body, bp);
            Button connect = Ui.nativePrimaryButton(this, "Add API key");
            connect.setOnClickListener(v -> startActivity(new Intent(this, OnboardingActivity.class)
                    .putExtra("edit_key", true)));
            card.addView(connect, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));
            content.addView(card);
            return;
        }

        UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
        if (snapshot == null) {
            addInfoCard("Waiting for API data",
                    "Refresh to load models and, when your credential has Analytics access, "
                            + "organization usage and cost.");
            return;
        }

        String org = snapshot.organizationName.isEmpty()
                ? (snapshot.organizationId.isEmpty() ? "Anthropic API" : snapshot.organizationId)
                : snapshot.organizationName;
        addInfoCard(org,
                snapshot.analyticsAvailable
                        ? "Organization Analytics connected"
                        : "API key connected · Analytics unavailable for this credential");

        if (snapshot.analyticsAvailable) {
            addMetric("Today", formatTokens(snapshot.todayTokens),
                    formatNumber(snapshot.todayRequests) + " requests");
            addMetric("Last 7 days", formatTokens(snapshot.weekTokens),
                    formatNumber(snapshot.weekRequests) + " requests");
            addMetric("Last 30 days", snapshot.monthCostUsd.isEmpty()
                            ? "—" : "$" + snapshot.monthCostUsd,
                    "API cost reported by Anthropic");
        } else {
            addInfoCard("Usage and cost unavailable", snapshot.analyticsMessage);
        }

        String models = snapshot.activeModels.isEmpty()
                ? "No models returned"
                : String.join(" · ", snapshot.activeModels.subList(
                        0, Math.min(3, snapshot.activeModels.size())));
        addMetric("Available models", Integer.toString(snapshot.activeModels.size()), models);

        addInfoCard("About subscription limits",
                "Claude Pro/Max session and weekly allowance percentages are not exposed through "
                        + "the public third-party API. Claude Meter does not fabricate them.");

        String error = AppPreferences.getLastError(this);
        if (!error.isEmpty()) addInfoCard("Last refresh issue", error);
    }

    private void addMetric(String title, String value, String detail) {
        LinearLayout card = Ui.card(this, dark);
        TextView label = Ui.text(this, title, 14, Ui.secondaryText(dark));
        card.addView(label);
        TextView number = Ui.text(this, value, 30, Ui.mainText(dark));
        number.setTypeface(Ui.mediumTypeface(this));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, Ui.dp(this, 7), 0, 0);
        card.addView(number, np);
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
            try {
                UsageApi.refreshAndCacheLocked(getApplicationContext());
            } catch (Exception exception) {
                error = UsageApi.safeMessage(exception);
            }
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

    static String formatTokens(long value) {
        if (value >= 1_000_000_000L) return String.format(Locale.US, "%.2fB tokens", value / 1e9);
        if (value >= 1_000_000L) return String.format(Locale.US, "%.2fM tokens", value / 1e6);
        if (value >= 1_000L) return String.format(Locale.US, "%.1fK tokens", value / 1e3);
        return formatNumber(value) + " tokens";
    }

    static String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }
}

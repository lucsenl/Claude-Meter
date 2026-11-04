package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private boolean dark;

    @Override protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        dark = Ui.isDark(this);
        content = Ui.installPage(this, "Settings", true).content;
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render() {
        content.removeAllViews();

        LinearLayout account = Ui.card(this, dark);
        TextView title = Ui.text(this, "Claude account", 20, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        account.addView(title);

        UsageSnapshot snapshot = AppPreferences.loadSnapshot(this);
        boolean connected = SecureTokenStore.isSignedIn(this);
        String detail = connected ? "Signed in" : "Not signed in";
        if (snapshot != null) {
            if (!snapshot.plan.isEmpty()) detail = snapshot.plan;
            if (!snapshot.email.isEmpty()) detail += " · " + snapshot.email;
        }
        TextView state = Ui.text(this, detail, 14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, Ui.dp(this, 7), 0, Ui.dp(this, 14));
        account.addView(state, sp);

        Button signIn = Ui.nativePrimaryButton(this,
                connected ? "Sign in with another account" : "Sign in with Claude");
        signIn.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));
        account.addView(signIn, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));

        if (connected) {
            Button refresh = Ui.button(this, "Refresh usage", false, dark);
            refresh.setOnClickListener(v -> refreshNow(refresh));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
            rp.setMargins(0, Ui.dp(this, 8), 0, 0);
            account.addView(refresh, rp);

            Button disconnect = Ui.button(this, "Sign out", false, dark);
            disconnect.setOnClickListener(v -> confirmDisconnect());
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
            dp.setMargins(0, Ui.dp(this, 8), 0, 0);
            account.addView(disconnect, dp);
        }
        content.addView(account);

        Ui.addSpacer(content, 16);
        LinearLayout appearance = Ui.card(this, dark);
        TextView appearanceTitle = Ui.text(this, "Appearance", 20, Ui.mainText(dark));
        appearanceTitle.setTypeface(Ui.mediumTypeface(this));
        appearance.addView(appearanceTitle);
        addThemeButton(appearance, "System theme", AppPreferences.THEME_SYSTEM);
        addThemeButton(appearance, "Light", AppPreferences.THEME_LIGHT);
        addThemeButton(appearance, "Dark", AppPreferences.THEME_DARK);
        content.addView(appearance);

        Ui.addSpacer(content, 16);
        LinearLayout app = Ui.card(this, dark);
        TextView appTitle = Ui.text(this, "Claude Meter", 20, Ui.mainText(dark));
        appTitle.setTypeface(Ui.mediumTypeface(this));
        app.addView(appTitle);
        addAction(app, "About", () -> Ui.startSecondaryActivity(this, AboutActivity.class));
        addAction(app, "Project on GitHub", () -> open(AppConstants.PROJECT_URL));
        content.addView(app);
    }

    private void addThemeButton(LinearLayout parent, String label, String value) {
        Button button = Ui.button(this, label, false, dark);
        button.setOnClickListener(v -> {
            AppPreferences.setAppTheme(this, value);
            recreate();
        });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        p.setMargins(0, Ui.dp(this, 8), 0, 0);
        parent.addView(button, p);
    }

    private void addAction(LinearLayout parent, String label, Runnable action) {
        Button button = Ui.button(this, label, false, dark);
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        p.setMargins(0, Ui.dp(this, 8), 0, 0);
        parent.addView(button, p);
    }

    private void refreshNow(Button button) {
        button.setEnabled(false);
        executor.execute(() -> {
            String error = "";
            try { UsageApi.refreshAndCacheLocked(getApplicationContext()); }
            catch (Exception exception) { error = UsageApi.safeMessage(exception); }
            String finalError = error;
            runOnUiThread(() -> {
                button.setEnabled(true);
                Toast.makeText(this,
                        finalError.isEmpty() ? "Usage refreshed." : finalError,
                        Toast.LENGTH_LONG).show();
                render();
            });
        });
    }

    private void confirmDisconnect() {
        new AlertDialog.Builder(this)
                .setTitle("Sign out of Claude Meter")
                .setMessage("The encrypted Claude OAuth tokens and cached usage will be removed from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Sign out", (dialog, which) -> {
                    SecureTokenStore.clear(this);
                    AppPreferences.resetOnboarding(this);
                    WidgetRenderer.updateAll(this);
                    render();
                })
                .show();
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException exception) {
            Toast.makeText(this, "No browser is available.", Toast.LENGTH_LONG).show();
        }
    }
}

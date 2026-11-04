package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
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

        LinearLayout api = Ui.card(this, dark);
        TextView apiTitle = Ui.text(this, "Anthropic API", 20, Ui.mainText(dark));
        apiTitle.setTypeface(Ui.mediumTypeface(this));
        api.addView(apiTitle);
        boolean connected = SecureTokenStore.isSignedIn(this);
        TextView state = Ui.text(this,
                connected ? "API key stored securely on this device"
                        : "No API key connected",
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, Ui.dp(this, 7), 0, Ui.dp(this, 14));
        api.addView(state, sp);

        Button key = Ui.nativePrimaryButton(this, connected ? "Replace API key" : "Add API key");
        key.setOnClickListener(v -> showKeyDialog());
        api.addView(key, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));

        if (connected) {
            Button refresh = Ui.button(this, "Refresh API data", false, dark);
            refresh.setOnClickListener(v -> refreshNow(refresh));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
            rp.setMargins(0, Ui.dp(this, 8), 0, 0);
            api.addView(refresh, rp);

            Button disconnect = Ui.button(this, "Disconnect API key", false, dark);
            disconnect.setOnClickListener(v -> confirmDisconnect());
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
            dp.setMargins(0, Ui.dp(this, 8), 0, 0);
            api.addView(disconnect, dp);
        }

        Button console = Ui.button(this, "Open Claude Console", false, dark);
        console.setOnClickListener(v -> open(AppConstants.CONSOLE_KEYS_URL));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        cp.setMargins(0, Ui.dp(this, 8), 0, 0);
        api.addView(console, cp);
        content.addView(api);

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
        addAction(app, "Check for updates", () -> startActivity(
                new Intent(this, UpdateActivity.class).putExtra(UpdateActivity.EXTRA_FORCE_CHECK, true)));
        addAction(app, "About", () -> Ui.startSecondaryActivity(this, AboutActivity.class));
        addAction(app, "Project on GitHub", () -> open(AppConstants.PROJECT_URL));
        addAction(app, "Anthropic API documentation", () -> open(AppConstants.API_DOCS_URL));
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

    private void showKeyDialog() {
        EditText input = new EditText(this);
        input.setHint("sk-ant-…");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = Ui.dp(this, 24);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(pad, 0, pad, 0);
        wrapper.addView(input, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Anthropic API key")
                .setMessage("The key is validated against Anthropic and encrypted with Android Keystore.")
                .setView(wrapper)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Verify", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    if (value.isEmpty()) {
                        input.setError("Enter an API key");
                        return;
                    }
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    executor.execute(() -> {
                        String error = "";
                        try {
                            UsageApi.validateAndStoreApiKey(getApplicationContext(), value);
                            try { UsageApi.refreshAndCacheLocked(getApplicationContext()); }
                            catch (Exception ignored2) {}
                        } catch (Exception exception) {
                            error = UsageApi.safeMessage(exception);
                        }
                        String finalError = error;
                        runOnUiThread(() -> {
                            if (!finalError.isEmpty()) {
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                            } else {
                                dialog.dismiss();
                                render();
                            }
                        });
                    });
                }));
        dialog.show();
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
                        finalError.isEmpty() ? "API data refreshed." : finalError,
                        Toast.LENGTH_LONG).show();
                render();
            });
        });
    }

    private void confirmDisconnect() {
        new AlertDialog.Builder(this)
                .setTitle("Disconnect Anthropic API")
                .setMessage("The encrypted API key and cached API metrics will be removed from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Disconnect", (dialog, which) -> {
                    SecureTokenStore.clear(this);
                    AppPreferences.saveSnapshot(this, null);
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

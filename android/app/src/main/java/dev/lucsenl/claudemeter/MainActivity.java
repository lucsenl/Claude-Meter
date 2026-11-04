package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Claude Meter dashboard backed only by documented Anthropic APIs.
 */
public final class MainActivity extends AppCompatActivity {
    private LinearLayout content;
    private boolean dark;
    private boolean refreshing;

    @Override
    protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);

        if (!AppPreferences.isOnboardingComplete(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        dark = Ui.isDark(this);
        Ui.Page page = Ui.installPage(this, "Claude Meter", false);
        content = page.content;
        View refreshContainer = findViewById(R.id.dashboard_refresh);
        if (refreshContainer != null) refreshContainer.setEnabled(false);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void render() {
        content.removeAllViews();
        AnthropicCredential credential = SecureCredentialStore.load(this);
        AnthropicSnapshot snapshot = AnthropicStateStore.load(this);

        buildConnectionCard(credential, snapshot);
        Ui.addSpacer(content, 16);

        if (credential == null) {
            buildNoCredentialState();
        } else {
            buildDataCards(snapshot);
        }

        Ui.addSpacer(content, 18);
        buildSubscriptionLimitsNotice();
        Ui.addSpacer(content, 18);
        buildNavigation();
        Ui.addSpacer(content, 24);
    }

    private void buildConnectionCard(AnthropicCredential credential, AnthropicSnapshot snapshot) {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Anthropic access", 20, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);

        String status = credential == null
                ? "No credential configured"
                : credential.displayType()
                        + (snapshot == null ? " · not refreshed yet" : " · verified");
        TextView summary = Ui.text(this, status, 14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(summary, summaryParams);

        Button action = Ui.nativePrimaryButton(this,
                credential == null ? "Configure Anthropic access" : "Refresh");
        action.setEnabled(!refreshing);
        if (refreshing) action.setText("Refreshing…");
        action.setOnClickListener(view -> {
            if (credential == null) {
                openCredentialSettings();
            } else {
                refresh(action);
            }
        });
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 60));
        actionParams.setMargins(0, Ui.dp(this, 16), 0, 0);
        card.addView(action, actionParams);

        if (credential != null) {
            Button change = Ui.button(this, "Change credential", false, dark);
            change.setOnClickListener(view -> openCredentialSettings());
            LinearLayout.LayoutParams changeParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
            changeParams.setMargins(0, Ui.dp(this, 8), 0, 0);
            card.addView(change, changeParams);
        }

        content.addView(card);
    }

    private void buildNoCredentialState() {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "What Claude Meter can read", 18, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        addBody(card,
                "A Claude API key can verify access and list the models available to that key. "
                        + "An organization Admin/Analytics credential can additionally read the "
                        + "documented Usage and Cost Analytics API.");
        addBody(card,
                "Credentials are encrypted with Android Keystore and sent only to Anthropic.");
        content.addView(card);
    }

    private void buildDataCards(AnthropicSnapshot snapshot) {
        if (snapshot == null) {
            LinearLayout empty = Ui.card(this, dark);
            TextView title = Ui.text(this, "No Anthropic data cached", 18, Ui.mainText(dark));
            title.setTypeface(Ui.mediumTypeface(this));
            empty.addView(title);
            addBody(empty, "Tap Refresh to verify the credential and load documented Anthropic data.");
            content.addView(empty);
            return;
        }

        buildModelsCard(snapshot);
        Ui.addSpacer(content, 16);
        buildAnalyticsCard(snapshot);
    }

    private void buildModelsCard(AnthropicSnapshot snapshot) {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Available Claude API models", 18, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);

        if (snapshot.models.isEmpty()) {
            addBody(card,
                    "No model list is available for this credential type. "
                            + "Organization-only credentials may not have inference access.");
        } else {
            addBody(card, snapshot.models.size() + " model"
                    + (snapshot.models.size() == 1 ? "" : "s") + " reported by /v1/models.");
            int shown = Math.min(6, snapshot.models.size());
            for (int i = 0; i < shown; i++) {
                AnthropicModel model = snapshot.models.get(i);
                TextView row = Ui.text(this,
                        "• " + model.label()
                                + (model.lifecycle.isEmpty() ? "" : " · " + model.lifecycle),
                        14, Ui.mainText(dark));
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.setMargins(0, Ui.dp(this, 7), 0, 0);
                card.addView(row, rowParams);
            }
            if (snapshot.models.size() > shown) {
                addBody(card, "+" + (snapshot.models.size() - shown) + " more.");
            }
        }
        content.addView(card);
    }

    private void buildAnalyticsCard(AnthropicSnapshot snapshot) {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Organization usage · last 30 days", 18,
                Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);

        if (!snapshot.analyticsAvailable) {
            addBody(card, snapshot.analyticsMessage.isEmpty()
                    ? "Organization analytics are not available for this credential."
                    : snapshot.analyticsMessage);
            addBody(card,
                    "Claude Meter does not interpret a normal API key as a subscription-usage credential.");
            content.addView(card);
            return;
        }

        NumberFormat number = NumberFormat.getIntegerInstance(Locale.getDefault());
        addMetric(card, "Total tokens", number.format(snapshot.totalTokens));
        addMetric(card, "Input tokens", number.format(snapshot.inputTokens));
        addMetric(card, "Output tokens", number.format(snapshot.outputTokens));
        addMetric(card, "Requests", number.format(snapshot.requests));
        addMetric(card, "Cost", "$" + (snapshot.costUsd.isEmpty() ? "0.00" : snapshot.costUsd) + " USD");
        addBody(card, "Source: Anthropic Usage and Cost Analytics API.");
        content.addView(card);
    }

    private void buildSubscriptionLimitsNotice() {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Claude subscription limits", 18, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);
        addBody(card,
                "Claude Pro, Max and Ultra session/weekly limits are not exposed to independent "
                        + "third-party Android apps through a documented public usage API. "
                        + "Claude Meter therefore does not fabricate or scrape those values.");
        Button docs = Ui.button(this, "Read Anthropic documentation", false, dark);
        docs.setOnClickListener(view -> openUrl(
                "https://support.claude.com/en/articles/11145838-use-claude-code-with-your-pro-max-or-ultra-plan"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        params.setMargins(0, Ui.dp(this, 12), 0, 0);
        card.addView(docs, params);
        content.addView(card);
    }

    private void buildNavigation() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button settings = Ui.button(this, "Settings", false, dark);
        settings.setOnClickListener(view -> Ui.startSecondaryActivity(this, SettingsActivity.class));
        row.addView(settings, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f));

        Button about = Ui.button(this, "About", false, dark);
        about.setOnClickListener(view -> Ui.startSecondaryActivity(this, AboutActivity.class));
        LinearLayout.LayoutParams aboutParams = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
        aboutParams.setMargins(Ui.dp(this, 8), 0, 0, 0);
        row.addView(about, aboutParams);
        content.addView(row);
    }

    private void refresh(Button button) {
        if (refreshing) return;
        refreshing = true;
        button.setEnabled(false);
        button.setText("Refreshing…");
        new Thread(() -> {
            try {
                AnthropicApi.refreshAndCache(getApplicationContext());
                runOnUiThread(() -> {
                    refreshing = false;
                    Toast.makeText(this, "Anthropic data refreshed.", Toast.LENGTH_SHORT).show();
                    render();
                });
            } catch (Exception exception) {
                String message = exception.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Anthropic refresh failed.";
                }
                String finalMessage = message;
                runOnUiThread(() -> {
                    refreshing = false;
                    Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show();
                    render();
                });
            }
        }, "anthropic-refresh").start();
    }

    private void openCredentialSettings() {
        startActivity(new Intent(this, CredentialActivity.class));
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void addMetric(LinearLayout card, String label, String value) {
        TextView row = Ui.text(this, label + "\n" + value, 15, Ui.mainText(dark));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, Ui.dp(this, 10), 0, 0);
        card.addView(row, params);
    }

    private void addBody(LinearLayout card, String text) {
        TextView body = Ui.text(this, text, 14, Ui.secondaryText(dark));
        body.setLineSpacing(0, 1.14f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, Ui.dp(this, 9), 0, 0);
        card.addView(body, params);
    }
}

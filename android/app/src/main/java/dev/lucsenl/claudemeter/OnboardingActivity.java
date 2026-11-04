package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import dev.oneuiproject.oneui.widget.RoundedLinearLayout;

/** First-run explanation of Claude Meter's supported Anthropic data sources. */
public final class OnboardingActivity extends AppCompatActivity {
    private LinearLayout content;
    private boolean dark;

    @Override
    protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        if (AppPreferences.isOnboardingComplete(this)) {
            openMain();
            return;
        }
        dark = Ui.isDark(this);
        Ui.Page page = Ui.installPage(this, "Claude Meter", false);
        content = page.content;
        findViewById(R.id.dashboard_refresh).setEnabled(false);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void render() {
        content.removeAllViews();

        RoundedLinearLayout hero = Ui.seslCard(this, dark);
        TextView title = Ui.text(this, "Claude Meter", 34, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        hero.addView(title);
        addBody(hero,
                "An independent Android client for documented Anthropic API data, "
                        + "built on the native One UI foundation of the original project.");
        content.addView(hero);

        Ui.addSpacer(content, 16);
        RoundedLinearLayout sources = Ui.seslCard(this, dark);
        TextView sourcesTitle = Ui.text(this, "What it can read", 20, Ui.mainText(dark));
        sourcesTitle.setTypeface(Ui.mediumTypeface(this));
        sources.addView(sourcesTitle);
        addBody(sources,
                "• Claude API model metadata through /v1/models.\n"
                        + "• Organization token usage, request counts and cost through the "
                        + "Usage and Cost Analytics API when you provide an eligible "
                        + "Admin/Analytics credential.\n"
                        + "• Only data returned by documented Anthropic APIs.");
        content.addView(sources);

        Ui.addSpacer(content, 16);
        RoundedLinearLayout limits = Ui.seslCard(this, dark);
        TextView limitsTitle = Ui.text(this, "What it deliberately does not read", 20,
                Ui.mainText(dark));
        limitsTitle.setTypeface(Ui.mediumTypeface(this));
        limits.addView(limitsTitle);
        addBody(limits,
                "Claude Pro, Max and Ultra session/weekly allowances are not exposed to "
                        + "independent third-party apps through a documented public usage API. "
                        + "Claude Meter does not copy Claude Code OAuth credentials, scrape "
                        + "claude.ai, capture cookies, or call private usage routes.");
        content.addView(limits);

        Ui.addSpacer(content, 16);
        RoundedLinearLayout security = Ui.seslCard(this, dark);
        TextView securityTitle = Ui.text(this, "Credentials stay on this device", 20,
                Ui.mainText(dark));
        securityTitle.setTypeface(Ui.mediumTypeface(this));
        security.addView(securityTitle);
        addBody(security,
                "Anthropic credentials are encrypted with Android Keystore. "
                        + "They are sent only to api.anthropic.com for the requests you enable.");
        content.addView(security);

        Ui.addSpacer(content, 20);
        boolean configured = SecureCredentialStore.isConfigured(this);
        Button configure = Ui.nativePrimaryButton(this,
                configured ? "Review Anthropic access" : "Configure Anthropic access");
        configure.setOnClickListener(view ->
                startActivity(new Intent(this, CredentialActivity.class)));
        content.addView(configure, new LinearLayout.LayoutParams(-1, Ui.dp(this, 60)));

        Button continueButton = Ui.button(this,
                configured ? "Open Claude Meter" : "Continue without a credential",
                false, dark);
        continueButton.setOnClickListener(view -> complete());
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        continueParams.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 16));
        content.addView(continueButton, continueParams);
    }

    private void complete() {
        AppPreferences.completeOnboarding(this);
        openMain();
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private void addBody(LinearLayout card, String text) {
        TextView body = Ui.text(this, text, 15, Ui.secondaryText(dark));
        body.setLineSpacing(0, 1.16f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, Ui.dp(this, 12), 0, 0);
        card.addView(body, params);
    }
}

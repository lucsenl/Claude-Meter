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
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** First-run setup for a user-supplied Anthropic API key. */
public final class OnboardingActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private boolean dark;
    private EditText keyInput;
    private Button verify;

    @Override protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        dark = Ui.isDark(this);
        content = Ui.installPage(this, "Claude Meter setup", true).content;
        render();
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
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Connect Anthropic", 26, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);

        TextView body = Ui.text(this,
                "Enter an Anthropic API key. Claude Meter validates it against the public Models "
                        + "API and stores it encrypted with Android Keystore. It never asks for "
                        + "your Claude.ai password, cookies, or Claude Code session tokens.",
                15, Ui.secondaryText(dark));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 18));
        card.addView(body, bp);

        keyInput = new EditText(this);
        keyInput.setHint("sk-ant-…");
        keyInput.setSingleLine(true);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(keyInput, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));

        verify = Ui.nativePrimaryButton(this, "Verify and save");
        verify.setOnClickListener(v -> verifyKey());
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 60));
        vp.setMargins(0, Ui.dp(this, 14), 0, 0);
        card.addView(verify, vp);

        Button console = Ui.button(this, "Open Claude Console API keys", false, dark);
        console.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(AppConstants.CONSOLE_KEYS_URL))));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        cp.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(console, cp);
        content.addView(card);

        Ui.addSpacer(content, 16);
        LinearLayout note = Ui.card(this, dark);
        note.addView(Ui.text(this, "What Claude Meter can show", 18, Ui.mainText(dark)));
        TextView detail = Ui.text(this,
                "With an Admin/Analytics-capable organization credential: token usage, request "
                        + "counts and cost. With a regular API key: available models and connection "
                        + "status. Claude Pro/Max subscription quota percentages are not available "
                        + "through the public third-party API.",
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.setMargins(0, Ui.dp(this, 8), 0, 0);
        note.addView(detail, dp);
        content.addView(note);

        if (AppPreferences.isOnboardingComplete(this)) {
            Button cancel = Ui.button(this, "Cancel", false, dark);
            cancel.setOnClickListener(v -> finish());
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
            p.setMargins(0, Ui.dp(this, 12), 0, 0);
            content.addView(cancel, p);
        }
    }

    private void verifyKey() {
        String key = keyInput.getText() == null ? "" : keyInput.getText().toString().trim();
        if (key.isEmpty()) {
            keyInput.setError("Enter an Anthropic API key");
            return;
        }
        verify.setEnabled(false);
        verify.setText("Verifying…");
        executor.execute(() -> {
            String error = "";
            try {
                UsageApi.validateAndStoreApiKey(getApplicationContext(), key);
                AppPreferences.completeOnboarding(getApplicationContext());
                try { UsageApi.refreshAndCacheLocked(getApplicationContext()); }
                catch (Exception ignored) {}
            } catch (Exception exception) {
                error = UsageApi.safeMessage(exception);
            }
            String finalError = error;
            runOnUiThread(() -> {
                verify.setEnabled(true);
                verify.setText("Verify and save");
                if (!finalError.isEmpty()) {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                    return;
                }
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            });
        });
    }
}

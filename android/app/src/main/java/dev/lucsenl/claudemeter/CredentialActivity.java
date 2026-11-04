package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import dev.oneuiproject.oneui.widget.RoundedLinearLayout;

/** Configures a supported Anthropic API or organization credential. */
public final class CredentialActivity extends AppCompatActivity {
    public static final String EXTRA_FINISH_ON_SAVE = "finish_on_save";

    private boolean dark;
    private EditText secret;
    private EditText workspace;
    private RadioGroup kind;
    private Button save;

    @Override
    protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        dark = Ui.isDark(this);
        Ui.Page page = Ui.installPage(this, "Anthropic access", true);
        build(page.content);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void build(LinearLayout content) {
        RoundedLinearLayout intro = Ui.seslCard(this, dark);
        TextView title = Ui.text(this, "Connect with a supported Anthropic credential",
                20, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        intro.addView(title);
        TextView explanation = Ui.text(this,
                "Claude Meter is an independent client. It does not reuse Claude Code tokens, "
                        + "browser cookies, or private claude.ai endpoints. A normal Claude API key "
                        + "can list the models available to it. Organization Admin/Analytics "
                        + "credentials can additionally read documented usage and cost analytics.",
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams explanationParams = new LinearLayout.LayoutParams(-1, -2);
        explanationParams.setMargins(0, Ui.dp(this, 10), 0, 0);
        intro.addView(explanation, explanationParams);
        content.addView(intro);

        content.addView(Ui.separator(this, "Credential type"));
        RoundedLinearLayout typeCard = Ui.seslCard(this, dark);
        kind = new RadioGroup(this);
        kind.setOrientation(RadioGroup.VERTICAL);

        RadioButton apiKey = radio("Claude API key",
                "For Claude Platform API access. Subscription Pro/Max usage is not available through this key.");
        apiKey.setId(View.generateViewId());
        RadioButton orgKey = radio("Organization Admin / Analytics key",
                "For documented organization usage and cost analytics. Requires organization-level access.");
        orgKey.setId(View.generateViewId());
        kind.addView(apiKey);
        kind.addView(orgKey);
        kind.check(apiKey.getId());
        typeCard.addView(kind);
        content.addView(typeCard);

        content.addView(Ui.separator(this, "Credential"));
        RoundedLinearLayout credentialCard = Ui.seslCard(this, dark);
        secret = new EditText(this);
        secret.setHint("Anthropic key");
        secret.setSingleLine(true);
        secret.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        credentialCard.addView(secret, new LinearLayout.LayoutParams(-1, -2));

        workspace = new EditText(this);
        workspace.setHint("Workspace ID (optional for workspace-scoped keys)");
        workspace.setSingleLine(true);
        workspace.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout.LayoutParams workspaceParams = new LinearLayout.LayoutParams(-1, -2);
        workspaceParams.setMargins(0, Ui.dp(this, 8), 0, 0);
        credentialCard.addView(workspace, workspaceParams);
        content.addView(credentialCard);

        kind.setOnCheckedChangeListener((group, checkedId) ->
                workspace.setVisibility(checkedId == apiKey.getId() ? View.VISIBLE : View.GONE));

        save = Ui.nativePrimaryButton(this, "Verify and save");
        save.setOnClickListener(view -> verifyAndSave(apiKey.getId()));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 60));
        saveParams.setMargins(0, Ui.dp(this, 20), 0, 0);
        content.addView(save, saveParams);

        Button console = Ui.button(this, "Open Claude Console", false, dark);
        console.setOnClickListener(view -> startActivity(new Intent(
                Intent.ACTION_VIEW, Uri.parse("https://platform.claude.com/settings/keys"))));
        LinearLayout.LayoutParams consoleParams = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        consoleParams.setMargins(0, Ui.dp(this, 10), 0, Ui.dp(this, 12));
        content.addView(console, consoleParams);

        AnthropicCredential existing = SecureCredentialStore.load(this);
        if (existing != null) {
            workspace.setText(existing.workspaceId);
            if (existing.kind == AnthropicCredential.Kind.ORGANIZATION_KEY) {
                kind.check(orgKey.getId());
                workspace.setVisibility(View.GONE);
            }
        }
    }

    private RadioButton radio(String title, String summary) {
        RadioButton button = new RadioButton(this);
        button.setText(title + "\n" + summary);
        button.setTextColor(Ui.mainText(dark));
        button.setTextSize(14);
        button.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        button.setTypeface(Typeface.DEFAULT);
        return button;
    }

    private void verifyAndSave(int apiKeyId) {
        String value = secret.getText().toString().trim();
        if (value.isEmpty()) {
            Toast.makeText(this, "Enter an Anthropic credential.", Toast.LENGTH_LONG).show();
            return;
        }
        AnthropicCredential.Kind selected =
                kind.getCheckedRadioButtonId() == apiKeyId
                        ? AnthropicCredential.Kind.API_KEY
                        : AnthropicCredential.Kind.ORGANIZATION_KEY;
        AnthropicCredential candidate = new AnthropicCredential(
                value,
                selected == AnthropicCredential.Kind.API_KEY
                        ? workspace.getText().toString().trim() : "",
                selected);
        save.setEnabled(false);
        save.setText("Verifying…");

        new Thread(() -> {
            try {
                AnthropicApi.validate(getApplicationContext(), candidate);
                SecureCredentialStore.save(getApplicationContext(), candidate);
                AnthropicSnapshot snapshot = AnthropicApi.refreshAndCache(getApplicationContext());
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            snapshot.analyticsAvailable
                                    ? "Credential saved. Organization analytics are available."
                                    : "Credential saved. Claude API access verified.",
                            Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception exception) {
                String message = exception.getMessage();
                if (message == null || message.trim().isEmpty()) {
                    message = "Anthropic credential verification failed.";
                }
                String finalMessage = message;
                runOnUiThread(() -> {
                    save.setEnabled(true);
                    save.setText("Verify and save");
                    Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show();
                });
            }
        }, "anthropic-credential-check").start();
    }
}

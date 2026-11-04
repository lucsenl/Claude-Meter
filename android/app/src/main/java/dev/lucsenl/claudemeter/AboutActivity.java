package dev.lucsenl.claudemeter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public final class AboutActivity extends AppCompatActivity {
    private LinearLayout content;
    private boolean dark;

    @Override protected void onCreate(Bundle state) {
        Ui.applySelectedTheme(this);
        super.onCreate(state);
        dark = Ui.isDark(this);
        content = Ui.installPage(this, "About", true).content;
        render();
    }

    @Override public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void render() {
        LinearLayout card = Ui.card(this, dark);
        TextView title = Ui.text(this, "Claude Meter", 26, Ui.mainText(dark));
        title.setTypeface(Ui.mediumTypeface(this));
        card.addView(title);

        TextView version = Ui.text(this, "Version " + AppConstants.VERSION_NAME,
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
        vp.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        card.addView(version, vp);

        TextView body = Ui.text(this,
                "An independent Android client for documented Anthropic API analytics. "
                        + "Claude Meter is not affiliated with or endorsed by Anthropic.",
                14, Ui.secondaryText(dark));
        card.addView(body);

        Button project = Ui.button(this, "Project on GitHub", false, dark);
        project.setOnClickListener(v -> open(AppConstants.PROJECT_URL));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        pp.setMargins(0, Ui.dp(this, 14), 0, 0);
        card.addView(project, pp);

        Button docs = Ui.button(this, "Anthropic API documentation", false, dark);
        docs.setOnClickListener(v -> open(AppConstants.API_DOCS_URL));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        dp.setMargins(0, Ui.dp(this, 8), 0, 0);
        card.addView(docs, dp);
        content.addView(card);

        Ui.addSpacer(content, 16);
        LinearLayout attribution = Ui.card(this, dark);
        TextView at = Ui.text(this, "Attribution", 19, Ui.mainText(dark));
        at.setTypeface(Ui.mediumTypeface(this));
        attribution.addView(at);
        TextView detail = Ui.text(this,
                "This fork is based on the MIT-licensed Codex Meter project. Historical "
                        + "attribution and license notices remain in the repository. Product "
                        + "behavior, package identity and provider integration have been rebuilt "
                        + "for Anthropic.",
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.setMargins(0, Ui.dp(this, 8), 0, 0);
        attribution.addView(detail, ap);
        content.addView(attribution);
    }

    private void open(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}

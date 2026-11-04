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
                "An independent Android usage meter for Claude subscriptions. "
                        + "It signs in through Claude OAuth and reads the account profile and "
                        + "subscription usage windows. Claude Meter is not affiliated with or "
                        + "endorsed by Anthropic.",
                14, Ui.secondaryText(dark));
        card.addView(body);

        Button project = Ui.button(this, "Project on GitHub", false, dark);
        project.setOnClickListener(v -> open(AppConstants.PROJECT_URL));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        pp.setMargins(0, Ui.dp(this, 14), 0, 0);
        card.addView(project, pp);
        content.addView(card);

        Ui.addSpacer(content, 16);
        LinearLayout note = Ui.card(this, dark);
        TextView noteTitle = Ui.text(this, "Compatibility", 19, Ui.mainText(dark));
        noteTitle.setTypeface(Ui.mediumTypeface(this));
        note.addView(noteTitle);
        TextView detail = Ui.text(this,
                "Subscription usage OAuth endpoints are not a stable public third-party API. "
                        + "Claude Meter handles token refresh and unknown usage fields defensively, "
                        + "but Anthropic-side changes can require an app update.",
                14, Ui.secondaryText(dark));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.setMargins(0, Ui.dp(this, 8), 0, 0);
        note.addView(detail, np);
        content.addView(note);
    }

    private void open(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}

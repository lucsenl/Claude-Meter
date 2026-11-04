package dev.lucsenl.claudemeter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.widget.RoundedLinearLayout;

public final class Ui {
    public static final class Page {
        public final ToolbarLayout toolbar;
        public final LinearLayout content;
        Page(ToolbarLayout toolbar, LinearLayout content) {
            this.toolbar = toolbar;
            this.content = content;
        }
    }

    private Ui() {}

    public static void applySelectedTheme(Activity activity) {
        String theme = AppPreferences.getAppTheme(activity);
        if (activity instanceof AppCompatActivity) {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (AppPreferences.THEME_DARK.equals(theme)) mode = AppCompatDelegate.MODE_NIGHT_YES;
            if (AppPreferences.THEME_LIGHT.equals(theme)) mode = AppCompatDelegate.MODE_NIGHT_NO;
            ((AppCompatActivity) activity).getDelegate().setLocalNightMode(mode);
        }
        activity.setTheme(AppPreferences.isMaterialYouEnabled(activity)
                ? R.style.AppTheme_MaterialYou : R.style.AppTheme);
    }

    public static Page installPage(AppCompatActivity activity, String title, boolean back) {
        ViewGroup parent = activity.findViewById(android.R.id.content);
        View root = LayoutInflater.from(activity).inflate(
                R.layout.activity_oneui_dashboard, parent, false);
        ToolbarLayout toolbar = root.findViewById(R.id.toolbar_layout);
        LinearLayout content = root.findViewById(R.id.dashboard_content);
        toolbar.setTitle(title);
        toolbar.setShowNavigationButtonAsBack(back);
        toolbar.setExpandable(false);
        toolbar.setExpandable(true);
        toolbar.setExpanded(true, false);
        activity.setContentView(root);
        return new Page(toolbar, content);
    }

    public static void startSecondaryActivity(Activity activity,
            Class<? extends Activity> target) {
        activity.startActivity(new Intent(activity, target));
    }

    public static boolean isDark(Context context) {
        String theme = AppPreferences.getAppTheme(context);
        if (AppPreferences.THEME_DARK.equals(theme)) return true;
        if (AppPreferences.THEME_LIGHT.equals(theme)) return false;
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static int accent(Context context, boolean dark) {
        if (AppPreferences.isMaterialYouEnabled(context)
                && android.os.Build.VERSION.SDK_INT >= 31) {
            int id = context.getResources().getIdentifier(
                    dark ? "system_accent1_200" : "system_accent1_600",
                    "color", "android");
            if (id != 0) return context.getColor(id);
        }
        return Color.rgb(217, 119, 87); // Anthropic orange
    }

    public static int cardColor(Context context, boolean dark) {
        return dark ? Color.rgb(31, 30, 28) : Color.WHITE;
    }

    public static int mainText(boolean dark) {
        return dark ? Color.rgb(250, 249, 245) : Color.rgb(20, 20, 19);
    }

    public static int secondaryText(boolean dark) {
        return dark ? Color.rgb(176, 174, 165) : Color.rgb(94, 92, 86);
    }

    public static int danger(boolean dark) {
        return dark ? Color.rgb(255, 177, 173) : Color.rgb(180, 45, 45);
    }

    public static LinearLayout card(Context context, boolean dark) {
        RoundedLinearLayout card = new RoundedLinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 20), dp(context, 18), dp(context, 20), dp(context, 18));
        card.setBackground(shape(cardColor(context, dark), dp(context, 26)));
        card.setClipToOutline(true);
        card.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    public static TextView text(Context context, String value, float size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sec", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.12f);
        return view;
    }

    public static Typeface mediumTypeface(Context context) {
        return Typeface.create("sec", Typeface.BOLD);
    }

    public static Button nativePrimaryButton(Context context, String label) {
        Button button = new AppCompatButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTypeface(mediumTypeface(context));
        button.setGravity(android.view.Gravity.CENTER);
        boolean dark = isDark(context);
        int accent = accent(context, dark);
        int foreground = Color.luminance(accent) > 0.55f ? Color.BLACK : Color.WHITE;
        button.setTextColor(foreground);
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(Color.argb(35, 255, 255, 255)),
                shape(accent, dp(context, 999)), null));
        return button;
    }

    public static Button button(Context context, String label, boolean primary, boolean dark) {
        if (primary) return nativePrimaryButton(context, label);
        Button button = new AppCompatButton(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(mainText(dark));
        button.setBackground(shape(dark ? Color.rgb(48, 47, 44)
                : Color.rgb(232, 230, 220), dp(context, 999)));
        return button;
    }

    public static void addSpacer(LinearLayout parent, int dp) {
        View spacer = new View(parent.getContext());
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(parent.getContext(), dp)));
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }
}

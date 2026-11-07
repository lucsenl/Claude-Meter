package dev.bennett.claudemeter;

import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parses the account identity and subscription reported by Claude OAuth profile.
 * Never infers subscription tier from usage utilization or fabricates "Pro 20x".
 */
public final class ClaudeAccountProfile {
    public final String accountId;
    public final String email;
    public final String plan;

    private ClaudeAccountProfile(String accountId, String email, String plan) {
        this.accountId = accountId;
        this.email = email;
        this.plan = plan;
    }

    public static ClaudeAccountProfile parse(String json, String previousAccountId,
            String previousEmail) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONObject account = root.optJSONObject("account");
        JSONObject organization = root.optJSONObject("organization");
        String accountId = firstNonempty(
                account == null ? "" : account.optString("uuid", ""),
                account == null ? "" : account.optString("id", ""),
                previousAccountId);
        String email = firstNonempty(
                account == null ? "" : account.optString("email", ""),
                previousEmail);
        return new ClaudeAccountProfile(accountId, email, resolvePlan(root));
    }

    public static String resolvePlan(JSONObject root) {
        if (root == null) return "";
        JSONObject org = root.optJSONObject("organization");
        JSONObject account = root.optJSONObject("account");

        // Explicit plan names take precedence over legacy capability booleans.
        String name = displayLabel(org == null ? "" : org.optString("plan_display_name", ""));
        if (!name.isEmpty()) return name;
        name = displayLabel(root.optString("plan_display_name", ""));
        if (!name.isEmpty()) return name;

        String type = org == null ? "" : org.optString("organization_type", "");
        String tier = org == null ? "" : org.optString("rate_limit_tier", "");
        switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "claude_pro": return "Pro";
            case "claude_max":
                // Claude renamed the old Max 20x tier to Ultra. The rate-limit
                // tier is relevant ONLY once the org type identifies a Max plan.
                return "default_claude_max_20x".equalsIgnoreCase(tier)
                        ? "Ultra" : "Max";
            case "claude_ultra": return "Ultra";
            case "claude_team": return "Team";
            case "claude_enterprise": return "Enterprise";
            case "claude_free": return "Free";
            default: break;
        }
        if (account != null) {
            if (account.optBoolean("has_claude_max", false)) return "Max";
            if (account.optBoolean("has_claude_pro", false)) return "Pro";
        }
        return ""; // Unknown: show a neutral Claude label, not a guessed paid plan.
    }

    public static String displayLabel(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "").replace("-", "").replace("_", "");
        if (normalized.startsWith("claude")) {
            normalized = normalized.substring("claude".length());
        }
        switch (normalized) {
            case "free": return "Free";
            case "pro": case "prolite": case "pro5x":
            case "pro20x": return "Pro"; // Old bad label is normalized conservatively.
            case "max": case "max5x": return "Max";
            case "max20x": case "ultra": return "Ultra";
            case "team": return "Team";
            case "enterprise": return "Enterprise";
            default: return "";
        }
    }

    private static String firstNonempty(String... candidates) {
        for (String value : candidates) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}

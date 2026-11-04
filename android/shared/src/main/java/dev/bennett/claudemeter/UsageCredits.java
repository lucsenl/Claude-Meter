package dev.bennett.claudemeter;

import java.math.BigDecimal;
import org.json.JSONException;
import org.json.JSONObject;

/** Claude Extra Usage state returned by the usage endpoint. */
public final class UsageCredits {
    public final boolean isEnabled;
    public final String monthlyLimit;
    public final String usedCredits;
    public final int utilization;
    public final String currency;

    // Compatibility aliases used by older dashboard code while the UI migrates.
    public final boolean hasCredits;
    public final boolean unlimited;
    public final String balance;

    public UsageCredits(boolean isEnabled, String monthlyLimit, String usedCredits,
            int utilization, String currency) {
        this.isEnabled = isEnabled;
        this.monthlyLimit = clean(monthlyLimit);
        this.usedCredits = clean(usedCredits);
        this.utilization = clamp(utilization);
        this.currency = clean(currency);
        this.hasCredits = isEnabled;
        this.unlimited = false;
        this.balance = remaining(this.monthlyLimit, this.usedCredits);
    }

    public boolean shouldDisplay() {
        return isEnabled && (!monthlyLimit.isEmpty() || !usedCredits.isEmpty() || utilization > 0);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("is_enabled", isEnabled);
        if (!monthlyLimit.isEmpty()) object.put("monthly_limit", monthlyLimit);
        if (!usedCredits.isEmpty()) object.put("used_credits", usedCredits);
        object.put("utilization", utilization);
        if (!currency.isEmpty()) object.put("currency", currency);
        return object;
    }

    public static UsageCredits fromJson(JSONObject object) {
        if (object == null) return null;
        if (object.has("is_enabled") || object.has("monthly_limit")
                || object.has("used_credits") || object.has("utilization")) {
            return new UsageCredits(
                    object.optBoolean("is_enabled", false),
                    value(object.opt("monthly_limit")),
                    value(object.opt("used_credits")),
                    (int) Math.round(object.optDouble("utilization", 0.0d)),
                    object.optString("currency", ""));
        }
        return null;
    }

    public static UsageCredits fromExtraUsage(JSONObject object) {
        return fromJson(object);
    }

    private static String value(Object raw) {
        return raw == null || JSONObject.NULL.equals(raw) ? "" : String.valueOf(raw);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String remaining(String limit, String used) {
        try {
            if (limit.isEmpty() || used.isEmpty()) return "";
            BigDecimal result = new BigDecimal(limit).subtract(new BigDecimal(used));
            return result.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ignored) {
            return "";
        }
    }
}

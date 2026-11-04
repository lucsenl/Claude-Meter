package dev.bennett.claudemeter;

import java.util.Locale;

/**
 * Pricing/value estimation is intentionally disabled.
 *
 * Claude Meter displays provider-reported usage and Extra Usage data. It does not invent an
 * API-dollar value for subscription quotas because that conversion is not a provider contract.
 */
public final class PlanPricing {
    public final String planKey;
    public final String planLabel;
    public final double monthlyPriceUsd;
    public final double monthlyValueUsd;

    private PlanPricing(String planKey, String planLabel, double monthlyPriceUsd,
            double monthlyValueUsd) {
        this.planKey = planKey;
        this.planLabel = planLabel;
        this.monthlyPriceUsd = monthlyPriceUsd;
        this.monthlyValueUsd = monthlyValueUsd;
    }

    public static PlanPricing forPlan(String planType) {
        return null;
    }

    public double weeklyValueUsd() { return 0d; }
    public double fiveHourValueUsd() { return 0d; }
    public double windowValueUsd(String historyKind) { return 0d; }
    public double estimatedValueUsd(String historyKind, double usedPercent) { return 0d; }
    public double valueMultiplier() { return 0d; }

    public static String formatUsd(double value) {
        double amount = Math.max(0d, value);
        if (amount >= 10d) return String.format(Locale.US, "$%,d", Math.round(amount));
        return String.format(Locale.US, "$%.2f", amount);
    }
}

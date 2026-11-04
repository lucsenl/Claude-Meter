package dev.lucsenl.claudemeter;

public final class ParserSelfTest {
    public static void main(String[] args) throws Exception {
        testUsageAndProfile();
        testNullWindows();
        testSnapshotRoundTrip();
        System.out.println("Claude Meter subscription parser tests passed.");
    }

    private static void testUsageAndProfile() throws Exception {
        String usage = "{"
                + "\"five_hour\":{\"utilization\":15.0,"
                + "\"resets_at\":\"2026-11-04T18:00:00+00:00\"},"
                + "\"seven_day\":{\"utilization\":36.0,"
                + "\"resets_at\":\"2026-11-09T18:00:00+00:00\"},"
                + "\"extra_usage\":{\"is_enabled\":true},"
                + "\"member_dashboard_available\":true,"
                + "\"limits\":["
                + "{\"kind\":\"session\",\"percent\":15,\"scope\":null},"
                + "{\"kind\":\"weekly_scoped\",\"percent\":71,"
                + "\"resets_at\":\"2026-11-09T18:00:00+00:00\","
                + "\"scope\":{\"model\":{\"display_name\":\"Fable\"}},"
                + "\"is_active\":true}"
                + "]}";
        String profile = "{"
                + "\"account\":{\"email\":\"user@example.com\","
                + "\"display_name\":\"Example User\"},"
                + "\"organization\":{\"organization_type\":\"claude_max\","
                + "\"rate_limit_tier\":\"default_claude_max_20x\","
                + "\"subscription_status\":\"active\"}}";

        UsageSnapshot value = UsageParser.parse(usage, profile, 123456L);
        assert "Example User".equals(value.displayName);
        assert "user@example.com".equals(value.email);
        assert "Max 20×".equals(value.plan) : value.plan;
        assert value.fiveHourUtilization == 15d;
        assert value.sevenDayUtilization == 36d;
        assert value.extraUsageEnabled;
        assert value.memberDashboardAvailable;
        assert value.scopedLimits.size() == 1 : value.scopedLimits.size();
        assert "Fable · Weekly".equals(value.scopedLimits.get(0).label);
        assert value.scopedLimits.get(0).utilization == 71d;
        assert value.scopedLimits.get(0).active;
    }

    private static void testNullWindows() throws Exception {
        UsageSnapshot value = UsageParser.parse(
                "{\"five_hour\":null,\"seven_day\":null}",
                "{\"account\":{},\"organization\":{}}",
                1L);
        assert !value.hasFiveHour();
        assert !value.hasSevenDay();
        assert "Claude".equals(value.plan);
    }

    private static void testSnapshotRoundTrip() throws Exception {
        UsageSnapshot original = UsageParser.parse(
                "{\"five_hour\":{\"utilization\":22.5},"
                        + "\"seven_day\":{\"utilization\":48.0}}",
                "{\"account\":{\"email\":\"a@b.test\"},"
                        + "\"organization\":{\"organization_type\":\"claude_pro\"}}",
                999L);
        UsageSnapshot restored = UsageSnapshot.fromJson(original.toJson());
        assert restored != null;
        assert "Pro".equals(restored.plan);
        assert restored.fiveHourUtilization == 22.5d;
        assert restored.sevenDayUtilization == 48d;
        assert restored.fetchedAtMillis == 999L;
    }
}

package dev.lucsenl.claudemeter;

import java.util.List;

public final class ParserSelfTest {
    public static void main(String[] args) throws Exception {
        testUsageTotals();
        testCostConversion();
        testModels();
        testSnapshotRoundTrip();
        System.out.println("Claude Meter parser tests passed.");
    }

    private static void testUsageTotals() throws Exception {
        String json = "{\"data\":["
                + "{\"usage\":{\"total_tokens\":1200,\"requests\":4}},"
                + "{\"usage\":{\"total_tokens\":800,\"requests\":2}}]}";
        UsageParser.Totals totals = UsageParser.usageTotals(json);
        assert totals.tokens == 2000L : totals.tokens;
        assert totals.requests == 6L : totals.requests;
    }

    private static void testCostConversion() throws Exception {
        String json = "{\"data\":[{\"cost\":{\"amount\":\"123.450000\"}},"
                + "{\"cost\":{\"amount\":\"126.550000\"}}]}";
        assert "2.50".equals(UsageParser.costUsd(json)) : UsageParser.costUsd(json);
    }

    private static void testModels() throws Exception {
        String json = "{\"data\":["
                + "{\"id\":\"claude-a\",\"display_name\":\"Claude A\"},"
                + "{\"id\":\"claude-b\",\"display_name\":\"Claude B\"},"
                + "{\"id\":\"claude-a\",\"display_name\":\"Claude A\"}]}";
        List<String> models = UsageParser.models(json);
        assert models.size() == 2 : models;
        assert "Claude A".equals(models.get(0));
        assert "Claude B".equals(models.get(1));
    }

    private static void testSnapshotRoundTrip() throws Exception {
        UsageSnapshot original = new UsageSnapshot(
                true, "", "org_1", "Example Org",
                100L, 2L, 700L, 8L, "3.21",
                List.of("Claude A", "Claude B"), 123456L);
        UsageSnapshot restored = UsageSnapshot.fromJson(original.toJson());
        assert restored != null;
        assert restored.analyticsAvailable;
        assert restored.todayTokens == 100L;
        assert restored.weekTokens == 700L;
        assert restored.weekRequests == 8L;
        assert "3.21".equals(restored.monthCostUsd);
        assert restored.activeModels.size() == 2;
        assert restored.fetchedAtMillis == 123456L;
    }
}

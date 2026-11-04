package dev.bennett.claudemeter;

import org.json.JSONObject;

/** Contract tests: labels must come from Claude profile fields, never quota math. */
public final class ClaudeAccountProfileTest {
    private static void verify(boolean ok, String description) {
        if (!ok) throw new AssertionError(description);
    }

    public static void main(String[] args) throws Exception {
        check("{\"account\":{\"uuid\":\"abc\",\"email\":\"u@example.test\","
                + "\"has_claude_pro\":true},\"organization\":{\"organization_type\":\"claude_pro\","
                + "\"rate_limit_tier\":\"default_claude_ai\"}}",
                "Pro", "Claude organization Pro");
        check("{\"account\":{\"has_claude_pro\":true},"
                + "\"organization\":{\"organization_type\":\"claude_max\","
                + "\"rate_limit_tier\":\"default_claude_max_5x\"}}",
                "Max", "Claude Max 5x");
        check("{\"organization\":{\"organization_type\":\"claude_max\","
                + "\"rate_limit_tier\":\"default_claude_max_20x\"}}",
                "Ultra", "old Max 20x is now Ultra");
        check("{\"organization\":{\"organization_type\":\"claude_team\"}}",
                "Team", "Claude Team");
        check("{\"organization\":{\"organization_type\":\"claude_enterprise\"}}",
                "Enterprise", "Claude Enterprise");
        check("{\"organization\":{\"plan_display_name\":\"Claude Ultra\","
                + "\"organization_type\":\"claude_pro\"}}",
                "Ultra", "provider explicit plan name wins");
        check("{\"organization\":{\"organization_type\":\"unknown\","
                + "\"rate_limit_tier\":\"default_claude_max_20x\"}}",
                "", "tier alone cannot establish a paid plan");
        check("{\"account\":{\"has_claude_pro\":true}}",
                "Pro", "legacy Pro capability flag fallback");
        check("{}", "", "unknown profile must not synthesize Max");

        ClaudeAccountProfile profile = ClaudeAccountProfile.parse(
                "{\"account\":{\"uuid\":\"abc\",\"email\":\"u@example.test\"},"
                + "\"organization\":{\"organization_type\":\"claude_pro\"}}",
                "old-id", "old@example.test");
        verify("abc".equals(profile.accountId), "account ID from profile");
        verify("u@example.test".equals(profile.email), "account email from profile");
        verify("Pro".equals(profile.plan), "account plan from organization");
        verify("Pro".equals(ClaudeAccountProfile.displayLabel("pro")),
                "bare Pro must never mean Pro 20x");
        verify("Pro".equals(ClaudeAccountProfile.displayLabel("Pro 20x")),
                "legacy incorrectly labeled Pro 20x demoted to Pro");
        verify("".equals(ClaudeAccountProfile.displayLabel("Plus")),
                "OpenAI Plus has no Claude subscription equivalent");
        verify("".equals(ClaudeAccountProfile.displayLabel("Go")),
                "OpenAI Go has no Claude subscription equivalent");
        System.out.println("Claude profile and subscription-label tests passed.");
    }

    private static void check(String body, String expected, String description) throws Exception {
        ClaudeAccountProfile profile = ClaudeAccountProfile.parse(body, "", "");
        verify(expected.equals(profile.plan), description + ": got " + profile.plan);
    }
}

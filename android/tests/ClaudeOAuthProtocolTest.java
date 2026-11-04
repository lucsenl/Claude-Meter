package dev.bennett.claudemeter;

public final class ClaudeOAuthProtocolTest {
    public static void main(String[] args) throws Exception {
        ClaudeOAuthProtocol.LoginRequest req = ClaudeOAuthProtocol.beginLogin();
        check(req.url.startsWith("https://claude.com/cai/oauth/authorize?"), "authorize host");
        check(req.url.contains("code_challenge_method=S256"), "PKCE S256");
        check(req.url.contains("user%3Aprofile+user%3Ainference"), "minimal OAuth scopes");
        check(!req.url.contains("org%3Acreate_api_key"), "no API key creation");
        check(!req.url.contains("claude_code"), "no impersonation scope");
        check(req.verifier.length() > 42, "strong verifier");
        check(req.state.length() > 24, "strong state");
        check(ClaudeOAuthProtocol.stateMatches(req.state, req.state), "state match");
        check(!ClaudeOAuthProtocol.stateMatches(req.state, "wrong"), "state reject");
        ClaudeOAuthProtocol.AuthorizationResult result =
                ClaudeOAuthProtocol.parseAuthorizationResult("test-code#" + req.state);
        check("test-code".equals(result.code), "code parsed");
        check(req.state.equals(result.state), "state parsed");
        System.out.println("Claude PKCE protocol: state and scope checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

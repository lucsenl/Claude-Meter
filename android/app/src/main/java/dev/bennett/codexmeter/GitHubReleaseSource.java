package dev.bennett.codexmeter;

/** Canonical production repository used for release discovery and project links. */
public final class GitHubReleaseSource {
    public static final String REPOSITORY_URL =
            "https://github.com/lucsenl/Claude-Meter"; // pragma: allowlist secret
    public static final String RELEASES_API_URL =
            "https://api.github.com/repos/lucsenl/Claude-Meter/releases?per_page=30"; // pragma: allowlist secret

    private GitHubReleaseSource() {
    }
}

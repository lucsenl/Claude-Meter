# Claude terminology evidence

This file records the naming rules for the Claude Meter migration. It exists to prevent blind
OpenAI/ChatGPT -> Claude string replacement.

Verified on 2026-11-04 against current Anthropic/Claude first-party documentation.

## Naming rules

| Old Codex Meter wording | Claude Meter wording | Evidence / rationale |
|---|---|---|
| ChatGPT account | Claude account | Claude Help Center consistently calls the consumer identity a "Claude account". |
| Sign in with ChatGPT | Sign in with Claude | First-party Claude surfaces describe signing in to Claude / a Claude account. |
| ChatGPT/Codex account | Claude account | Claude and Claude Code share the same signed-in subscription identity and usage limits. |
| OpenAI (company/provider) | Anthropic | Anthropic is the provider/company behind Claude. |
| OpenAI API | Claude API | First-party platform documentation names the primary REST product "Claude API". |
| OpenAI services | Anthropic services | First-party Help Center uses "Anthropic services" for provider-level access. |
| OpenAI returned/reported (network/provider context) | Anthropic returned/reported, or Claude usage data when user-facing | Choose provider vs product based on context; never mechanically replace with Claude. |
| OpenAI page / ChatGPT page (login UI) | Claude authorization page | Avoid claiming a specific backend host in user-facing copy; the user is authorizing a Claude account. |
| ChatGPT tokens (consumer OAuth credential) | Claude authorization tokens | Claude Help Center calls Claude Code credentials authorization/OAuth tokens. |
| Codex usage limits | Claude usage limits | First-party Help Center uses "usage limits" for Claude and Claude Code. |
| 5-hour window | five-hour session limit | First-party Help Center wording. |
| weekly window | weekly limit | First-party Help Center wording. |
| model-specific weekly limit | named weekly model limit (currently Fable where applicable) | First-party docs currently document a separate Fable weekly limit for eligible plans. |
| purchased usage credits | usage credits | "Usage credits" is the first-party Claude term. |
| Codex reset credits | no Claude equivalent | Remove; do not rename to usage credits. |

## First-party evidence

- Claude account/login terminology:
  https://support.claude.com/en/articles/13189465-log-in-to-your-claude-account
- Claude Code subscription login wording ("Claude account with subscription"):
  https://support.claude.com/en/articles/11845131-use-claude-code-with-your-team-or-enterprise-plan
- Claude / Claude Code usage limits:
  https://support.claude.com/en/articles/11647753-how-do-usage-and-length-limits-work
- Claude Code with Pro/Max/Ultra and shared usage:
  https://support.claude.com/en/articles/11145838-use-claude-code-with-your-pro-max-or-ultra-plan
- Usage credits:
  https://support.claude.com/en/articles/12429409-manage-usage-credits-for-paid-claude-plans
- Claude API naming and provider:
  https://platform.claude.com/docs/en/api/overview
- Claude Code OAuth token as a supported Claude Code credential:
  https://support.claude.com/en/articles/14128775-claude-code-on-console-to-enterprise-migration
- Claude Code authorization-token lifecycle:
  https://support.claude.com/en/articles/10310342-how-do-i-log-out-of-all-active-sessions

## Important third-party-client constraint

Anthropic's first-party login documentation says the preferred authentication mechanism for
third-party software is an API key through Claude Console or a supported cloud provider. It also
states that Anthropic may allow paid subscribers with usage credits to use certain third-party
tools, but prohibits tools that misrepresent their identity or attempt to route third-party traffic
against subscription limits.

Source:
https://support.claude.com/en/articles/13189465-log-in-to-your-claude-account

Therefore Claude Meter must not simply copy Claude Code's private OAuth client identity/endpoints
and present itself as Claude Code. Any subscription-OAuth implementation requires a separately
justified, non-impersonating integration path. Until that is established, endpoint/client-id
replacement is NOT a terminology cleanup.

## Technical strings that must not be blindly renamed

These are implementation contracts, not branding copy:

- `https://auth.openai.com/...`
- `https://chatgpt.com/backend-api/...`
- `ChatGPT-Account-Id`
- `https://api.openai.com/auth`
- JWT claims such as `chatgpt_account_id`
- OpenAI OAuth client identifiers
- persisted Keychain/Keystore keys used for migration compatibility

They disappear only when the corresponding OpenAI implementation is removed or migrated. Changing
the spelling while leaving the old protocol in place would make the code incorrect.

## Historical records

Past changelog entries may retain OpenAI/ChatGPT wording when they describe behavior of the original
Codex Meter release. Historical facts are not rebranded. Current product documentation, active UI,
active code paths, tests, identifiers, and network contracts must converge on Claude/Anthropic as
their implementations are migrated.

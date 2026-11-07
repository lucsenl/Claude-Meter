# Claude provider and brand evidence

This file records the sources used for product terminology, provider contracts and visual styling.

## Terminology

Use:

- Claude account
- Sign in with Claude
- Claude usage limits
- five-hour session limit
- weekly limit
- usage credits
- Anthropic for provider/company references
- Claude API for the API product

## Brand palette

Anthropic's published brand styling uses:

- dark: `#141413`
- light: `#FAF9F5`
- mid gray: `#B0AEA5`
- light gray: `#E8E6DC`
- orange: `#D97757`
- blue: `#6A9BCC`
- green: `#788C5D`

Claude Meter uses those colors for its own meter artwork and UI accents. It does not copy or alter Anthropic logos.

## Trademark constraint

Anthropic's trademark guidelines require permission for use of official marks and prohibit altered official logos or presentation that implies endorsement. Claude Meter therefore uses an original meter symbol rather than an Anthropic logo.

## Sources

- Claude account sign-in: https://support.claude.com/en/articles/13189465-log-in-to-your-claude-account
- Claude usage limits: https://support.claude.com/en/articles/11647753-how-do-usage-and-length-limits-work
- Claude Code subscription usage: https://support.claude.com/en/articles/11145838-use-claude-code-with-your-pro-max-or-ultra-plan
- Usage credits: https://support.claude.com/en/articles/12429409-manage-usage-credits-for-paid-claude-plans
- Claude API overview: https://platform.claude.com/docs/en/api/overview
- Anthropic trademark guidelines: https://www.anthropic.com/legal/trademark-guidelines
- Anthropic brand styling example: https://claude.com/resources/articles/how-to-create-skills-key-steps-limitations-and-examples

## Engineering rule

Brand strings and network contracts are different concerns. A provider endpoint, client identifier, header or persisted protocol key must only change when its Claude behavior is verified. UI copy and visual assets follow the Claude terminology and palette above.

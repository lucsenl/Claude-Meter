# Contributing

Claude Meter is a phone-only Android project.

- app code: `android/app/`
- parser/source tests: `android/tests/`
- package: `dev.lucsenl.claudemeter`
- provider integration: documented Anthropic APIs only

Do not add Wear OS, iOS, private Claude.ai endpoints, Claude Code OAuth impersonation, or
subscription-quota scraping.

Before submitting changes:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

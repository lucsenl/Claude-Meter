# Contributing

Claude Meter is a phone-only Android project.

- app code: `android/app/`
- parser/source tests: `android/tests/`
- package: `dev.lucsenl.claudemeter`
- authentication: Claude OAuth authorization-code + PKCE
- usage: Claude subscription profile and usage windows

Do not add API-key onboarding or spoof another application's identity.

Before submitting changes:

```bash
./run-tests.sh
./lint.sh
./build.sh
```

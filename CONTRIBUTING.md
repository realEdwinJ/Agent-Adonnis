# Contributing to Adonnis

Thanks for wanting to help! Here's how to get started.

## Development setup

1. Clone the repo and open it in Android Studio.
2. Ensure you have the Android SDK 35 installed.
3. Run the app on an emulator or device.
4. Complete onboarding with an [OpenRouter API key](https://openrouter.ai/keys).

## Branching & commits

- Create a feature branch off `main`: `git checkout -b feature/my-change`
- Commit messages should be concise and describe the change, e.g. `Add snooze limit to math challenge alarm`
- Before opening a PR, make sure the project builds: `./gradlew assembleDebug`

## Code style

- Follow the existing conventions (MVVM, ViewModels + Repositories, Compose).
- Keep AI prompt templates in `ai/Prompts.kt` — never inline prompts in screens.
- Never log or commit API keys.

## Pull requests

- Keep PRs small and focused on a single concern.
- Describe what changed and why.
- Test the flow end-to-end (onboarding → chat → plan → alarm) before submitting.

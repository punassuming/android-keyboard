# Usage and development workflows

## Common local workflows

- Build debug for day-to-day development:
  - `./gradlew assembleUnstableDebug`
- Build release-style artifacts:
  - `./gradlew assembleUnstableRelease`
  - `./gradlew assembleStableRelease`
  - `./gradlew bundlePlaystoreRelease`

## Translation workflow snippets

- Translation resources are integrated through the `translations/` directories used by Gradle source sets.
- Locale/text generation for keyboard text data is maintained by:
  - `tools/make-keyboard-text-py`
  - `./gradlew updateLocales`
- CI sync automation references `syncTranslations.sh` for pushing updated string resources.

## Flavor overview

Defined product flavors include:

- `unstable`
- `stable`
- `playstore`

These flavors are configured in `build.gradle` and differ in IDs, update behavior, and distribution settings.

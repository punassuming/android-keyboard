# Getting started

## Clone and initialize

```bash
git clone --recursive https://gitlab.futo.org/keyboard/latinime.git
cd latinime
```

If you already cloned without submodules:

```bash
git submodule update --init --recursive
```

## Build locally

From repository root:

```bash
./gradlew assembleUnstableDebug
./gradlew assembleStableRelease
```

You can also open the project in Android Studio and build from there.

## Helpful build tasks from this codebase

- `./gradlew updateLocales` — regenerates keyboard text artifacts from `tools/make-keyboard-text-py`
- `./gradlew updateBundleResources` — updates Play Store bundle resources from `java/res-bundle`
- `./gradlew updateContributors` — regenerates contributors Kotlin source from `tools/contributors.py`

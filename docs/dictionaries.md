# Dictionaries

FUTO Keyboard uses binary dictionaries (`.dict` files) for word suggestions, spell-check, and autocorrect.  This document explains the dictionary format, how to create a new wordlist, how to compile it into a binary dictionary, and how to include it in the app.

---

## Table of Contents

1. [Overview](#overview)
2. [Using a Custom Dictionary](#using-a-custom-dictionary)
3. [The Combined (Source) Format](#the-combined-source-format)
4. [Compiling a Dictionary](#compiling-a-dictionary)
5. [Adding a Dictionary to the App](#adding-a-dictionary-to-the-app)
6. [Dictionary Naming and Locale Matching](#dictionary-naming-and-locale-matching)
7. [Reference: Word Frequency Scale](#reference-word-frequency-scale)

---

## Overview

The pipeline for dictionaries is:

```
Plain-text .combined file  →  makedict tool  →  Binary .dict file  →  Bundled in APK / imported by user
```

- **`.combined`** files are human-readable CSV-like text files that list words with their frequencies and optional n-gram and shortcut data.
- **`.dict`** files are compiled binary dictionaries used at runtime.  They start with the magic bytes `0x9B 0xC1 0x3A 0xFE`.
- The app bundles a minimal fallback dictionary (`main.dict`) and loads locale-specific dictionaries from Android [split APKs / AABs](https://developer.android.com/guide/playcore/feature-delivery) at runtime.

Pre-built dictionaries for many languages are available from the FUTO keyboard add-on site:

> <https://keyboard.futo.org/dictionaries>

---

## Using a Custom Dictionary

### Importing via the keyboard settings

1. Compile your wordlist into a `.dict` file (see [Compiling a Dictionary](#compiling-a-dictionary)).
2. Transfer the file to your Android device.
3. Open the file with any file manager — Android will offer to open it with FUTO Keyboard.
4. The keyboard's import screen will display the dictionary's locale and description; confirm to install.

### What the keyboard checks on import

The keyboard reads the first 4 bytes of the file to determine its type:

| Magic bytes | Type |
|-------------|------|
| `47 47 55 46` (`GGUF`) | Transformer language model |
| `9B C1 3A FE` | Binary dictionary |
| `EF 4D 4F 5A` | Japanese Mozc dictionary |
| `1F 8B …` (gzip) | **Invalid** — you imported a raw wordlist instead of a compiled `.dict` |

If you receive an "invalid file" error saying the file looks like a wordlist, you need to compile the `.combined` source into a `.dict` first.

The import logic is in [`java/src/org/futo/inputmethod/latin/uix/ImportResourceActivity.kt`](../java/src/org/futo/inputmethod/latin/uix/ImportResourceActivity.kt).

---

## The Combined (Source) Format

The `.combined` format is the human-readable source for binary dictionaries.  The reference sample is at [`dictionaries/sample.combined`](../dictionaries/sample.combined).

### File structure

```
# Lines starting with # are comments and are ignored.

# The first non-comment line is the header — a single CSV line:
dictionary=<id>,locale=<locale>,description=<description>,date=<unix-timestamp>,version=<int>

# Subsequent lines are word entries (one space of indentation):
 word=<word>,f=<frequency>

# A word can have bigram hints (two spaces of indentation):
  bigram=<following-word>,f=<bigram-frequency>

# A word can have shortcut/whitelist entries (two spaces of indentation):
  shortcut=<expansion>,f=<shortcut-frequency>
```

### Header fields

| Field | Description |
|-------|-------------|
| `dictionary` | Identifier string, conventionally `main:<locale>` (e.g. `main:en`) |
| `locale` | BCP-47 locale tag (e.g. `en`, `en_US`, `fr`) |
| `description` | Human-readable description shown in the import UI |
| `date` | Unix timestamp (seconds since epoch) |
| `version` | Integer version number |

### Word entry fields

| Field | Description |
|-------|-------------|
| `word` | The word string |
| `f` | Frequency: integer 0–255 on a logarithmic scale (see [Reference: Word Frequency Scale](#reference-word-frequency-scale)) |
| `not_a_word=true` | Marks the entry as a non-word (used only as a shortcut target or allowlist entry) |

### Bigram lines (two-space indent, child of a word)

| Field | Description |
|-------|-------------|
| `bigram` | The word that follows the parent word |
| `f` | Bigram frequency (0–255); overrides the unigram frequency when this word follows the parent |

### Shortcut lines (two-space indent, child of a word)

| Field | Description |
|-------|-------------|
| `shortcut` | The expansion string |
| `f` | Shortcut frequency 0–14, or the special value `whitelist` (= 15) which allowlists the parent word unconditionally |

### Minimal example

```
dictionary=main:en,locale=en,description=English,date=1700000000,version=1
 word=hello,f=200
  bigram=world,f=210
 word=world,f=195
 word=omg,f=10,not_a_word=true
  shortcut=oh my god,f=whitelist
 word=badword,f=0
```

---

## Compiling a Dictionary

The `dicttoolkit` native command-line tool converts `.combined` files into binary `.dict` files.

### Building dicttoolkit

`dicttoolkit` is built as part of the normal Android build and uses the Android NDK.  To build it standalone for the host machine you can use Android NDK CMake directly, but the simplest approach is to build the full project:

```bash
./gradlew assembleUnstableDebug
```

For a host-side command-line build (Linux/macOS):

```bash
cd native/dicttoolkit
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build
./build/dicttoolkit --help
```

> **Note:** The `dicttoolkit` build requires the Android NDK to be installed and `ANDROID_NDK_HOME` set when cross-compiling.  For host-side builds, a standard C++ toolchain is sufficient.

### Running makedict

```bash
dicttoolkit makedict -o combined source.combined output.dict
# or to produce a v4 binary dictionary:
dicttoolkit makedict -o 4 source.combined output.dict
```

Supported output formats (`-o` flag):

| Value | Format |
|-------|--------|
| `2` | Binary version 2 (Jelly Bean) |
| `4` | Binary version 4 |
| `combined` | Combined text format (round-trip) |

The source code for the `makedict` command is in [`native/dicttoolkit/src/command_executors/makedict_executor.cpp`](../native/dicttoolkit/src/command_executors/makedict_executor.cpp).

### Using the existing pre-built dictionaries

The `dictionaries/` directory contains pre-built gzip-compressed wordlists (`.combined.gz`) for many languages.  These are the source wordlists — they must be compiled into `.dict` files before use.  For most contributors, it is easier to download a pre-built `.dict` file from the FUTO keyboard add-on site:

> <https://keyboard.futo.org/dictionaries>

---

## Adding a Dictionary to the App

### As a locale split (for Play Store / AAB builds)

Locale-specific dictionaries are packaged as Android split APKs so only the relevant dictionary is downloaded.  The file must be placed in the locale-specific raw resource directory and follow the naming convention:

```
java/res-bundle/raw-<language>/main_<locale>.dict
```

For example, for French (`fr`):

```
java/res-bundle/raw-fr/main_fr.dict
```

A zero-byte placeholder file must also exist at:

```
java/res-bundle/raw/main_fr.dict
```

This ensures the resource identifier exists in the base APK so the fallback lookup does not fail on devices that have not downloaded the split.  See the existing `download-bundles.py` script in `java/res-bundle/` for an example of this pattern.

The `BundleHelper` class handles finding the correct split at runtime: [`java/src/org/futo/inputmethod/latin/BundleHelper.kt`](../java/src/org/futo/inputmethod/latin/BundleHelper.kt).

### As a built-in fallback (for non-Play builds)

For sideload or F-Droid builds, place the dictionary directly in `java/res/raw/`:

```
java/res/raw/main_en_us.dict
```

The resource name must match the pattern `main_<locale_lowercase>` (underscores replace hyphens and `#` characters).  The `Dictionaries.kt` utility generates candidate names automatically from the device locale:

```
main_en_US   →  main_en_us
main_en      (language-only fallback)
```

The lookup logic is in [`java/src/org/futo/inputmethod/latin/utils/Dictionaries.kt`](../java/src/org/futo/inputmethod/latin/utils/Dictionaries.kt).

### Rebuild after adding a dictionary

```bash
./gradlew assembleUnstableDebug
```

The `aaptOptions { noCompress 'dict' }` line in `build.gradle` ensures `.dict` files are stored uncompressed inside the APK so the native code can memory-map them directly.

---

## Dictionary Naming and Locale Matching

When the keyboard needs a dictionary for locale `fr_CA`, it tries these resource names in order (from `Dictionaries.kt`):

1. `main_fr_CA` (exact match)
2. `main_fr` (language-only fallback)

For BCP-47 locales that contain a script tag (e.g. `sr_Latn`), the Android resource system maps `-` and `#` to `_` automatically.

---

## Reference: Word Frequency Scale

The `f` field uses a **logarithmic scale** from 0 to 255:

- **255** corresponds to probability ≈ 1 (the most common words, e.g. "the", "a").
- Each decrement of 1 divides the probability by approximately **1.15**.
- **0** is special: it marks a word as *profanity* — the word is recognized as valid (not flagged as a typo) but will **never** be suggested to the user.

As a rough guide:

| Frequency | Meaning |
|-----------|---------|
| 240–255 | Extremely common words |
| 200–239 | Common words |
| 150–199 | Moderately common words |
| 100–149 | Less common words |
| 50–99 | Rare words |
| 1–49 | Very rare words |
| 0 | Profanity (no suggestion, no typo) |

For bigrams, the frequency range is 0–255 using the same scale; higher values make the bigram override the unigram probability more aggressively.

For shortcuts, the range is 0–14, plus the special string value `whitelist` (which evaluates to 15 and unconditionally allowlists the parent word).

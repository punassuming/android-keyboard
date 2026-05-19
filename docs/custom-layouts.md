# Custom Keyboard Layouts

FUTO Keyboard uses a YAML-based layout system that lets you define your own keyboard from scratch or tweak an existing one.  This document explains the layout format, how to create a custom layout inside the app, and how to contribute layouts to the official layouts repository.

---

## Table of Contents

1. [Overview](#overview)
2. [Creating a Layout in the App](#creating-a-layout-in-the-app)
3. [Layout YAML Format](#layout-yaml-format)
    - [Top-level fields](#top-level-fields)
    - [Rows](#rows)
    - [Keys](#keys)
    - [Key attributes](#key-attributes)
    - [Key widths](#key-widths)
4. [Template keys](#template-keys)
5. [Examples](#examples)
    - [Minimal layout](#minimal-layout)
    - [Layout with custom widths](#layout-with-custom-widths)
    - [Layout with shifted letters](#layout-with-shifted-letters)
    - [Layout with long-press / more-keys](#layout-with-long-press--more-keys)
6. [Testing your layout](#testing-your-layout)
7. [Contributing a layout](#contributing-a-layout)

---

## Overview

Each keyboard is described by a single YAML file.  The app ships with a collection of built-in layouts (maintained in a [separate repository](https://github.com/futo-org/futo-keyboard-layouts)), but you can also write your own directly inside the app and use them immediately.

The full, authoritative specification for the layout format lives in that repository:

> <https://github.com/futo-org/futo-keyboard-layouts/blob/main/LayoutSpec.md>

This document gives a practical introduction.

---

## Creating a Layout in the App

You can create and edit custom layouts entirely inside FUTO Keyboard without writing any code or using external tools.

1. Open **FUTO Keyboard Settings**.
2. Go to **Languages & Models**.
3. Tap **Custom Layouts**.
4. Tap **Create new layout** (the `+` button at the bottom of the list).
5. A starter YAML template is created for you.  Edit the **Language** field (e.g. `en_US`) and the **Layout YAML** field.
6. Use the **Test the layout** field at the bottom to type with your new layout immediately — no need to save first.
7. Tap **Save** when you're happy.  The new layout will appear as an available keyboard for the language you specified.

To use the layout, go to **Languages & Models**, find your language, and add the custom layout from the layout picker.

> **Tip:** The editor automatically matches indentation when you press Enter, which makes editing YAML easier on a phone.

---

## Layout YAML Format

### Top-level fields

```yaml
# Required
name: My Layout          # Display name shown in the layout picker
rows:                    # List of keyboard rows (see below)
  - letters: a b c ...

# Optional
languages: en            # Language(s) this layout is intended for (space-separated tags)
description: ""          # Free-text description / credits
numberRowMode: UserConfigurable   # Whether the number row is shown
                                  # Values: UserConfigurable | AlwaysEnabled | AlwaysDisabled
autoShift: true          # Automatically capitalise the first letter of a sentence
useZWNJKey: false        # Show a Zero-Width Non-Joiner key (useful for some scripts)
```

### Rows

Each entry in `rows` is a mapping that describes a single row of keys.

```yaml
rows:
  - letters: q w e r t y u i o p   # A normal letter row
  - letters: a s d f g h j k l     # Another letter row
  - letters: z x c v b n m         # The last letter row — shift & backspace are added automatically
```

A row can also carry optional metadata:

```yaml
rows:
  - letters: a b c
    rowHeight: 1.0      # Relative height (default 1.0)
    splittable: true    # Allow the row to be split (default: true for letter rows)
```

Instead of `letters`, a row can use `numbers` (for the number row) or `bottom` (for the bottom action row).

### Keys

Within a row, keys are listed as a space-separated string for the simple case:

```yaml
letters: q w e r t y u i o p
```

Each token is an **AOSP key spec** — typically just the character the key produces (e.g. `a`, `1`, `.`).

Special characters can be entered using standard escape sequences or the `\uXXXX` Unicode notation.  For example, the Euro sign: `\u20AC`.

For more control, you can expand a row into an explicit list of key objects:

```yaml
letters:
  - a
  - b
  - keySpec: "c"
    moreKeys: "ç|!fixedColumnOrder!1"
```

#### Key types

| Type | Description |
|---|---|
| `base` (default) | A regular key — just specify `keySpec` |
| `case` | Different output in normal / shifted / caps-lock states |
| `moreKeys` | A key with a long-press popup showing additional keys |

Example of a `case` key (produces a different character when shifted):

```yaml
- !type:case
  normal: "a"
  shiftedManually: "A"
  shiftLocked: "Ä"
```

### Key attributes

Attributes can be set at the key, row, or keyboard level.  A key inherits attributes from its row, which inherits from the keyboard defaults.

```yaml
# On the keyboard (applies to all keys unless overridden)
attributes:
  style: Normal          # Visual style: Normal | Functional | Action | Spacer

# On a row
rows:
  - letters: a b c
    attributes:
      style: Functional

# On a single key
- keySpec: "@"
  attributes:
    width: FunctionalKey
    showPopup: false
```

### Key widths

The `width` attribute accepts one of the following tokens:

| Token | Meaning |
|---|---|
| `Regular` | Standard letter key width |
| `FunctionalKey` | Shift, backspace, symbols — slightly wider than Regular |
| `Grow` | Fills all remaining space in the row (used for the space bar) |
| `Custom1` … `Custom4` | User-defined widths set via `overrideWidths` at the keyboard level |

```yaml
# Define a custom width
overrideWidths:
  Custom1: 0.15   # fraction of the total keyboard width

rows:
  - letters: ...
    attributes:
      width: Custom1
```

---

## Template keys

The following special keys are inserted automatically by the layout engine.  You rarely need to add them yourself.

| Key | Inserted when |
|---|---|
| Shift | Last letter row has no explicit functional keys |
| Backspace (Delete) | Last letter row has no explicit functional keys |
| Symbols | Bottom row is auto-generated |
| Space | Bottom row is auto-generated |
| Enter / Action | Bottom row is auto-generated |
| Number row | `numberRowMode` is `AlwaysEnabled` or the user has the setting turned on |

You can disable automatic insertion of shift/backspace by providing explicit `bottom` or functional-key columns in your last letter row.

---

## Examples

### Minimal layout

A three-row alphabetic layout.  Shift, backspace, symbols, space, and enter are added automatically.

```yaml
name: Example Alphabet Layout
rows:
  - letters: a b c d e f g h i j
  - letters: k l m n o p q r s '
  - letters: t u v w x y z
```

### Layout with custom widths

```yaml
name: Wide Function Keys
overrideWidths:
  Custom1: 0.16

rows:
  - letters: q w e r t y u i o p
  - letters: a s d f g h j k l
  - letters:
      - !type:base
        keySpec: z
        attributes:
          width: Custom1
      - x
      - c
      - v
      - b
      - "n"
      - m
```

### Layout with shifted letters

Use `case` keys to define separate normal and shifted characters.

```yaml
name: Custom Accents
rows:
  - letters:
      - !type:case
        normal: "a"
        shiftedManually: "à"
      - !type:case
        normal: "e"
        shiftedManually: "é"
      - o
      - i
      - u
```

### Layout with long-press / more-keys

Add accent variants or symbols to a long-press popup using `moreKeys`.

```yaml
name: English with Accents
rows:
  - letters: q w e r t y u i o p
  - letters:
      - keySpec: "a"
        moreKeys: "à,á,â,ã,ä,å"
      - keySpec: "s"
        moreKeys: "ß,š"
      - d
      - f
      - g
      - h
      - j
      - k
      - l
  - letters: z x c v b n m
```

---

## Testing your layout

Inside the in-app editor, the **Test the layout** field at the bottom of the editor uses your current (unsaved) YAML — including any changes you have not yet saved — so you can iterate quickly without repeatedly tapping Save.

For more thorough testing you can build the app locally (see [Building](../README.md#building)) and place your YAML file under `java/assets/layouts/`.

---

## Contributing a layout

Community layouts live in a separate Apache-2.0 licensed repository (no CLA required):

> <https://github.com/futo-org/futo-keyboard-layouts>

To contribute:

1. Fork that repository.
2. Add your `my-layout.yaml` file (follow the naming conventions of existing layouts).
3. Open a pull request against the `main` branch.

The full layout specification, including every field and key type, is documented in that repository:

> <https://github.com/futo-org/futo-keyboard-layouts/blob/main/LayoutSpec.md>

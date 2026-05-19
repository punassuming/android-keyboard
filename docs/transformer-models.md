# Transformer Language Models

FUTO Keyboard uses transformer-based language models (LLMs) in the [GGUF format](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md) to provide next-word predictions and autocorrect.  Models are loaded on-device and never send data to a server.

---

## Table of Contents

1. [Overview](#overview)
2. [Using an Existing Model](#using-an-existing-model)
3. [Required GGUF Metadata](#required-gguf-metadata)
4. [Supported Features](#supported-features)
5. [Creating a New Model](#creating-a-new-model)
6. [Building the App With a Custom Default Model](#building-the-app-with-a-custom-default-model)
7. [Troubleshooting](#troubleshooting)

---

## Overview

The keyboard uses a LLaMA-compatible GGUF model file to rescore word suggestions and to provide predictions.  When the user types, the context (the text before the cursor) is fed into the model and the results are used to re-rank or generate word candidates.

Models are stored on the device at:

```
<app files dir>/transformer-models/<model-name>.gguf
```

The bundled default model (`ml4_q6_k.gguf`) is extracted from the app's raw resources on first run.

---

## Using an Existing Model

### From the app settings

1. Open **FUTO Keyboard** settings → **Predictive Text** → **Language Model Manager**.
2. Tap **Import model** and select a `.gguf` file from your device.

The keyboard validates that:
- The file starts with the GGUF magic bytes (`GGUF`).
- The file contains `keyboardlm.*` metadata (see [Required GGUF Metadata](#required-gguf-metadata)).
- All declared features are recognized by this version of the keyboard.

### Pre-built models

FUTO publishes additional language models at:

> <https://keyboard.futo.org>

A community FAQ and detailed model-author guide is maintained at:

> <https://gitlab.futo.org/alex/futo-keyboard-lm-docs/-/blob/main/README.md>

---

## Required GGUF Metadata

A valid keyboard model must include the following GGUF key-value pairs.  Keys come from two namespaces: the standard `general.*` namespace and the custom `keyboardlm.*` namespace.

### Standard GGUF fields (optional but recommended)

| Key | Type | Description |
|-----|------|-------------|
| `general.name` | `STRING` | Human-readable model name |
| `general.author` | `STRING` | Author / organization |
| `general.description` | `STRING` | Short description displayed in the UI |
| `general.license` | `STRING` | SPDX license identifier or free text |
| `general.url` | `STRING` | Model homepage or download URL |

### Keyboard-specific fields (required)

| Key | Type | Description |
|-----|------|-------------|
| `keyboardlm.languages` | `STRING` | Space-separated list of BCP-47 locale tags the model supports (e.g. `"en en_US"`) |
| `keyboardlm.features` | `STRING` | Space-separated list of feature flags (see [Supported Features](#supported-features)) |
| `keyboardlm.ext_tokenizer_type` | `STRING` | Tokenizer type; currently only `"sentencepiece"` is supported |
| `keyboardlm.ext_tokenizer_data` | `UINT8[]` | Raw bytes of the SentencePiece model file embedded in the GGUF |

### Optional keyboard-specific fields

| Key | Type | Description |
|-----|------|-------------|
| `keyboardlm.finetuning_count` | `UINT32` | Number of on-device fine-tuning runs applied (default 0) |
| `keyboardlm.history` | `STRING` | Free-text fine-tuning history for debugging |

These keys are defined in [`native/jni/src/ggml/ModelMeta.h`](../native/jni/src/ggml/ModelMeta.h).

---

## Supported Features

The `keyboardlm.features` field is a space-separated list of capability tokens.  At least one recognized feature must be present or the keyboard will reject the model.

| Feature token | Description |
|---------------|-------------|
| `base_v1` | Core next-word prediction support |
| `inverted_space` | Tokenizer produces inverted (leading) spaces instead of trailing spaces |
| `xbu_char_autocorrect_v1` | Character-level autocorrect via beam search |
| `lora_finetunable_v1` | On-device LoRA fine-tuning is supported |
| `xc0_swipe_typing_v1` | Swipe / gesture typing support |
| `char_embed_mixing_v1` | Character embedding mixing for proximity scoring |
| `experiment_linear_208_209_210` | Experimental encoder layer (linear projection of x/y coordinates) |

Tokens prefixed with `opt_` or `_` are treated as optional hints and are silently ignored when unrecognized.  Any other unrecognized token causes the import to fail with a message asking the user to update the keyboard.

---

## Creating a New Model

Creating a model from scratch requires training a LLaMA-compatible transformer, embedding a SentencePiece tokenizer, and writing the required metadata into the GGUF file.  The high-level steps are:

### 1. Train a LLaMA-architecture model

Train (or fine-tune) a LLaMA-architecture language model using your preferred framework (e.g., `transformers`, `llama.cpp`, or custom PyTorch code).  The model must be convertible to GGUF.

Recommended starting point: convert an existing small LLaMA model using the `convert-hf-to-gguf.py` script from [llama.cpp](https://github.com/ggerganov/llama.cpp).

### 2. Prepare a SentencePiece tokenizer

Train or reuse a SentencePiece tokenizer (`.model` file).  The raw bytes of this file will be embedded directly in the GGUF as the `keyboardlm.ext_tokenizer_data` array.

```python
import sentencepiece as spm
spm.SentencePieceTrainer.train(
    input='corpus.txt',
    model_prefix='tokenizer',
    vocab_size=32000,
    character_coverage=1.0,
    model_type='bpe'
)
```

### 3. Write keyboard metadata into the GGUF

Use the [`gguf`](https://github.com/ggerganov/ggml/tree/master/gguf-py) Python package to patch the metadata into your converted GGUF file:

```python
import gguf

# Open existing GGUF and add keyboard metadata
writer = gguf.GGUFWriter("model.gguf", "llama")

writer.add_string("general.name", "My Keyboard Model")
writer.add_string("general.author", "Your Name")
writer.add_string("general.description", "English keyboard language model")
writer.add_string("general.license", "Apache-2.0")

# Required keyboard fields
writer.add_string("keyboardlm.languages", "en en_US en_GB")
writer.add_string("keyboardlm.features", "base_v1 inverted_space xbu_char_autocorrect_v1")
writer.add_string("keyboardlm.ext_tokenizer_type", "sentencepiece")

# Embed the SentencePiece model bytes
with open("tokenizer.model", "rb") as f:
    tokenizer_bytes = list(f.read())
writer.add_array("keyboardlm.ext_tokenizer_data", tokenizer_bytes)

writer.add_uint32("keyboardlm.finetuning_count", 0)
writer.add_string("keyboardlm.history", "")

writer.write_header_to_file()
writer.write_kv_data_to_file()
writer.write_tensors_to_file()
writer.close()
```

> **Note:** The exact API depends on the version of the `gguf` Python package.  Refer to the upstream [llama.cpp gguf-py documentation](https://github.com/ggerganov/llama.cpp/tree/master/gguf-py) for the current API.

### 4. Quantize (optional but recommended)

Quantizing the model reduces its size and improves inference speed on mobile hardware.  Use `llama.cpp`'s `quantize` tool:

```bash
./quantize model.gguf model_q6_k.gguf Q6_K
```

`Q6_K` (6-bit k-quant) is the quantization used by the default bundled model and offers a good balance of quality and size.

### 5. Validate and import

Transfer the `.gguf` file to an Android device and import it via **Settings → Predictive Text → Language Model Manager → Import model**.

---

## Building the App With a Custom Default Model

The default model is included as an Android raw resource.  To swap it out:

1. Add your quantized model to `java/res/raw/` — for example `java/res/raw/my_model_q6_k.gguf`.
2. Edit [`java/src/org/futo/inputmethod/latin/xlm/ModelPaths.kt`](../java/src/org/futo/inputmethod/latin/xlm/ModelPaths.kt):

```kotlin
// Before
val BASE_MODEL_RESOURCE = R.raw.ml4_q6_k
val BASE_MODEL_NAME = "ml4_q6_k"

// After
val BASE_MODEL_RESOURCE = R.raw.my_model_q6_k
val BASE_MODEL_NAME = "my_model_q6_k"
```

3. Build the app:

```bash
./gradlew assembleUnstableDebug
```

---

## Troubleshooting

| Error message | Likely cause |
|---------------|-------------|
| `Failed to load models …` | File is corrupt or not a valid GGUF |
| `it lacks KeyboardLM metadata` | `keyboardlm.features` or other required keys are missing |
| `Model has the following unknown features: […]` | The feature token is not recognized; update the keyboard or remove the unsupported feature |
| `File's extension must equal 'gguf'` | Rename the file so it ends in `.gguf` |
| `Model with the name "…" already exists` | Delete the existing model before re-importing |

The source code for model validation is in [`java/src/org/futo/inputmethod/latin/xlm/ModelPaths.kt`](../java/src/org/futo/inputmethod/latin/xlm/ModelPaths.kt) and the metadata loading code is in [`native/jni/src/ggml/ModelMeta.cpp`](../native/jni/src/ggml/ModelMeta.cpp).

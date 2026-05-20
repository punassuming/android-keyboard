package org.futo.inputmethod.latin.uix.theme

import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class KeyBackground(
    val foregroundColor: Int?,
    val padding: Rect,
    val background: Drawable
)

data class KeyIcon(
    val drawable: Drawable
)

data class AdvancedThemeOptions(
    val backgroundShader: String? = null,
    val backgroundImage: ImageBitmap? = null,
    val backgroundImageVisibleArea: Rect? = null,
    val thumbnailImage: ImageBitmap? = null,
    val thumbnailScale: Float = 1.0f,
    val keyRoundness: Float = 1.0f,
    val keyBorders: Boolean? = null,
    val keyBackgrounds: KeyedBitmaps<KeyBackground>? = null,
    val keyIcons: KeyedBitmaps<KeyIcon>? = null,
    val font: Typeface? = null,
    val themeName: String? = null,
    val themeAuthor: String? = null,
    /**
     * Optional custom color for wall keys ([org.futo.inputmethod.v2keyboard.WallKey]).
     * When null, a default color derived from the keyboard surface color is used.
     */
    val wallColor: Int? = null,
)
package org.futo.inputmethod.latin.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

fun InputStream.readAllBytesCompat(): ByteArray {
    return readAllBytesCompat(Int.MAX_VALUE)
}

fun InputStream.readAllBytesCompat(maxBytes: Int): ByteArray {
    require(maxBytes > 0) { "maxBytes must be > 0" }
    val buffer = ByteArrayOutputStream()
    val data = ByteArray(4096)
    var total = 0
    var nRead: Int
    while (this.read(data, 0, data.size).also { nRead = it } != -1) {
        total += nRead
        if (total > maxBytes) {
            throw IOException("Input exceeds maximum allowed size of $maxBytes bytes")
        }
        buffer.write(data, 0, nRead)
    }
    return buffer.toByteArray()
}

inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    try {
        enumValueOf<T>(this)
    } catch (e: IllegalArgumentException) {
        null
    }

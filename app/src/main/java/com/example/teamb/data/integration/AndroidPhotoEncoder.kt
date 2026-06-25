package com.example.teamb.data.integration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reads a picked photo via the [Context]'s ContentResolver, downscales it and compresses it to a
 * JPEG, then base64-encodes it for storage in the shared community feed (Realtime Database).
 *
 * Photos are kept small on purpose: the feed is synced to every device, so the longest edge is
 * capped at [MAX_EDGE] px at [QUALITY] JPEG quality (typically ~30–80 KB). This is Android-bound and
 * therefore excluded from JVM unit-test coverage.
 */
class AndroidPhotoEncoder(context: Context) : PhotoEncoder {
    private val appContext = context.applicationContext

    override suspend fun encode(photoUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(photoUri)
            val original = appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return@runCatching null
            val scaled = downscale(original)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            if (scaled != original) scaled.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    /** Scales [bitmap] so its longest edge is at most [MAX_EDGE], preserving aspect ratio. */
    private fun downscale(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val ratio = MAX_EDGE.toFloat() / longest
        val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private companion object {
        const val MAX_EDGE = 720
        const val QUALITY = 60
    }
}

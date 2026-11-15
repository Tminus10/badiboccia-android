package swiss.reutener.boccia.net

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Turns a picked photo into what the website stores: a center-cropped square JPEG. */
object TeamPhoto {
    /**
     * Center-cropped square JPEG, at most `side` pixels wide. Decoding applies the photo's EXIF
     * orientation and converts HEIC, which the website can't read.
     */
    fun squareJpeg(resolver: ContentResolver, uri: Uri, side: Int, quality: Int = 85): ByteArray {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val crop = squareCrop(info.size.width, info.size.height, side)
            // Decoded at roughly the final size: a 48-megapixel photo would not fit in memory otherwise.
            decoder.setTargetSize(crop.scaledWidth, crop.scaledHeight)
            decoder.crop = crop.rect
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    /** The whole image scaled so its shorter side is `side` (or less, never upscaled), and the centered square in it. */
    data class Crop(val scaledWidth: Int, val scaledHeight: Int, val left: Int, val top: Int, val size: Int) {
        val rect: Rect get() = Rect(left, top, left + size, top + size)
    }

    fun squareCrop(width: Int, height: Int, side: Int): Crop {
        val shorter = minOf(width, height)
        val size = minOf(side, shorter)
        val scale = size.toDouble() / shorter
        val scaledWidth = maxOf(size, Math.round(width * scale).toInt())
        val scaledHeight = maxOf(size, Math.round(height * scale).toInt())
        return Crop(scaledWidth, scaledHeight, (scaledWidth - size) / 2, (scaledHeight - size) / 2, size)
    }
}

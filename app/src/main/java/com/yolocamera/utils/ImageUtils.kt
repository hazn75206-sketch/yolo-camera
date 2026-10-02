package com.yolocamera.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {
    fun imageProxyToBitmapUpright(proxy: ImageProxy): Bitmap? {
        val nv21 = yuv420ToNv21(proxy) ?: return null
        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, proxy.width, proxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, proxy.width, proxy.height), 90, out)
        val bytes = out.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val rotation = proxy.imageInfo.rotationDegrees
        if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    private fun yuv420ToNv21(proxy: ImageProxy): ByteArray? {
        val yPlane = proxy.planes.getOrNull(0) ?: return null
        val uPlane = proxy.planes.getOrNull(1) ?: return null
        val vPlane = proxy.planes.getOrNull(2) ?: return null
        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.buffer.get(nv21, 0, ySize)
        vPlane.buffer.get(nv21, ySize, vSize)
        uPlane.buffer.get(nv21, ySize + vSize, uSize)
        yPlane.buffer.rewind()
        uPlane.buffer.rewind()
        vPlane.buffer.rewind()
        return nv21
    }

    data class Letterbox(val bitmap: Bitmap, val scale: Float, val padX: Float, val padY: Float)

    fun letterbox(src: Bitmap, targetSize: Int): Letterbox {
        val scale = targetSize / maxOf(src.width, src.height).toFloat()
        val newW = (src.width * scale).toInt().coerceAtLeast(1)
        val newH = (src.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
        val padX = (targetSize - newW) / 2f
        val padY = (targetSize - newH) / 2f
        canvas.drawBitmap(scaled, padX, padY, null)
        if (scaled != src) scaled.recycle()
        return Letterbox(output, scale, padX, padY)
    }

    fun bitmapToChwFloat(bitmap: Bitmap): FloatArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = FloatArray(3 * w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            out[i] = ((p shr 16) and 0xFF) / 255f
            out[w * h + i] = ((p shr 8) and 0xFF) / 255f
            out[2 * w * h + i] = (p and 0xFF) / 255f
        }
        return out
    }
}

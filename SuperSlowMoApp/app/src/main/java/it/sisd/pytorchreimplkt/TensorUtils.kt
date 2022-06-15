package it.sisd.pytorchreimplkt

import android.graphics.Bitmap
import kotlin.math.roundToInt

fun tensor2Bitmap(input: FloatArray, width: Int, height: Int, normMeanRGB: FloatArray, normStdRGB: FloatArray): Bitmap? {
    val pixelsCount = height * width
    val pixels = IntArray(pixelsCount)
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val conversion = { v: Float -> ((v.coerceIn(0.0f, 1.0f))*255.0f).roundToInt()}

    val offset_g = pixelsCount
    val offset_b = 2 * pixelsCount
    for (i in 0 until pixelsCount) {
        val r = conversion(input[i] * normStdRGB[0] + normMeanRGB[0])
        val g = conversion(input[i + offset_g] * normStdRGB[1] + normMeanRGB[1])
        val b = conversion(input[i + offset_b] * normStdRGB[2] + normMeanRGB[2])
        pixels[i] = 255 shl 24 or (r.toInt() and 0xff shl 16) or (g.toInt() and 0xff shl 8) or (b.toInt() and 0xff)
    }
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

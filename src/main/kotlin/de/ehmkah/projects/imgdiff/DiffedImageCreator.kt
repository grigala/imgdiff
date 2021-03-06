package de.ehmkah.projects.imgdiff

import com.squareup.gifencoder.*
import com.squareup.gifencoder.Image
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import java.io.FileOutputStream
import java.io.OutputStream


/**
 * stolen from https://stackoverflow.com/questions/25022578/highlight-differences-between-images
 * and modified.
 *
 * @author Michael Krausse (ehmkah)
 */
class DiffedImageCreator {

    private val PIXEL_HAVE_SAME_VALUE = 16777215
    private val PIXEL_HAVE_DIFFERENT_VALUE = 13294074
    private val PIXELD_OUT_OF_BOUNDS_VALUE = 16711680

    fun getDifferenceImageWhiteAsBackground(original: BufferedImage, changed: BufferedImage): BufferedImage {
        var backgroundColor = { _: Int, _: Int -> PIXEL_HAVE_SAME_VALUE }
        return getDifferenceImage(original, changed, backgroundColor, PIXEL_HAVE_DIFFERENT_VALUE)
    }

    fun getDifferenceImageOriginalAsBackground(original: BufferedImage, changed: BufferedImage): BufferedImage {
        var backgroundColor = { x: Int, y: Int -> getPixelValueOrEmpty(original, x, y) }
        return getDifferenceImage(original, changed, backgroundColor, 16711680)
    }

    private fun getPixelValueOrEmpty(image: BufferedImage, x: Int, y: Int): Int {
        if (image.width > x && image.height > y) {
            val rgb = image.getRGB(x, y)
            val color = Color(rgb)
            val red = tint(color.red)
            val green = tint(color.green)
            val blue = tint(color.blue)
            return Color(red, green, blue).rgb
        }
        return PIXEL_HAVE_SAME_VALUE
    }

    private fun tint(color: Int): Int {
        return (color + (0.5 * (255 - color))).toInt()

    }

    private fun getDifferenceImage(original: BufferedImage, changed: BufferedImage, backgroundColor: (x: Int, y: Int) -> Int, differentValue: Int): BufferedImage {
        val originalWidth = original.width
        val changedWidth = changed.width
        val originalHeight = original.height
        val ChangedHeight = changed.height
        val targetWidth = Math.max(originalWidth, changedWidth)
        val targetHeight = Math.max(originalHeight, ChangedHeight)

        var imagesAreIdentical = true

        val result = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)

        for (currentHeight in 0 until targetHeight) {
            for (currentWidth in 0 until targetWidth) {
                var diff: Int
                var diffPixel: Int
                if (pixelOutOfBounds(currentHeight, currentWidth, original, changed)) {
                    diffPixel = PIXELD_OUT_OF_BOUNDS_VALUE
                    imagesAreIdentical = false
                } else {
                    val rgb1 = original.getRGB(currentWidth, currentHeight)
                    val rgb2 = changed.getRGB(currentWidth, currentHeight)
                    val r1 = rgb1 shr 16 and 0xff
                    val g1 = rgb1 shr 8 and 0xff
                    val b1 = rgb1 and 0xff
                    val r2 = rgb2 shr 16 and 0xff
                    val g2 = rgb2 shr 8 and 0xff
                    val b2 = rgb2 and 0xff
                    diff = Math.abs(r1 - r2)
                    diff += Math.abs(g1 - g2)
                    diff += Math.abs(b1 - b2)
                    diff /= 3

                    diffPixel = diff shl 16 or (diff shl 8) or diff
                    if (diffPixel == 0) {
                        diffPixel = backgroundColor(currentWidth, currentHeight)
                    } else {
                        diffPixel = differentValue
                        imagesAreIdentical = false
                    }
                }
                result.setRGB(currentWidth, currentHeight, diffPixel)
            }
        }
        if (imagesAreIdentical) {
            return ImageIO.read(DiffedImageCreator::class.java.getResourceAsStream("/identical.png"))
        }

        return result
    }


    private fun pixelOutOfBounds(currentHeight: Int, currentWidth: Int, img1: BufferedImage, img2: BufferedImage): Boolean {
        return currentHeight > img1.height - 1 || currentHeight > img2.height - 1 ||
                currentWidth > img1.width - 1 || currentWidth > img2.width - 1
    }

    fun createGifImage(original: BufferedImage, changed: BufferedImage): Unit {
        val diff: BufferedImage = getDifferenceImageOriginalAsBackground(original, changed);
        val width: Int = original.width
        val height: Int = original.height

        val originalRGBData: Array<IntArray> = Array(height) { IntArray(width) { 0 } }
        val diffRGBData: Array<IntArray> = Array(height) { IntArray(width) { 0 } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                originalRGBData[y][x] = original.getRGB(x, y)
                diffRGBData[y][x] = diff.getRGB(x, y)
            }
        }

        val outputStream: OutputStream = FileOutputStream("test.png")
        val options = ImageOptions()

        GifEncoder(outputStream, width, height, -1)
                .addImage(originalRGBData, options)
                .addImage(diffRGBData, options)
                .finishEncoding()
        outputStream.close()

    }

}





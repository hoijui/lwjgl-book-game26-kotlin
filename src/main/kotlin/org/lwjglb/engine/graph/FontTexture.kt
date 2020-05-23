package org.lwjglb.engine.graph

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import javax.imageio.ImageIO

class FontTexture(private val font: Font, private val charSetName: String) {
    private val charMap: MutableMap<Char, CharInfo>
    var texture: Texture? = null
        private set
    var height = 0
        private set
    var width = 0
        private set

    fun getCharInfo(c: Char): CharInfo? {
        return charMap[c]
    }

    private fun getAllAvailableChars(charsetName: String): String {
        val ce = Charset.forName(charsetName).newEncoder()
        val result = StringBuilder()
        var c = 0.toChar()
        while (c < Character.MAX_VALUE) {
            if (ce.canEncode(c)) {
                result.append(c)
            }
            c++
        }
        return result.toString()
    }

    @Throws(Exception::class)
    private fun buildTexture() {
        // Get the font metrics for each character for the selected font by using image
        var img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        var g2D = img.createGraphics()
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2D.font = font
        var fontMetrics = g2D.fontMetrics
        val allChars = getAllAvailableChars(charSetName)
        width = 0
        height = fontMetrics.height
        for (c in allChars.toCharArray()) {
            // Get the size for each character and update global image size
            val charInfo =
                CharInfo(width, fontMetrics.charWidth(c))
            charMap[c] = charInfo
            width += charInfo.width + CHAR_PADDING
        }
        g2D.dispose()

        // Create the image associated to the charset
        img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        g2D = img.createGraphics()
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2D.font = font
        fontMetrics = g2D.fontMetrics
        g2D.color = Color.WHITE
        var startX = 0
        for (c in allChars.toCharArray()) {
            val charInfo = charMap[c]
            g2D.drawString("" + c, startX, fontMetrics.ascent)
            startX += charInfo!!.width + CHAR_PADDING
        }
        g2D.dispose()
        ByteArrayOutputStream().use { out ->
            ImageIO.write(img, IMAGE_FORMAT, out)
            out.flush()
            val data = out.toByteArray()
            val buf = ByteBuffer.allocateDirect(data.size)
            buf.put(data, 0, data.size)
            buf.flip()
            texture = Texture(buf)
        }
    }

    class CharInfo(val startX: Int, val width: Int)

    companion object {
        private const val IMAGE_FORMAT = "png"
        private const val CHAR_PADDING = 2
    }

    init {
        charMap = HashMap()
        buildTexture()
    }
}
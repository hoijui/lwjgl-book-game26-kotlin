package org.lwjglb.game

import org.lwjgl.glfw.GLFW
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG
import org.lwjgl.nanovg.NanoVGGL3
import org.lwjgl.system.MemoryUtil
import org.lwjglb.engine.Utils
import org.lwjglb.engine.Window
import java.nio.ByteBuffer
import java.nio.DoubleBuffer
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class Hud {

    private var vg: Long = 0
    private var colour: NVGColor = NVGColor.create()
    private var fontBuffer: ByteBuffer? = null
    private val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss")
    private var posX: DoubleBuffer? = null
    private var posY: DoubleBuffer? = null
    private var counter = 0

    @Throws(Exception::class)
    fun init(window: Window) {
        vg =
            if (window.options.antialiasing)
                NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS or NanoVGGL3.NVG_STENCIL_STROKES)
            else
                NanoVGGL3.nvgCreate(NanoVGGL3.NVG_STENCIL_STROKES)
        if (vg == MemoryUtil.NULL) {
            throw Exception("Could not init nanovg")
        }
        fontBuffer = Utils.ioResourceToByteBuffer("/fonts/OpenSans-Bold.ttf", 150 * 1024)
        val font = NanoVG.nvgCreateFontMem(vg, FONT_NAME, fontBuffer!!, 0)
        if (font == -1) {
            throw Exception("Could not add font")
        }
        posX = MemoryUtil.memAllocDouble(1)
        posY = MemoryUtil.memAllocDouble(1)
        counter = 0
    }

    fun render(window: Window) {
        NanoVG.nvgBeginFrame(vg, window.width.toFloat(), window.height.toFloat(), 1f)

        // Upper ribbon
        NanoVG.nvgBeginPath(vg)
        NanoVG.nvgRect(vg, 0f, window.height - 100.toFloat(), window.width.toFloat(), 50f)
        NanoVG.nvgFillColor(vg, rgba(0x23, 0xa1, 0xf1, 200, colour))
        NanoVG.nvgFill(vg)

        // Lower ribbon
        NanoVG.nvgBeginPath(vg)
        NanoVG.nvgRect(vg, 0f, window.height - 50.toFloat(), window.width.toFloat(), 10f)
        NanoVG.nvgFillColor(vg, rgba(0xc1, 0xe3, 0xf9, 200, colour))
        NanoVG.nvgFill(vg)
        GLFW.glfwGetCursorPos(window.windowHandle, posX, posY)
        val xCenter = 50
        val yCenter = window.height - 75
        val radius = 20
        val x = posX!![0].toInt()
        val y = posY!![0].toInt()
        val hover = ((x - xCenter.toDouble()).pow(2.0)
                + (y - yCenter.toDouble()).pow(2.0)) < radius.toDouble().pow(2.0)

        // Circle
        NanoVG.nvgBeginPath(vg)
        NanoVG.nvgCircle(vg, xCenter.toFloat(), yCenter.toFloat(), radius.toFloat())
        NanoVG.nvgFillColor(vg, rgba(0xc1, 0xe3, 0xf9, 200, colour))
        NanoVG.nvgFill(vg)

        // Clicks Text
        NanoVG.nvgFontSize(vg, 25.0f)
        NanoVG.nvgFontFace(vg, FONT_NAME)
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_CENTER or NanoVG.NVG_ALIGN_TOP)
        if (hover) {
            NanoVG.nvgFillColor(vg, rgba(0x00, 0x00, 0x00, 255, colour))
        } else {
            NanoVG.nvgFillColor(vg, rgba(0x23, 0xa1, 0xf1, 255, colour))
        }
        NanoVG.nvgText(vg, 50f, window.height - 87.toFloat(), String.format("%02d", counter))

        // Render hour text
        NanoVG.nvgFontSize(vg, 40.0f)
        NanoVG.nvgFontFace(vg, FONT_NAME)
        NanoVG.nvgTextAlign(vg, NanoVG.NVG_ALIGN_LEFT or NanoVG.NVG_ALIGN_TOP)
        NanoVG.nvgFillColor(vg, rgba(0xe6, 0xea, 0xed, 255, colour))
        NanoVG.nvgText(
            vg,
            window.width - 150.toFloat(),
            window.height - 95.toFloat(),
            dateFormat.format(Date())
        )
        NanoVG.nvgEndFrame(vg)

        // Restore state
        window.restoreState()
    }

    fun incCounter() {
        counter++
        if (counter > 99) {
            counter = 0
        }
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int, colour: NVGColor?): NVGColor {
        colour!!.r(r / 255.0f)
        colour.g(g / 255.0f)
        colour.b(b / 255.0f)
        colour.a(a / 255.0f)
        return colour
    }

    fun cleanup() {
        NanoVGGL3.nvgDelete(vg)
        if (posX != null) {
            MemoryUtil.memFree(posX)
        }
        if (posY != null) {
            MemoryUtil.memFree(posY)
        }
    }

    companion object {
        private const val FONT_NAME = "BOLD"
    }
}
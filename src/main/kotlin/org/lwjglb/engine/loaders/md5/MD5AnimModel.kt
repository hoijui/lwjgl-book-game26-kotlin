package org.lwjglb.engine.loaders.md5

import org.lwjglb.engine.Utils
import java.io.IOException
import java.util.*

class MD5AnimModel(
        var header: MD5AnimHeader,
        var hierarchy: MD5Hierarchy,
        var boundInfo: MD5BoundInfo,
        var baseFrame: MD5BaseFrame,
        var frames: List<MD5Frame>)
{
    override fun toString(): String {

        val str = StringBuilder("MD5AnimModel: " + System.lineSeparator())
        str.append(header).append(System.lineSeparator())
        str.append(hierarchy).append(System.lineSeparator())
        str.append(boundInfo).append(System.lineSeparator())
        str.append(baseFrame).append(System.lineSeparator())
        for (frame in frames) {
            str.append(frame).append(System.lineSeparator())
        }
        return str.toString()
    }

    companion object {
        @Throws(Exception::class)
        fun parse(animFile: String): MD5AnimModel {
            val lines = Utils.readAllLines(animFile)
            if (lines.isEmpty()) {
                throw IOException("Cannot parse empty file")
            }

            // Parse Header
            var headerEnd = false
            var start = 0
            run {
                var i = 0
                while (i < lines.size && !headerEnd) {
                    val line = lines[i]
                    headerEnd = line.trim { it <= ' ' }.endsWith("{")
                    start = i
                    i++
                }
            }
            if (!headerEnd) {
                throw Exception("Cannot find header")
            }
            val headerBlock: List<String?> = lines.subList(0, start)
            val header: MD5AnimHeader = MD5AnimHeader.parse(headerBlock)

            // Parse the rest of block
            var blockStart = 0
            var inBlock = false
            var blockId = ""
            var hierarchy: MD5Hierarchy? = null
            var boundInfo: MD5BoundInfo? = null
            var baseFrame: MD5BaseFrame? = null
            val frames: MutableList<MD5Frame> = ArrayList()
            for (i in start until lines.size) {
                val line = lines[i]
                if (line.endsWith("{")) {
                    blockStart = i
                    blockId = line.substring(0, line.lastIndexOf(" "))
                    inBlock = true
                } else if (inBlock && line.endsWith("}")) {
                    val blockBody: List<String> = lines.subList(blockStart + 1, i)
                    when (blockId) {
                        "hierarchy" -> {
                            hierarchy = MD5Hierarchy.parse(blockBody)
                        }
                        "bounds" -> {
                            boundInfo = MD5BoundInfo.parse(blockBody)
                        }
                        "baseframe" -> {
                            baseFrame = MD5BaseFrame.parse(blockBody)
                        }
                        else -> if (blockId.startsWith("frame ")) {
                            val frame: MD5Frame = MD5Frame.parse(blockId, blockBody)
                            frames.add(frame)
                        }
                    }
                    inBlock = false
                }
            }
            if (hierarchy == null) {
                throw IllegalStateException("No 'hierarchy' block present")
            }
            if (boundInfo == null) {
                throw IllegalStateException("No 'bounds' block present")
            }
            if (baseFrame == null) {
                throw IllegalStateException("No 'baseframe' block present")
            }
            return MD5AnimModel(header, hierarchy, boundInfo, baseFrame, frames)
        }
    }
}
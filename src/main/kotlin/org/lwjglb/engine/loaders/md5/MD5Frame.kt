package org.lwjglb.engine.loaders.md5

import org.lwjglb.engine.Utils
import java.util.*
import java.util.regex.Pattern

class MD5Frame(val frameData: FloatArray, private var id: Int = 0) {

    override fun toString(): String {
        val str = StringBuilder("frame " + id + " [data: " + System.lineSeparator())
        for (frameData in frameData) {
            str.append(frameData).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    companion object {
        private val spaceRegex : Pattern = Pattern.compile("\\s+")

        @Throws(Exception::class)
        fun parse(blockId: String, blockBody: List<String>): MD5Frame {

            val tokens = blockId.trim { it <= ' ' }.split(spaceRegex).toTypedArray()
            val id: Int
            if (tokens.size >= 2) {
                id = tokens[1].toInt()
            } else {
                throw Exception("Wrong frame definition: $blockId")
            }
            val data: MutableList<Float> = ArrayList()
            for (line in blockBody) {
                val lineData = parseLine(line)
                data.addAll(lineData)
            }
            val frameData = Utils.listToArray(data)
            return MD5Frame(frameData, id)
        }

        private fun parseLine(line: String?): List<Float> {
            val tokens = line!!.trim { it <= ' ' }.split(spaceRegex).toTypedArray()
            val data: MutableList<Float> = ArrayList()
            for (token in tokens) {
                data.add(token.toFloat())
            }
            return data
        }
    }
}

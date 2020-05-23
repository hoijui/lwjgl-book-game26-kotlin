package org.lwjglb.engine.loaders.md5

import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.regex.Pattern

class MD5BaseFrame(val frameDataList: List<MD5BaseFrameData>) {

    override fun toString(): String {
        val str = StringBuilder("base frame [" + System.lineSeparator())
        for (frameData in frameDataList) {
            str.append(frameData).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    class MD5BaseFrameData {
        var position: Vector3f? = null
        var orientation: Quaternionf? = null
            private set

        fun setOrientation(vec: Vector3f) {
            orientation = MD5Utils.calculateQuaternion(vec)
        }

        override fun toString(): String {
            return "[position: $position, orientation: $orientation]"
        }

        companion object {
            private val PATTERN_BASEFRAME =
                Pattern.compile("\\s*" + MD5Utils.VECTOR3_REGEXP + "\\s*" + MD5Utils.VECTOR3_REGEXP + ".*")

            fun parseLine(line: String): MD5BaseFrameData? {
                val matcher = PATTERN_BASEFRAME.matcher(line)
                var result: MD5BaseFrameData? = null
                if (matcher.matches()) {
                    result = MD5BaseFrameData()
                    var x = matcher.group(1).toFloat()
                    var y = matcher.group(2).toFloat()
                    var z = matcher.group(3).toFloat()
                    result.position = Vector3f(x, y, z)
                    x = matcher.group(4).toFloat()
                    y = matcher.group(5).toFloat()
                    z = matcher.group(6).toFloat()
                    result.setOrientation(Vector3f(x, y, z))
                }
                return result
            }
        }
    }

    companion object {
        fun parse(blockBody: List<String>): MD5BaseFrame {
            val frameInfoList: MutableList<MD5BaseFrameData> = ArrayList()
            for (line in blockBody) {
                val frameInfo = MD5BaseFrameData.parseLine(line)
                if (frameInfo != null) {
                    frameInfoList.add(frameInfo)
                }
            }
            return MD5BaseFrame(frameInfoList)
        }
    }
}
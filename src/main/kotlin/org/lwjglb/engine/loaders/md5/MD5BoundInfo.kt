package org.lwjglb.engine.loaders.md5

import org.joml.Vector3f
import java.util.*
import java.util.regex.Pattern

class MD5BoundInfo {
    var bounds: List<MD5Bound>? = null

    override fun toString(): String {
        val str = StringBuilder("bounds [" + System.lineSeparator())
        for (bound in bounds!!) {
            str.append(bound).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    class MD5Bound {
        var minBound: Vector3f? = null
        var maxBound: Vector3f? = null

        override fun toString(): String {
            return "[minBound: $minBound, maxBound: $maxBound]"
        }

        companion object {
            private val PATTERN_BOUND =
                Pattern.compile("\\s*" + MD5Utils.VECTOR3_REGEXP + "\\s*" + MD5Utils.VECTOR3_REGEXP + ".*")

            fun parseLine(line: String?): MD5Bound? {
                var result: MD5Bound? = null
                val matcher = PATTERN_BOUND.matcher(line)
                if (matcher.matches()) {
                    result = MD5Bound()
                    var x = matcher.group(1).toFloat()
                    var y = matcher.group(2).toFloat()
                    var z = matcher.group(3).toFloat()
                    result.minBound = Vector3f(x, y, z)
                    x = matcher.group(4).toFloat()
                    y = matcher.group(5).toFloat()
                    z = matcher.group(6).toFloat()
                    result.maxBound = Vector3f(x, y, z)
                }
                return result
            }
        }
    }

    companion object {
        fun parse(blockBody: List<String?>): MD5BoundInfo {
            val result = MD5BoundInfo()
            val bounds: MutableList<MD5Bound> = ArrayList()
            for (line in blockBody) {
                val bound = MD5Bound.parseLine(line)
                if (bound != null) {
                    bounds.add(bound)
                }
            }
            result.bounds = bounds
            return result
        }
    }
}
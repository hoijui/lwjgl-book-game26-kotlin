package org.lwjglb.engine.loaders.md5

class MD5AnimHeader {
    var version: String? = null
    var commandLine: String? = null
    var numFrames = 0
    var numJoints = 0
    var frameRate = 0
    var numAnimatedComponents = 0

    override fun toString(): String {
        return "animHeader: [version: " + version + ", commandLine: " + commandLine +
                ", numFrames: " + numFrames + ", numJoints: " + numJoints +
                ", frameRate: " + frameRate + ", numAnimatedComponents:" + numAnimatedComponents + "]"
    }

    companion object {
        @Throws(Exception::class)
        fun parse(lines: List<String?>?): MD5AnimHeader {
            val header = MD5AnimHeader()
            val numLines = lines?.size ?: 0
            if (numLines == 0) {
                throw Exception("Cannot parse empty file")
            }
            var finishHeader = false
            var i = 0
            while (i < numLines && !finishHeader) {
                val line = lines!![i]
                val tokens = line!!.split("\\s+").toTypedArray()
                if (tokens.size > 1) {
                    val paramName = tokens[0]
                    val paramValue = tokens[1]
                    when (paramName) {
                        "MD5Version" -> header.version = paramValue
                        "commandline" -> header.commandLine = paramValue
                        "numFrames" -> header.numFrames = paramValue.toInt()
                        "numJoints" -> header.numJoints = paramValue.toInt()
                        "frameRate" -> header.frameRate = paramValue.toInt()
                        "numAnimatedComponents" -> header.numAnimatedComponents = paramValue.toInt()
                        "hierarchy" -> finishHeader = true
                    }
                }
                i++
            }
            return header
        }
    }
}

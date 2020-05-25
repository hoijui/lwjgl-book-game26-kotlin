package org.lwjglb.engine.loaders.md5

class MD5ModelHeader {
    var version: String? = null
    var commandLine: String? = null
    var numJoints = 0
    var numMeshes = 0

    override fun toString(): String {
        return "[version: " + version + ", commandLine: " + commandLine +
                ", numJoints: " + numJoints + ", numMeshes: " + numMeshes + "]"
    }

    companion object {
        @Throws(Exception::class)
        fun parse(lines: List<String?>?): MD5ModelHeader {
            val header = MD5ModelHeader()
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
                        "numJoints" -> header.numJoints = paramValue.toInt()
                        "numMeshes" -> header.numMeshes = paramValue.toInt()
                        "joints" -> finishHeader = true
                    }
                }
                i++
            }
            return header
        }
    }
}

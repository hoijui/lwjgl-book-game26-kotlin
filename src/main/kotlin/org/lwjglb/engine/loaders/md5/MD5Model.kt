package org.lwjglb.engine.loaders.md5

import kotlinx.io.IOException
import org.lwjglb.engine.Utils
import java.util.*

class MD5Model(
        val header: MD5ModelHeader,
        val jointInfo: MD5JointInfo,
        val meshes: MutableList<MD5Mesh>)
{

    override fun toString(): String {
        val str = StringBuilder("MD5MeshModel: " + System.lineSeparator())
        str.append(header).append(System.lineSeparator())
        str.append(jointInfo).append(System.lineSeparator())
        for (mesh in meshes) {
            str.append(mesh).append(System.lineSeparator())
        }
        return str.toString()
    }

    companion object {
        @Throws(Exception::class)
        fun parse(meshModelFile: String): MD5Model {
            val lines = Utils.readAllLines(meshModelFile)
            if (lines.isEmpty()) {
                throw Exception("Cannot parse empty file")
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
            val headerBlock: List<String> = lines.subList(0, start)
            val header: MD5ModelHeader = MD5ModelHeader.parse(headerBlock)

            // Parse the rest of block
            var blockStart = 0
            var inBlock = false
            var blockId = ""
            var jointInfo: MD5JointInfo? = null
            val meshes: MutableList<MD5Mesh> = ArrayList()
            for (i in start until lines.size) {
                val line = lines[i]
                if (line.endsWith("{")) {
                    blockStart = i
                    blockId = line.substring(0, line.lastIndexOf(" "))
                    inBlock = true
                } else if (inBlock && line.endsWith("}")) {
                    val blockBody: List<String> = lines.subList(blockStart + 1, i)
                    when (blockId) {
                        "joints" -> {
                            jointInfo = MD5JointInfo.parse(blockBody)
                        }
                        "mesh" -> {
                            val md5Mesh: MD5Mesh = MD5Mesh.parse(blockBody)
                            meshes.add(md5Mesh)
                        }
                        else -> {
                        }
                    }
                    inBlock = false
                }
            }
            if (jointInfo == null) {
                throw IOException("No 'jointInfo' block found")
            }
            return MD5Model(header, jointInfo, meshes)
        }
    }
}
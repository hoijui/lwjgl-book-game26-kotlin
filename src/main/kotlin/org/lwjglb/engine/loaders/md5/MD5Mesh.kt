package org.lwjglb.engine.loaders.md5

import org.joml.Vector2f
import org.joml.Vector3f
import java.util.*
import java.util.regex.Pattern

class MD5Mesh {
    var texture: String? = null
    var vertices: MutableList<MD5Vertex>
    var triangles: MutableList<MD5Triangle>
    var weights: MutableList<MD5Weight>
    override fun toString(): String {
        val str = StringBuilder("mesh [" + System.lineSeparator())
        str.append("texture: ").append(texture).append(System.lineSeparator())
        str.append("vertices [").append(System.lineSeparator())
        for (vertex in vertices) {
            str.append(vertex).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        str.append("triangles [").append(System.lineSeparator())
        for (triangle in triangles) {
            str.append(triangle).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        str.append("weights [").append(System.lineSeparator())
        for (weight in weights) {
            str.append(weight).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    class MD5Vertex(
            val index: Int = 0,
            val textCoordinates: Vector2f,
            val startWeight: Int = 0,
            val weightCount: Int = 0)
    {
        override fun toString(): String {
            return ("[index: " + index + ", textCoordinates: " + textCoordinates
                    + ", startWeight: " + startWeight + ", weightCount: " + weightCount + "]")
        }
    }

    class MD5Triangle {
        var index = 0
        var vertex0 = 0
        var vertex1 = 0
        var vertex2 = 0

        override fun toString(): String {
            return ("[index: " + index + ", vertex0: " + vertex0
                    + ", vertex1: " + vertex1 + ", vertex2: " + vertex2 + "]")
        }
    }

    class MD5Weight {
        var index = 0
        var jointIndex = 0
        var bias = 0f
        var position: Vector3f? = null

        override fun toString(): String {
            return ("[index: " + index + ", jointIndex: " + jointIndex
                    + ", bias: " + bias + ", position: " + position + "]")
        }
    }

    companion object {
        private val PATTERN_SHADER =
            Pattern.compile("\\s*shader\\s*\\\"([^\\\"]+)\\\"")
        private val PATTERN_VERTEX = Pattern.compile(
            "\\s*vert\\s*(\\d+)\\s*\\(\\s*("
                    + MD5Utils.FLOAT_REGEXP + ")\\s*(" + MD5Utils.FLOAT_REGEXP + ")\\s*\\)\\s*(\\d+)\\s*(\\d+)"
        )
        private val PATTERN_TRI =
            Pattern.compile("\\s*tri\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)\\s*(\\d+)")
        private val PATTERN_WEIGHT = Pattern.compile(
            "\\s*weight\\s*(\\d+)\\s*(\\d+)\\s*" +
                    "(" + MD5Utils.FLOAT_REGEXP + ")\\s*" + MD5Utils.VECTOR3_REGEXP
        )

        fun parse(meshBlock: List<String?>): MD5Mesh {
            val mesh = MD5Mesh()
            val vertices = mesh.vertices
            val triangles = mesh.triangles
            val weights = mesh.weights
            for (line in meshBlock) {
                if (line!!.contains("shader")) {
                    val textureMatcher = PATTERN_SHADER.matcher(line)
                    if (textureMatcher.matches()) {
                        mesh.texture = textureMatcher.group(1)
                    }
                } else if (line.contains("vert")) {
                    val vertexMatcher = PATTERN_VERTEX.matcher(line)
                    if (vertexMatcher.matches()) {
                        val index = vertexMatcher.group(1).toInt()
                        val x = vertexMatcher.group(2).toFloat()
                        val y = vertexMatcher.group(3).toFloat()
                        val textCoordinates = Vector2f(x, y)
                        val startWeight = vertexMatcher.group(4).toInt()
                        val weightCount = vertexMatcher.group(5).toInt()
                        val vertex = MD5Vertex(index, textCoordinates, startWeight, weightCount)
                        vertices.add(vertex)
                    }
                } else if (line.contains("tri")) {
                    val triMatcher = PATTERN_TRI.matcher(line)
                    if (triMatcher.matches()) {
                        val triangle = MD5Triangle()
                        triangle.index = triMatcher.group(1).toInt()
                        triangle.vertex0 = triMatcher.group(2).toInt()
                        triangle.vertex1 = triMatcher.group(3).toInt()
                        triangle.vertex2 = triMatcher.group(4).toInt()
                        triangles.add(triangle)
                    }
                } else if (line.contains("weight")) {
                    val weightMatcher = PATTERN_WEIGHT.matcher(line)
                    if (weightMatcher.matches()) {
                        val weight = MD5Weight()
                        weight.index = weightMatcher.group(1).toInt()
                        weight.jointIndex = weightMatcher.group(2).toInt()
                        weight.bias = weightMatcher.group(3).toFloat()
                        val x = weightMatcher.group(4).toFloat()
                        val y = weightMatcher.group(5).toFloat()
                        val z = weightMatcher.group(6).toFloat()
                        weight.position = Vector3f(x, y, z)
                        weights.add(weight)
                    }
                }
            }
            return mesh
        }
    }

    init {
        vertices = ArrayList()
        triangles = ArrayList()
        weights = ArrayList()
    }
}
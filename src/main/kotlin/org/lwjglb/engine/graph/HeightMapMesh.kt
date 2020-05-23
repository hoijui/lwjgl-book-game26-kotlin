package org.lwjglb.engine.graph

import org.joml.Vector3f
import org.lwjglb.engine.Utils
import java.nio.ByteBuffer
import java.util.*

class HeightMapMesh(
    private val minY: Float,
    private val maxY: Float,
    heightMapImage: ByteBuffer?,
    width: Int,
    height: Int,
    textureFile: String,
    textInc: Int
) {
    val mesh: Mesh
    private val heightArray: Array<FloatArray> = Array(height) { FloatArray(width) }

    fun getHeight(row: Int, col: Int): Float {
        var result = 0f
        if (row >= 0 && row < heightArray.size) {
            if (col >= 0 && col < heightArray[row].size) {
                result = heightArray[row][col]
            }
        }
        return result
    }

    private fun calcNormals(posArr: FloatArray?, width: Int, height: Int): FloatArray? {
        val v0 = Vector3f()
        var v1 = Vector3f()
        var v2 = Vector3f()
        var v3 = Vector3f()
        var v4 = Vector3f()
        val v12 = Vector3f()
        val v23 = Vector3f()
        val v34 = Vector3f()
        val v41 = Vector3f()
        val normals: MutableList<Float> = ArrayList()
        var normal = Vector3f()
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (row > 0 && row < height - 1 && col > 0 && col < width - 1) {
                    val i0 = row * width * 3 + col * 3
                    v0.x = posArr!![i0]
                    v0.y = posArr[i0 + 1]
                    v0.z = posArr[i0 + 2]
                    val i1 = row * width * 3 + (col - 1) * 3
                    v1.x = posArr[i1]
                    v1.y = posArr[i1 + 1]
                    v1.z = posArr[i1 + 2]
                    v1 = v1.sub(v0)
                    val i2 = (row + 1) * width * 3 + col * 3
                    v2.x = posArr[i2]
                    v2.y = posArr[i2 + 1]
                    v2.z = posArr[i2 + 2]
                    v2 = v2.sub(v0)
                    val i3 = row * width * 3 + (col + 1) * 3
                    v3.x = posArr[i3]
                    v3.y = posArr[i3 + 1]
                    v3.z = posArr[i3 + 2]
                    v3 = v3.sub(v0)
                    val i4 = (row - 1) * width * 3 + col * 3
                    v4.x = posArr[i4]
                    v4.y = posArr[i4 + 1]
                    v4.z = posArr[i4 + 2]
                    v4 = v4.sub(v0)
                    v1.cross(v2, v12)
                    v12.normalize()
                    v2.cross(v3, v23)
                    v23.normalize()
                    v3.cross(v4, v34)
                    v34.normalize()
                    v4.cross(v1, v41)
                    v41.normalize()
                    normal = v12.add(v23).add(v34).add(v41)
                    normal.normalize()
                } else {
                    normal.x = 0f
                    normal.y = 1f
                    normal.z = 0f
                }
                normal.normalize()
                normals.add(normal.x)
                normals.add(normal.y)
                normals.add(normal.z)
            }
        }
        return Utils.listToArray(normals)
    }

    private fun getHeight(x: Int, z: Int, width: Int, buffer: ByteBuffer?): Float {
        val argb = getRGB(x, z, width, buffer)
        return minY + Math.abs(maxY - minY) * (argb.toFloat() / MAX_COLOUR.toFloat())
    }

    companion object {
        private const val MAX_COLOUR = 255 * 255 * 255
        const val START_X = -0.5f
        const val START_Z = -0.5f
        val xLength: Float
            get() = Math.abs(-START_X * 2)

        val zLength: Float
            get() = Math.abs(-START_Z * 2)

        fun getRGB(x: Int, z: Int, width: Int, buffer: ByteBuffer?): Int {
            val r = buffer!![x * 4 + 0 + z * 4 * width]
            val g = buffer[x * 4 + 1 + z * 4 * width]
            val b = buffer[x * 4 + 2 + z * 4 * width]
            val a = buffer[x * 4 + 3 + z * 4 * width]
            return (0xFF and a.toInt() shl 24 or (0xFF and r.toInt() shl 16)
                    or (0xFF and g.toInt() shl 8) or (0xFF and b.toInt()))
        }
    }

    init {
        val texture = Texture(textureFile)
        val incX = xLength / (width - 1)
        val incZ = zLength / (height - 1)
        val positions: MutableList<Float> = ArrayList()
        val textCoordinates: MutableList<Float> = ArrayList()
        val indices: MutableList<Int> = ArrayList()
        for (row in 0 until height) {
            for (col in 0 until width) {
                // Create vertex for current position
                positions.add(START_X + col * incX) // x
                val currentHeight = getHeight(col, row, width, heightMapImage)
                heightArray[row][col] = currentHeight
                positions.add(currentHeight) //y
                positions.add(START_Z + row * incZ) //z

                // Set texture coordinates
                textCoordinates.add(textInc.toFloat() * col.toFloat() / width.toFloat())
                textCoordinates.add(textInc.toFloat() * row.toFloat() / height.toFloat())

                // Create indices
                if (col < width - 1 && row < height - 1) {
                    val leftTop = row * width + col
                    val leftBottom = (row + 1) * width + col
                    val rightBottom = (row + 1) * width + col + 1
                    val rightTop = row * width + col + 1
                    indices.add(rightTop)
                    indices.add(leftBottom)
                    indices.add(rightBottom)
                    indices.add(leftTop)
                    indices.add(leftBottom)
                    indices.add(rightTop)
                }
            }
        }
        val posArr = Utils.listToArray(positions)
        val indicesArr = indices.stream().mapToInt { i: Int? -> i!! }.toArray()
        val textCoordinatesArr = Utils.listToArray(textCoordinates)
        val normalsArr = calcNormals(posArr, width, height)
        val material = Material(texture, 0.0f)
        mesh = Mesh(posArr, textCoordinatesArr, normalsArr, indicesArr, material=material)
    }
}
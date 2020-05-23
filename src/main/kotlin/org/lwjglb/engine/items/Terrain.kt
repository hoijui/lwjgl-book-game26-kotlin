package org.lwjglb.engine.items

import org.joml.Vector3f
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjglb.engine.graph.HeightMapMesh
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * A Terrain is composed by blocks, each block is a GameItem constructed
 * from a HeightMap.
 *
 * @param terrainSize The number of blocks will be terrainSize * terrainSize
 * @param scale The scale to be applied to each terrain block
 * @param minY The minimum y value, before scaling, of each terrain block
 * @param maxY The maximum y value, before scaling, of each terrain block
 * @param heightMapFile
 * @param textureFile
 * @param textInc
 * @throws Exception
 */
class Terrain(
    private val terrainSize: Int,
    scale: Float,
    minY: Float,
    maxY: Float,
    heightMapFile: String,
    textureFile: String,
    textInc: Int
) {
    val gameItems: MutableList<GameItem> = ArrayList()
    private val verticesPerCol: Int
    private val verticesPerRow: Int
    private val heightMapMesh: HeightMapMesh

    /**
     * It will hold the bounding box for each terrain block
     */
    private val boundingBoxes: Array<Array<Box2D?>>
    fun getHeight(position: Vector3f): Float {
        var result = Float.MIN_VALUE
        // For each terrain block we get the bounding box, translate it to view coordinates
        // and check if the position is contained in that bounding box
        var boundingBox: Box2D? = null
        var found = false
        var terrainBlock: GameItem? = null
        var row = 0
        while (row < terrainSize && !found) {
            var col = 0
            while (col < terrainSize && !found) {
                terrainBlock = gameItems[row * terrainSize + col]
                boundingBox = boundingBoxes[row][col]
                found = boundingBox!!.contains(position.x, position.z)
                col++
            }
            row++
        }

        // If we have found a terrain block that contains the position we need
        // to calculate the height of the terrain on that position
        if (found) {
            val triangle = getTriangle(position, boundingBox, terrainBlock)
            result = interpolateHeight(triangle[0], triangle[1], triangle[2], position.x, position.z)
        }
        return result
    }

    protected fun getTriangle(
        position: Vector3f,
        boundingBox: Box2D?,
        terrainBlock: GameItem?
    ): Array<Vector3f?> {
        // Get the column and row of the heightmap associated to the current position
        val cellWidth = boundingBox!!.width / verticesPerCol.toFloat()
        val cellHeight = boundingBox.height / verticesPerRow.toFloat()
        val col = ((position.x - boundingBox.x) / cellWidth).toInt()
        val row = ((position.z - boundingBox.y) / cellHeight).toInt()
        val triangle = arrayOfNulls<Vector3f>(3)
        triangle[1] = Vector3f(
            boundingBox.x + col * cellWidth,
            getWorldHeight(row + 1, col, terrainBlock),
            boundingBox.y + (row + 1) * cellHeight
        )
        triangle[2] = Vector3f(
            boundingBox.x + (col + 1) * cellWidth,
            getWorldHeight(row, col + 1, terrainBlock),
            boundingBox.y + row * cellHeight
        )
        if (position.z < getDiagonalZCoord(
                triangle[1]!!.x,
                triangle[1]!!.z,
                triangle[2]!!.x,
                triangle[2]!!.z,
                position.x
            )
        ) {
            triangle[0] = Vector3f(
                boundingBox.x + col * cellWidth,
                getWorldHeight(row, col, terrainBlock),
                boundingBox.y + row * cellHeight
            )
        } else {
            triangle[0] = Vector3f(
                boundingBox.x + (col + 1) * cellWidth,
                getWorldHeight(row + 2, col + 1, terrainBlock),
                boundingBox.y + (row + 1) * cellHeight
            )
        }
        return triangle
    }

    protected fun getDiagonalZCoord(
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        x: Float
    ): Float {
        return (z1 - z2) / (x1 - x2) * (x - x1) + z1
    }

    protected fun getWorldHeight(row: Int, col: Int, gameItem: GameItem?): Float {
        val y = heightMapMesh.getHeight(row, col)
        return y * gameItem!!.scale + gameItem.position.y
    }

    protected fun interpolateHeight(
        pA: Vector3f?,
        pB: Vector3f?,
        pC: Vector3f?,
        x: Float,
        z: Float
    ): Float {
        // Plane equation ax+by+cz+d=0
        val a = (pB!!.y - pA!!.y) * (pC!!.z - pA.z) - (pC.y - pA.y) * (pB.z - pA.z)
        val b = (pB.z - pA.z) * (pC.x - pA.x) - (pC.z - pA.z) * (pB.x - pA.x)
        val c = (pB.x - pA.x) * (pC.y - pA.y) - (pC.x - pA.x) * (pB.y - pA.y)
        val d = -(a * pA.x + b * pA.y + c * pA.z)
        // y = (-d -ax -cz) / b
        return (-d - a * x - c * z) / b
    }

    /**
     * Gets the bounding box of a terrain block
     *
     * @param terrainBlock A GameItem instance that defines the terrain block
     * @return The boundingg box of the terrain block
     */
    private fun getBoundingBox(terrainBlock: GameItem): Box2D {
        val scale = terrainBlock.scale
        val position = terrainBlock.position
        val topLeftX: Float = HeightMapMesh.START_X * scale + position.x
        val topLeftZ: Float = HeightMapMesh.START_Z * scale + position.z
        val width: Float = abs(HeightMapMesh.START_X * 2) * scale
        val height: Float = abs(HeightMapMesh.START_Z * 2) * scale
        return Box2D(topLeftX, topLeftZ, width, height)
    }

    class Box2D(var x: Float, var y: Float, var width: Float, var height: Float) {
        fun contains(x2: Float, y2: Float): Boolean {
            return x2 >= x && y2 >= y && x2 < x + width && y2 < y + height
        }

    }

    init {
//        gameItems = arrayOfNulls(terrainSize * terrainSize)
        var buf: ByteBuffer? = null
        var width: Int = -1
        var height: Int = -1
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            buf = STBImage.stbi_load(heightMapFile, w, h, channels, 4)
            if (buf == null) {
                throw Exception("Image file [" + heightMapFile + "] not loaded: " + STBImage.stbi_failure_reason())
            }
            width = w.get()
            height = h.get()
        }

        // The number of vertices per column and row
        verticesPerCol = width - 1
        verticesPerRow = height - 1
        heightMapMesh = HeightMapMesh(minY, maxY, buf, width, height, textureFile, textInc)
        boundingBoxes = Array(terrainSize) { arrayOfNulls<Box2D?>(terrainSize) }
        for (row in 0 until terrainSize) {
            for (col in 0 until terrainSize) {
                val xDisplacement: Float =
                    (col - (terrainSize.toFloat() - 1) / 2.toFloat()) * scale * HeightMapMesh.xLength
                val zDisplacement: Float =
                    (row - (terrainSize.toFloat() - 1) / 2.toFloat()) * scale * HeightMapMesh.zLength
                val terrainBlock = GameItem(heightMapMesh.mesh)
                terrainBlock.scale = scale
                terrainBlock.setPosition(xDisplacement, 0f, zDisplacement)
                gameItems[row * terrainSize + col] = terrainBlock
                boundingBoxes[row][col] = getBoundingBox(terrainBlock)
            }
        }
        STBImage.stbi_image_free(buf!!)
    }
}
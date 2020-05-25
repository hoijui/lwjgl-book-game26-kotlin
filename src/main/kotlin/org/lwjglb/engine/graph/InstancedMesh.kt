package org.lwjglb.engine.graph

import org.joml.Matrix4f
import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryUtil
import org.lwjglb.engine.items.GameItem
import java.nio.FloatBuffer
import kotlin.math.min

class InstancedMesh(
    positions: FloatArray,
    textCoordinates: FloatArray?,
    normals: FloatArray?,
    indices: IntArray?,
    private val numInstances: Int,
    material: Material)
    : Mesh(
    positions,
    textCoordinates,
    normals,
    indices,
    createEmptyIntArray(
        MAX_WEIGHTS * positions.size / 3,
        0
    ),
    createEmptyFloatArray(
        MAX_WEIGHTS * positions.size / 3,
        0.0f
    ),
    material)
{
    private val instanceDataVBO: Int
    private var instanceDataBuffer: FloatBuffer?

    override fun cleanUp() {
        super.cleanUp()
        if (instanceDataBuffer != null) {
            MemoryUtil.memFree(instanceDataBuffer)
            instanceDataBuffer = null
        }
    }

    fun renderListInstanced(
        gameItems: List<GameItem>,
        transformation: Transformation,
        viewMatrix: Matrix4f?)
    {
        renderListInstanced(gameItems, false, transformation, viewMatrix)
    }

    fun renderListInstanced(
        gameItems: List<GameItem>,
        billBoard: Boolean,
        transformation: Transformation,
        viewMatrix: Matrix4f?)
    {
        initRender()
        val chunkSize = numInstances
        val length = gameItems.size
        var i = 0
        while (i < length) {
            val end = min(length, i + chunkSize)
            val subList = gameItems.subList(i, end)
            renderChunkInstanced(subList, billBoard, transformation, viewMatrix)
            i += chunkSize
        }
        endRender()
    }

    private fun renderChunkInstanced(
        gameItems: List<GameItem>,
        billBoard: Boolean,
        transformation: Transformation,
        viewMatrix: Matrix4f?)
    {
        instanceDataBuffer!!.clear()
        val text = material.texture
        for ((i, gameItem) in gameItems.withIndex()) {
            val modelMatrix = transformation.buildModelMatrix(gameItem)
            if (billBoard && viewMatrix != null) {
                viewMatrix.transpose3x3(modelMatrix)
            }
            modelMatrix[INSTANCE_SIZE_FLOATS * i, instanceDataBuffer]
            if (text != null) {
                val col = gameItem.textPos % text.numCols
                val row = gameItem.textPos / text.numCols
                val textXOffset = col.toFloat() / text.numCols
                val textYOffset = row.toFloat() / text.numRows
                val buffPos =
                    INSTANCE_SIZE_FLOATS * i + MATRIX_SIZE_FLOATS
                instanceDataBuffer!!.put(buffPos, textXOffset)
                instanceDataBuffer!!.put(buffPos + 1, textYOffset)
            }

            // Selected data or scaling for billboard
            val buffPos =
                INSTANCE_SIZE_FLOATS * i + MATRIX_SIZE_FLOATS + 2
            instanceDataBuffer!!.put(
                buffPos,
                if (billBoard) gameItem.scale else if (gameItem.isSelected) 1.0f else 0.0f
            )
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceDataVBO)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceDataBuffer!!, GL15.GL_DYNAMIC_READ)
        GL31.glDrawElementsInstanced(
            GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0, gameItems.size
        )
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    }

    companion object {
        private const val FLOAT_SIZE_BYTES = 4
        private const val VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES
        private const val MATRIX_SIZE_FLOATS = 4 * 4
        private const val MATRIX_SIZE_BYTES =
            MATRIX_SIZE_FLOATS * FLOAT_SIZE_BYTES
        private const val INSTANCE_SIZE_BYTES =
            MATRIX_SIZE_BYTES + FLOAT_SIZE_BYTES * 2 + FLOAT_SIZE_BYTES
        private const val INSTANCE_SIZE_FLOATS = MATRIX_SIZE_FLOATS + 3
    }

    init {
        GL30.glBindVertexArray(vaoId)
        instanceDataVBO = GL15.glGenBuffers()
        vboIdList.add(instanceDataVBO)
        instanceDataBuffer = MemoryUtil.memAllocFloat(numInstances * INSTANCE_SIZE_FLOATS)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceDataVBO)
        var start = 5
        var strideStart = 0
        // Model matrix
        for (i in 0..3) {
            GL20.glVertexAttribPointer(
                start,
                4,
                GL11.GL_FLOAT,
                false,
                INSTANCE_SIZE_BYTES,
                strideStart.toLong()
            )
            GL33.glVertexAttribDivisor(start, 1)
            GL20.glEnableVertexAttribArray(start)
            start++
            strideStart += VECTOR4F_SIZE_BYTES
        }

        // Texture offsets
        GL20.glVertexAttribPointer(
            start,
            2,
            GL11.GL_FLOAT,
            false,
            INSTANCE_SIZE_BYTES,
            strideStart.toLong()
        )
        GL33.glVertexAttribDivisor(start, 1)
        GL20.glEnableVertexAttribArray(start)
        strideStart += FLOAT_SIZE_BYTES * 2
        start++

        // Selected or Scaling (for particles)
        GL20.glVertexAttribPointer(
            start,
            1,
            GL11.GL_FLOAT,
            false,
            INSTANCE_SIZE_BYTES,
            strideStart.toLong()
        )
        GL33.glVertexAttribDivisor(start, 1)
        GL20.glEnableVertexAttribArray(start)
//        start++

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
    }
}

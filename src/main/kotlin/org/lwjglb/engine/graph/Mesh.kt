package org.lwjglb.engine.graph

import org.lwjgl.opengl.*
import org.lwjgl.system.MemoryUtil
import org.lwjglb.engine.items.GameItem
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

open class Mesh @JvmOverloads constructor(
        positions: FloatArray?,
        textCoordinates: FloatArray?,
        normals: FloatArray?,
        indices: IntArray?,
        jointIndices: IntArray? = createEmptyIntArray(
            MAX_WEIGHTS * positions!!.size / 3,
            0
        ),
        weights: FloatArray? = createEmptyFloatArray(
            MAX_WEIGHTS * positions!!.size / 3,
            0f
        ),
        val material: Material)
{
    var vaoId = 0
    protected var vboIdList: MutableList<Int> = ArrayList()
    val vertexCount = indices!!.size
    var boundingRadius = 0f

    protected fun initRender() {
        val texture = material.texture
        if (texture != null) {
            // Activate first texture bank
            GL13.glActiveTexture(GL13.GL_TEXTURE0)
            // Bind the texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id)
        }
        val normalMap = material.normalMap
        if (normalMap != null) {
            // Activate second texture bank
            GL13.glActiveTexture(GL13.GL_TEXTURE1)
            // Bind the texture
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, normalMap.id)
        }

        // Draw the mesh
        GL30.glBindVertexArray(vaoId)
    }

    protected fun endRender() {
        // Restore state
        GL30.glBindVertexArray(0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    fun render() {
        initRender()
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0)
        endRender()
    }

    fun renderList(
        gameItems: List<GameItem>?,
        consumer: (GameItem) -> Unit)
    {
        initRender()
        gameItems?.forEach { gameItem ->
            if (gameItem.isInsideFrustum) {
                // Set up data required by GameItem
                consumer.invoke(gameItem)
                // Render this game item
                GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0)
            }
        }
        endRender()
    }

    open fun cleanUp() {
        GL20.glDisableVertexAttribArray(0)

        // Delete the VBOs
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        for (vboId in vboIdList) {
            GL15.glDeleteBuffers(vboId)
        }

        // Delete the texture
        val texture = material.texture
        texture?.cleanup()

        // Delete the VAO
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(vaoId)
    }

    fun deleteBuffers() {
        // Delete the VBOs
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        for (vboId in vboIdList) {
            GL15.glDeleteBuffers(vboId)
        }

        // Delete the VAO
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(vaoId)
    }

    companion object {
        const val MAX_WEIGHTS = 4
        fun createEmptyFloatArray(length: Int, defaultValue: Float): FloatArray {
            val result = FloatArray(length)
            Arrays.fill(result, defaultValue)
            return result
        }

        fun createEmptyIntArray(length: Int, defaultValue: Int): IntArray {
            val result = IntArray(length)
            Arrays.fill(result, defaultValue)
            return result
        }
    }

    init {
        if (vertexCount <= 0) {
            throw IllegalArgumentException("No vertices supplied")
        }

        var posBuffer: FloatBuffer? = null
        var textCoordinatesBuffer: FloatBuffer? = null
        var vecNormalsBuffer: FloatBuffer? = null
        var weightsBuffer: FloatBuffer? = null
        var jointIndicesBuffer: IntBuffer? = null
        var indicesBuffer: IntBuffer? = null
        try {
            vaoId = GL30.glGenVertexArrays()
            GL30.glBindVertexArray(vaoId)

            // Position VBO
            var vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            posBuffer = MemoryUtil.memAllocFloat(positions!!.size)
            posBuffer.put(positions).flip()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, posBuffer!!, GL15.GL_STATIC_DRAW)
            GL20.glEnableVertexAttribArray(0)
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)

            // Texture coordinates VBO
            vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            textCoordinatesBuffer = MemoryUtil.memAllocFloat(textCoordinates!!.size)
            textCoordinatesBuffer.put(textCoordinates).flip()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, textCoordinatesBuffer!!, GL15.GL_STATIC_DRAW)
            GL20.glEnableVertexAttribArray(1)
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0)

            // Vertex normals VBO
            vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            vecNormalsBuffer = MemoryUtil.memAllocFloat(normals!!.size)
            if (vecNormalsBuffer.capacity() > 0) {
                vecNormalsBuffer.put(normals).flip()
            } else {
                // Create empty structure
                vecNormalsBuffer = MemoryUtil.memAllocFloat(positions.size)
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vecNormalsBuffer!!, GL15.GL_STATIC_DRAW)
            GL20.glEnableVertexAttribArray(2)
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 0, 0)

            // Weights
            vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            weightsBuffer = MemoryUtil.memAllocFloat(weights!!.size)
            weightsBuffer.put(weights).flip()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, weightsBuffer!!, GL15.GL_STATIC_DRAW)
            GL20.glEnableVertexAttribArray(3)
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, 0, 0)

            // Joint indices
            vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            jointIndicesBuffer = MemoryUtil.memAllocInt(jointIndices!!.size)
            jointIndicesBuffer.put(jointIndices).flip()
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, jointIndicesBuffer!!, GL15.GL_STATIC_DRAW)
            GL20.glEnableVertexAttribArray(4)
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, 0, 0)

            // Index VBO
            vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            indicesBuffer = MemoryUtil.memAllocInt(vertexCount)
            indicesBuffer.put(indices).flip()
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer!!, GL15.GL_STATIC_DRAW)
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
            GL30.glBindVertexArray(0)
        } finally {
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer)
            }
            if (textCoordinatesBuffer != null) {
                MemoryUtil.memFree(textCoordinatesBuffer)
            }
            if (vecNormalsBuffer != null) {
                MemoryUtil.memFree(vecNormalsBuffer)
            }
            if (weightsBuffer != null) {
                MemoryUtil.memFree(weightsBuffer)
            }
            if (jointIndicesBuffer != null) {
                MemoryUtil.memFree(jointIndicesBuffer)
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer)
            }
        }
    }
}

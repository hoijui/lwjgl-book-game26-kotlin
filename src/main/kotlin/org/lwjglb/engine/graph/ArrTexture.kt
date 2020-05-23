package org.lwjglb.engine.graph

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL14
import java.nio.ByteBuffer

class ArrTexture(numTextures: Int, width: Int, height: Int, pixelFormat: Int) {
    val ids: IntArray
    val width: Int
    val height: Int

    fun cleanup() {
        for (id in ids) {
            GL11.glDeleteTextures(id)
        }
    }

    init {
        ids = IntArray(numTextures)
        GL11.glGenTextures(ids)
        this.width = width
        this.height = height
        for (i in 0 until numTextures) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ids[i])
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_DEPTH_COMPONENT,
                this.width,
                this.height,
                0,
                pixelFormat,
                GL11.GL_FLOAT,
                null as ByteBuffer?
            )
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        }
    }
}
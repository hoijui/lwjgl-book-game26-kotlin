package org.lwjglb.engine.graph.shadow

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import org.lwjglb.engine.graph.ArrTexture

class ShadowBuffer {
    val depthMapFBO: Int
    val depthMapTexture: ArrTexture

    fun bindTextures(start: Int) {
        for (i in 0 until ShadowRenderer.Companion.NUM_CASCADES) {
            GL13.glActiveTexture(start + i)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthMapTexture.ids[i])
        }
    }

    fun cleanup() {
        GL30.glDeleteFramebuffers(depthMapFBO)
        depthMapTexture.cleanup()
    }

    companion object {
        val SHADOW_MAP_WIDTH = Math.pow(65.0, 2.0).toInt()
        val SHADOW_MAP_HEIGHT = SHADOW_MAP_WIDTH
    }

    init {
        // Create a FBO to render the depth map
        depthMapFBO = GL30.glGenFramebuffers()

        // Create the depth map textures
        depthMapTexture = ArrTexture(
            ShadowRenderer.Companion.NUM_CASCADES,
            SHADOW_MAP_WIDTH,
            SHADOW_MAP_HEIGHT,
            GL11.GL_DEPTH_COMPONENT
        )

        // Attach the the depth map texture to the FBO
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO)
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL11.GL_TEXTURE_2D,
            depthMapTexture.ids[0],
            0
        )

        // Set only depth
        GL11.glDrawBuffer(GL11.GL_NONE)
        GL11.glReadBuffer(GL11.GL_NONE)
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw Exception("Could not create FrameBuffer")
        }

        // Unbind
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    }
}
package org.lwjglb.engine.graph

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

class Texture {

    val id: Int
    var width: Int = -1
    var height: Int = -1
    var numRows = 1
        private set
    var numCols = 1
        private set

    /**
     * Creates an empty texture.
     *
     * @param width Width of the texture
     * @param height Height of the texture
     * @param pixelFormat Specifies the format of the pixel data (GL_RGBA, etc.)
     * @throws Exception
     */
    constructor(width: Int, height: Int, pixelFormat: Int) {
        id = GL11.glGenTextures()
        this.width = width
        this.height = height
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
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
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
    }

    constructor(fileName: String) {
        var buf: ByteBuffer? = null
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            buf = STBImage.stbi_load(fileName, w, h, channels, 4)
            if (buf == null) {
                throw Exception("Image file [" + fileName + "] not loaded: " + STBImage.stbi_failure_reason())
            }
            width = w.get()
            height = h.get()
        }
        id = createTexture(buf!!)
        STBImage.stbi_image_free(buf!!)
    }

    constructor(fileName: String, numCols: Int, numRows: Int) : this(fileName) {
        this.numCols = numCols
        this.numRows = numRows
    }

    constructor(imageBuffer: ByteBuffer) {
        var buf: ByteBuffer? = null
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            buf = STBImage.stbi_load_from_memory(imageBuffer, w, h, channels, 4)
            if (buf == null) {
                throw Exception("Image file not loaded: " + STBImage.stbi_failure_reason())
            }
            width = w.get()
            height = h.get()
        }
        id = createTexture(buf!!)
        STBImage.stbi_image_free(buf!!)
    }

    private fun createTexture(buf: ByteBuffer): Int {
        // Create a new OpenGL texture
        val textureId = GL11.glGenTextures()
        // Bind the texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

        // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte size
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        // Upload the texture data
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf
        )
        // Generate Mip Map
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
        return textureId
    }

    fun bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
    }

    fun cleanup() {
        GL11.glDeleteTextures(id)
    }
}

package org.lwjglb.engine.graph.shadow

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjglb.engine.Utils
import org.lwjglb.engine.Window
import org.lwjglb.engine.graph.Material
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.graph.ShaderProgram
import org.lwjglb.engine.loaders.obj.OBJLoader

class ShadowTestRenderer {
    
    private var testShaderProgram: ShaderProgram? = null
    private var quadMesh: Mesh? = null

    @Throws(Exception::class)
    fun init(window: Window?) {
        setupTestShader()
    }

    @Throws(Exception::class)
    private fun setupTestShader() {
        testShaderProgram = ShaderProgram()
        testShaderProgram!!.createVertexShader(Utils.loadResource("/shaders/test_vertex.vs"))
        testShaderProgram!!.createFragmentShader(Utils.loadResource("/shaders/test_fragment.fs"))
        testShaderProgram!!.link()
        for (i in 0 until ShadowRenderer.NUM_CASCADES) {
            testShaderProgram!!.createUniform("texture_sampler[$i]")
        }
        quadMesh = OBJLoader.loadMesh("/models/quad.obj", material=Material())
    }

    fun renderTest(shadowMap: ShadowBuffer) {
        testShaderProgram!!.bind()
        testShaderProgram!!.setUniform("texture_sampler[0]", 0)
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMap.depthMapTexture.ids[0])
        quadMesh!!.render()
        testShaderProgram!!.unbind()
    }

    fun cleanup() {
        if (testShaderProgram != null) {
            testShaderProgram!!.cleanup()
        }
    }
}
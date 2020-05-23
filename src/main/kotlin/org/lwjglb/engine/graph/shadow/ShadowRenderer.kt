package org.lwjglb.engine.graph.shadow

import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import org.lwjglb.engine.Scene
import org.lwjglb.engine.Utils
import org.lwjglb.engine.Window
import org.lwjglb.engine.graph.Camera
import org.lwjglb.engine.graph.Renderer
import org.lwjglb.engine.graph.ShaderProgram
import org.lwjglb.engine.graph.Transformation
import org.lwjglb.engine.graph.anim.AnimGameItem
import org.lwjglb.engine.items.GameItem
import java.util.*

class ShadowRenderer {

    private var depthShaderProgram: ShaderProgram? = null
    var shadowCascades: List<ShadowCascade>? = null
        private set
    private var shadowBuffer: ShadowBuffer? = null
    private val filteredItems: MutableList<GameItem>

    @Throws(Exception::class)
    fun init(window: Window) {
        shadowBuffer = ShadowBuffer()
        val tmpShadowCascades : MutableList<ShadowCascade> = ArrayList()
        setupDepthShader()
        var zNear: Float = Window.Z_NEAR
        for (i in 0 until NUM_CASCADES) {
            val shadowCascade = ShadowCascade(zNear, CASCADE_SPLITS[i])
            tmpShadowCascades.add(shadowCascade)
            zNear = CASCADE_SPLITS[i]
        }
        shadowCascades = tmpShadowCascades
    }

    fun bindTextures(start: Int) {
        shadowBuffer!!.bindTextures(start)
    }

    @Throws(Exception::class)
    private fun setupDepthShader() {
        depthShaderProgram = ShaderProgram()
        depthShaderProgram!!.createVertexShader(Utils.loadResource("/shaders/depth_vertex.vs"))
        depthShaderProgram!!.createFragmentShader(Utils.loadResource("/shaders/depth_fragment.fs"))
        depthShaderProgram!!.link()
        depthShaderProgram!!.createUniform("isInstanced")
        depthShaderProgram!!.createUniform("modelNonInstancedMatrix")
        depthShaderProgram!!.createUniform("lightViewMatrix")
        depthShaderProgram!!.createUniform("jointsMatrix")
        depthShaderProgram!!.createUniform("orthoProjectionMatrix")
    }

    private fun update(
        window: Window,
        viewMatrix: Matrix4f,
        scene: Scene)
    {
        val directionalLight = scene.sceneLight.directionalLight
        for (i in 0 until NUM_CASCADES) {
            val shadowCascade = shadowCascades!![i]
            shadowCascade.update(window, viewMatrix, directionalLight)
        }
    }

    fun render(
        window: Window,
        scene: Scene,
        camera: Camera,
        transformation: Transformation,
        renderer: Renderer?)
    {
        update(window, camera.viewMatrix, scene)

        // Setup view port to match the texture size
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, shadowBuffer!!.depthMapFBO)
        GL11.glViewport(0, 0, ShadowBuffer.SHADOW_MAP_WIDTH, ShadowBuffer.SHADOW_MAP_HEIGHT)
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        depthShaderProgram!!.bind()

        // Render scene for each cascade map
        for (i in 0 until NUM_CASCADES) {
            val shadowCascade = shadowCascades!![i]
            depthShaderProgram!!.setUniform("orthoProjectionMatrix", shadowCascade.orthoProjectionMatrix)
            depthShaderProgram!!.setUniform("lightViewMatrix", shadowCascade.lightViewMatrix)
            GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                shadowBuffer!!.depthMapTexture.ids[i],
                0
            )
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            renderNonInstancedMeshes(scene, transformation)
            renderInstancedMeshes(scene, transformation)
        }

        // Unbind
        depthShaderProgram!!.unbind()
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    }

    private fun renderNonInstancedMeshes(scene: Scene, transformation: Transformation) {
        depthShaderProgram!!.setUniform("isInstanced", 0)

        // Render each mesh with the associated game Items
        val mapMeshes = scene.gameMeshes
        for (mesh in mapMeshes.keys) {
            mesh.renderList(mapMeshes[mesh]) { gameItem: GameItem ->
                val modelMatrix = transformation.buildModelMatrix(gameItem)
                depthShaderProgram!!.setUniform("modelNonInstancedMatrix", modelMatrix)
                if (gameItem is AnimGameItem) {
                    val frame = gameItem.getCurrentAnimatedFrame()
                    depthShaderProgram!!.setUniform("jointsMatrix", frame.jointMatrices)
                }
            }
        }
    }

    private fun renderInstancedMeshes(scene: Scene, transformation: Transformation) {
        depthShaderProgram!!.setUniform("isInstanced", 1)

        // Render each mesh with the associated game Items
        val mapMeshes = scene.gameInstancedMeshes
        for (mesh in mapMeshes.keys) {
            filteredItems.clear()
            for (gameItem in mapMeshes[mesh]!!) {
                if (gameItem.isInsideFrustum) {
                    filteredItems.add(gameItem)
                }
            }
            bindTextures(GL13.GL_TEXTURE2)
            mesh.renderListInstanced(filteredItems, transformation, null)
        }
    }

    fun cleanup() {
        if (shadowBuffer != null) {
            shadowBuffer!!.cleanup()
        }
        if (depthShaderProgram != null) {
            depthShaderProgram!!.cleanup()
        }
    }

    companion object {
        const val NUM_CASCADES = 3
        val CASCADE_SPLITS = floatArrayOf(
            Window.Z_FAR / 20.0f,
            Window.Z_FAR / 10.0f,
            Window.Z_FAR
        )
    }

    init {
        filteredItems = ArrayList()
    }
}
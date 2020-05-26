package org.lwjglb.engine.graph

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjglb.engine.Scene
import org.lwjglb.engine.SceneLight
import org.lwjglb.engine.Utils
import org.lwjglb.engine.Window
import org.lwjglb.engine.graph.anim.AnimGameItem
import org.lwjglb.engine.graph.lights.DirectionalLight
import org.lwjglb.engine.graph.lights.PointLight
import org.lwjglb.engine.graph.lights.SpotLight
import org.lwjglb.engine.graph.shadow.ShadowRenderer
import org.lwjglb.engine.items.GameItem
import java.util.*

class Renderer {

    private val transformation: Transformation = Transformation()
    private val shadowRenderer: ShadowRenderer?
    private var sceneShaderProgram: ShaderProgram? = null
    private var skyBoxShaderProgram: ShaderProgram? = null
    private var particlesShaderProgram: ShaderProgram? = null
    private val specularPower: Float = 10.0f
    private val frustumFilter: FrustumCullingFilter
    private val filteredItems: MutableList<GameItem>

    init {
        shadowRenderer = ShadowRenderer()
        frustumFilter = FrustumCullingFilter()
        filteredItems = ArrayList()
    }

    @Throws(Exception::class)
    fun init(window: Window) {
        shadowRenderer!!.init(window)
        skyBoxShaderProgram = setupSkyBoxShader()
        sceneShaderProgram = setupSceneShader()
        particlesShaderProgram = setupParticlesShader()
    }

    fun render(
        window: Window,
        camera: Camera,
        scene: Scene,
        sceneChanged: Boolean)
    {
        clear()
        if (window.options.frustumCulling) {
            frustumFilter.updateFrustum(window.projectionMatrix, camera.viewMatrix)
            frustumFilter.filter(scene.gameMeshes)
            frustumFilter.filter(scene.gameInstancedMeshes)
        }

        // Render depth map before view ports has been set up
        if (scene.isRenderShadows && sceneChanged) {
            shadowRenderer!!.render(window, scene, camera, transformation, this)
        }
        GL11.glViewport(0, 0, window.width, window.height)

        // Update projection matrix once per render cycle
        window.updateProjectionMatrix()
        renderScene(window, camera, scene)
        renderSkyBox(window, camera, scene)
        renderParticles(window, camera, scene)

        renderAxes(window, camera)
        renderCrossHair(window)
    }

    fun clear() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT or GL11.GL_STENCIL_BUFFER_BIT)
    }

    private fun renderParticles(
        window: Window,
        camera: Camera,
        scene: Scene)
    {
        particlesShaderProgram!!.bind()
        val viewMatrix = camera.viewMatrix
        particlesShaderProgram!!.setUniform("viewMatrix", viewMatrix)
        particlesShaderProgram!!.setUniform("texture_sampler", 0)
        val projectionMatrix = window.projectionMatrix
        particlesShaderProgram!!.setUniform("projectionMatrix", projectionMatrix)
        val emitters = scene.particleEmitters
        val numEmitters = emitters?.size ?: 0
        GL11.glDepthMask(false)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        for (i in 0 until numEmitters) {
            val emitter = emitters!![i]
            val mesh = emitter.baseParticle.mesh as InstancedMesh
            val text = mesh.material.texture
            if (text != null) {
                particlesShaderProgram!!.setUniform("numCols", text.numCols)
                particlesShaderProgram!!.setUniform("numRows", text.numRows)
            }
            mesh.renderListInstanced(emitter.particles, true, transformation, viewMatrix)
        }
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glDepthMask(true)
        particlesShaderProgram!!.unbind()
    }

    private fun renderSkyBox(
        window: Window,
        camera: Camera,
        scene: Scene)
    {
        val skyBox = scene.skyBox
        if (skyBox != null) {
            skyBoxShaderProgram?.let { skyBoxShaderProgram ->
                skyBoxShaderProgram.bind()
                skyBoxShaderProgram.setUniform("texture_sampler", 0)
                val projectionMatrix = window.projectionMatrix
                skyBoxShaderProgram.setUniform("projectionMatrix", projectionMatrix)
                val viewMatrix = camera.viewMatrix
                val m30 = viewMatrix.m30()
                viewMatrix.m30(0f)
                val m31 = viewMatrix.m31()
                viewMatrix.m31(0f)
                val m32 = viewMatrix.m32()
                viewMatrix.m32(0f)
                val mesh = skyBox.mesh
                val modelViewMatrix = transformation.buildModelViewMatrix(skyBox, viewMatrix)
                skyBoxShaderProgram.setUniform("modelViewMatrix", modelViewMatrix)
                skyBoxShaderProgram.setUniform("ambientLight", scene.sceneLight.skyBoxLight)
                skyBoxShaderProgram.setUniform("colour", mesh.material.ambientColour)
                skyBoxShaderProgram.setUniform("hasTexture", if (mesh.material.isTextured) 1 else 0)
                mesh.render()
                viewMatrix.m30(m30)
                viewMatrix.m31(m31)
                viewMatrix.m32(m32)
                skyBoxShaderProgram.unbind()
            }
        }
    }

    private fun renderScene(window: Window, camera: Camera, scene: Scene) {
        sceneShaderProgram!!.bind()
        val viewMatrix = camera.viewMatrix
        val projectionMatrix = window.projectionMatrix
        sceneShaderProgram!!.setUniform("viewMatrix", viewMatrix)
        sceneShaderProgram!!.setUniform("projectionMatrix", projectionMatrix)
        val shadowCascades = shadowRenderer!!.shadowCascades
        for (i in 0 until ShadowRenderer.NUM_CASCADES) {
            val shadowCascade = shadowCascades!![i]
            sceneShaderProgram!!.setUniform("orthoProjectionMatrix", shadowCascade.orthoProjectionMatrix, i)
            sceneShaderProgram!!.setUniform("cascadeFarPlanes", ShadowRenderer.CASCADE_SPLITS[i], i)
            sceneShaderProgram!!.setUniform("lightViewMatrix", shadowCascade.lightViewMatrix, i)
        }
        renderLights(viewMatrix, scene.sceneLight)
        sceneShaderProgram!!.setUniform("fog", scene.fog)
        sceneShaderProgram!!.setUniform("texture_sampler", 0)
        sceneShaderProgram!!.setUniform("normalMap", 1)
        val start = 2
        for (i in 0 until ShadowRenderer.NUM_CASCADES) {
            sceneShaderProgram!!.setUniform("shadowMap_$i", start + i)
        }
        sceneShaderProgram!!.setUniform("renderShadow", if (scene.isRenderShadows) 1 else 0)
        renderNonInstancedMeshes(scene)
        renderInstancedMeshes(scene, viewMatrix)
        sceneShaderProgram!!.unbind()
    }

    private fun renderNonInstancedMeshes(scene: Scene) {
        sceneShaderProgram!!.setUniform("isInstanced", 0)

        // Render each mesh with the associated game Items
        val mapMeshes = scene.gameMeshes
        for (mesh in mapMeshes.keys) {
            sceneShaderProgram!!.setUniform("material", mesh.material)
            val text = mesh.material.texture
            if (text != null) {
                sceneShaderProgram!!.setUniform("numCols", text.numCols)
                sceneShaderProgram!!.setUniform("numRows", text.numRows)
            }
            shadowRenderer!!.bindTextures(GL13.GL_TEXTURE2)
            mesh.renderList(mapMeshes[mesh]) { gameItem: GameItem ->
                sceneShaderProgram!!.setUniform("selectedNonInstanced", if (gameItem.isSelected) 1.0f else 0.0f)
                val modelMatrix = transformation.buildModelMatrix(gameItem)
                sceneShaderProgram!!.setUniform("modelNonInstancedMatrix", modelMatrix)
                if (gameItem is AnimGameItem) {
                    val frame = gameItem.getCurrentAnimatedFrame()
                    sceneShaderProgram!!.setUniform("jointsMatrix", frame.jointMatrices)
                }
            }
        }
    }

    private fun renderInstancedMeshes(scene: Scene, viewMatrix: Matrix4f) {
        sceneShaderProgram!!.setUniform("isInstanced", 1)

        // Render each mesh with the associated game Items
        val mapMeshes = scene.gameInstancedMeshes
        for (mesh in mapMeshes.keys) {
            val material = mesh.material
            val text = material.texture
            if (text != null) {
                sceneShaderProgram!!.setUniform("numCols", text.numCols)
                sceneShaderProgram!!.setUniform("numRows", text.numRows)
            }
            sceneShaderProgram!!.setUniform("material", material)
            filteredItems.clear()
            mapMeshes[mesh]?.forEach { gameItem ->
                if (gameItem.isInsideFrustum) {
                    filteredItems.add(gameItem)
                }
            }
            shadowRenderer!!.bindTextures(GL13.GL_TEXTURE2)
            mesh.renderListInstanced(filteredItems, transformation, viewMatrix)
        }
    }

    private fun renderLights(viewMatrix: Matrix4f, sceneLight: SceneLight) {
        sceneShaderProgram!!.setUniform("ambientLight", sceneLight.ambientLight)
        sceneShaderProgram!!.setUniform("specularPower", specularPower)

        // Process Point Lights
        val pointLightList = sceneLight.pointLightList
        for (i in 0 until pointLightList.size) {
            // Get a copy of the point light object and transform its position to view coordinates
            val currPointLight = PointLight(pointLightList[i])
            val lightPos = currPointLight.position
            val aux = Vector4f(lightPos, 1.0f)
            aux.mul(viewMatrix)
            lightPos.x = aux.x
            lightPos.y = aux.y
            lightPos.z = aux.z
            sceneShaderProgram!!.setUniform("pointLights", currPointLight, i)
        }

        // Process Spot Lights
        val spotLightList = sceneLight.spotLightList
        for (i in 0 until spotLightList.size) {
            // Get a copy of the spot light object and transform its position and cone direction to view coordinates
            val currSpotLight = SpotLight(spotLightList[i])
            val dir = Vector4f(currSpotLight.coneDirection, 0.0f)
            dir.mul(viewMatrix)
            currSpotLight.coneDirection = Vector3f(dir.x, dir.y, dir.z)
            val lightPos = currSpotLight.pointLight.position
            val aux = Vector4f(lightPos, 1.0f)
            aux.mul(viewMatrix)
            lightPos.x = aux.x
            lightPos.y = aux.y
            lightPos.z = aux.z
            sceneShaderProgram!!.setUniform("spotLights", currSpotLight, i)
        }

        // Get a copy of the directional light object and transform its position to view coordinates
        val currDirLight = DirectionalLight(sceneLight.directionalLight)
        val dir = Vector4f(currDirLight.direction, 0.0f)
        dir.mul(viewMatrix)
        currDirLight.direction = Vector3f(dir.x, dir.y, dir.z)
        sceneShaderProgram!!.setUniform("directionalLight", currDirLight)
    }

    private fun renderCrossHair(window: Window) {
        if (window.options.compatibleProfile) {
            GL11.glPushMatrix()
            GL11.glLoadIdentity()
            val inc = 0.05f
            GL11.glLineWidth(1.0f)
            GL11.glBegin(GL11.GL_LINES)
            GL11.glColor3f(1.0f, 1.0f, 1.0f)

            // Horizontal line
            GL11.glVertex3f(-inc, 0.0f, 0.0f)
            GL11.glVertex3f(+inc, 0.0f, 0.0f)
            GL11.glEnd()

            // Vertical line
            GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex3f(0.0f, -inc, 0.0f)
            GL11.glVertex3f(0.0f, +inc, 0.0f)
            GL11.glEnd()
            GL11.glPopMatrix()
        }
    }

    /**
     * Renders the three axis in space (For debugging purposes only
     *
     * @param camera
     */
    private fun renderAxes(window: Window, camera: Camera) {

        val opts = window.options
        if (opts.compatibleProfile) {
            GL11.glPushMatrix()
            GL11.glLoadIdentity()
            val rotX = camera.rotation.x
            val rotY = camera.rotation.y
            val rotZ = 0f
            GL11.glRotatef(rotX, 1.0f, 0.0f, 0.0f)
            GL11.glRotatef(rotY, 0.0f, 1.0f, 0.0f)
            GL11.glRotatef(rotZ, 0.0f, 0.0f, 1.0f)
            GL11.glLineWidth(2.0f)
            GL11.glBegin(GL11.GL_LINES)
            // X Axis
            GL11.glColor3f(1.0f, 0.0f, 0.0f)
            GL11.glVertex3f(0.0f, 0.0f, 0.0f)
            GL11.glVertex3f(1.0f, 0.0f, 0.0f)
            // Y Axis
            GL11.glColor3f(0.0f, 1.0f, 0.0f)
            GL11.glVertex3f(0.0f, 0.0f, 0.0f)
            GL11.glVertex3f(0.0f, 1.0f, 0.0f)
            // Z Axis
            GL11.glColor3f(1.0f, 1.0f, 1.0f)
            GL11.glVertex3f(0.0f, 0.0f, 0.0f)
            GL11.glVertex3f(0.0f, 0.0f, 1.0f)
            GL11.glEnd()
            GL11.glPopMatrix()
        }
    }

    fun cleanup() {
        shadowRenderer?.cleanup()
        skyBoxShaderProgram?.cleanup()
        sceneShaderProgram?.cleanup()
        particlesShaderProgram?.cleanup()
    }

    companion object {
        private const val MAX_POINT_LIGHTS = 5
        private const val MAX_SPOT_LIGHTS = 5

        @Throws(Exception::class)
        private fun setupSkyBoxShader(): ShaderProgram {
            val tmpSkyBoxShaderProgram = ShaderProgram()
            tmpSkyBoxShaderProgram.createVertexShader(Utils.loadResource("/shaders/sb_vertex.vs"))
            tmpSkyBoxShaderProgram.createFragmentShader(Utils.loadResource("/shaders/sb_fragment.fs"))
            tmpSkyBoxShaderProgram.link()

            // Create uniforms for projection matrix
            tmpSkyBoxShaderProgram.createUniform("projectionMatrix")
            tmpSkyBoxShaderProgram.createUniform("modelViewMatrix")
            tmpSkyBoxShaderProgram.createUniform("texture_sampler")
            tmpSkyBoxShaderProgram.createUniform("ambientLight")
            tmpSkyBoxShaderProgram.createUniform("colour")
            tmpSkyBoxShaderProgram.createUniform("hasTexture")
            return tmpSkyBoxShaderProgram
        }

        @Throws(Exception::class)
        private fun setupSceneShader() : ShaderProgram {
            // Create shader
            val sceneShaderProgram = ShaderProgram()
            sceneShaderProgram.createVertexShader(Utils.loadResource("/shaders/scene_vertex.vs"))
            sceneShaderProgram.createFragmentShader(Utils.loadResource("/shaders/scene_fragment.fs"))
            sceneShaderProgram.link()

            // Create uniforms for view and projection matrices
            sceneShaderProgram.createUniform("viewMatrix")
            sceneShaderProgram.createUniform("projectionMatrix")
            sceneShaderProgram.createUniform("texture_sampler")
            sceneShaderProgram.createUniform("normalMap")
            // Create uniform for material
            sceneShaderProgram.createMaterialUniform("material")
            // Create lighting related uniforms
            sceneShaderProgram.createUniform("specularPower")
            sceneShaderProgram.createUniform("ambientLight")
            sceneShaderProgram.createPointLightListUniform(
                    "pointLights",
                    MAX_POINT_LIGHTS
            )
            sceneShaderProgram.createSpotLightListUniform(
                    "spotLights",
                    MAX_SPOT_LIGHTS
            )
            sceneShaderProgram.createDirectionalLightUniform("directionalLight")
            sceneShaderProgram.createFogUniform("fog")

            // Create uniforms for shadow mapping
            for (i in 0 until ShadowRenderer.NUM_CASCADES) {
                sceneShaderProgram.createUniform("shadowMap_$i")
            }
            sceneShaderProgram.createUniform("orthoProjectionMatrix", ShadowRenderer.NUM_CASCADES)
            sceneShaderProgram.createUniform("modelNonInstancedMatrix")
            sceneShaderProgram.createUniform("lightViewMatrix", ShadowRenderer.NUM_CASCADES)
            sceneShaderProgram.createUniform("cascadeFarPlanes", ShadowRenderer.NUM_CASCADES)
            sceneShaderProgram.createUniform("renderShadow")

            // Create uniform for joint matrices
            sceneShaderProgram.createUniform("jointsMatrix")
            sceneShaderProgram.createUniform("isInstanced")
            sceneShaderProgram.createUniform("numCols")
            sceneShaderProgram.createUniform("numRows")
            sceneShaderProgram.createUniform("selectedNonInstanced")

            return sceneShaderProgram
        }

        @Throws(Exception::class)
        private fun setupParticlesShader() : ShaderProgram {

            val particlesShaderProgram = ShaderProgram()

            particlesShaderProgram.createVertexShader(Utils.loadResource("/shaders/particles_vertex.vs"))
            particlesShaderProgram.createFragmentShader(Utils.loadResource("/shaders/particles_fragment.fs"))
            particlesShaderProgram.link()
            particlesShaderProgram.createUniform("viewMatrix")
            particlesShaderProgram.createUniform("projectionMatrix")
            particlesShaderProgram.createUniform("texture_sampler")
            particlesShaderProgram.createUniform("numCols")
            particlesShaderProgram.createUniform("numRows")

            return particlesShaderProgram
        }
    }
}

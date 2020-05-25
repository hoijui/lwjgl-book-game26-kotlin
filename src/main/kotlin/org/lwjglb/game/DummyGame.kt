package org.lwjglb.game

import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.glfw.GLFW
import org.lwjgl.openal.AL11
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjglb.engine.*
import org.lwjglb.engine.graph.*
import org.lwjglb.engine.graph.lights.DirectionalLight
import org.lwjglb.engine.graph.lights.PointLight
import org.lwjglb.engine.graph.particles.FlowParticleEmitter
import org.lwjglb.engine.graph.particles.Particle
import org.lwjglb.engine.graph.weather.Fog
import org.lwjglb.engine.items.GameItem
import org.lwjglb.engine.items.SkyBox
import org.lwjglb.engine.loaders.obj.OBJLoader
import org.lwjglb.engine.sound.SoundBuffer
import org.lwjglb.engine.sound.SoundListener
import org.lwjglb.engine.sound.SoundManager
import org.lwjglb.engine.sound.SoundSource
import java.nio.ByteBuffer
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.sin

class DummyGame : IGameLogic {

    private val cameraInc: Vector3f = Vector3f(0.0f, 0.0f, 0.0f)
    private val renderer: Renderer = Renderer()
    private val soundMgr = SoundManager()
    private val camera = Camera()
    private val scene: Scene = Scene(setupLights())
    private val hud: Hud? = Hud()
    private var angleInc = 0.0f
    private var lightAngle = 90.0f
    private var particleEmitter: FlowParticleEmitter? = null
    private var selectDetector: MouseBoxSelectionDetector? = null
    private var leftButtonPressed = false
    private var firstTime = true
    private var sceneChanged = false

    private enum class Sounds {
        FIRE
    }

    private val gameItems: MutableList<GameItem> = ArrayList()

    @Throws(Exception::class)
    override fun init(window: Window) {
        hud!!.init(window)
        renderer.init(window)
        soundMgr.init()
        leftButtonPressed = false
        val reflectance = 1f
        val blockScale = 0.5f
        val skyBoxScale = 100.0f
        val extension = 2.0f
        val startX = extension * (-skyBoxScale + blockScale)
        val startZ = extension * (skyBoxScale - blockScale)
        val startY = -1.0f
        val inc = blockScale * 2
        var posX = startX
        var posZ = startZ
        selectDetector = MouseBoxSelectionDetector()
        var buf: ByteBuffer? = null
        var width: Int = -1
        var height: Int = -1
        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val channels = stack.mallocInt(1)
            buf = STBImage.stbi_load("textures/heightmap.png", w, h, channels, 4)
            if (buf == null) {
                throw Exception("Image file not loaded: " + STBImage.stbi_failure_reason())
            }
            width = w.get()
            height = h.get()
        }
        val instances = height * width
        val texture = Texture("textures/terrain_textures.png", 2, 1)
        val material = Material(texture, reflectance)
        val mesh = OBJLoader.loadMesh("/models/cube.obj", instances, material)
        mesh.boundingRadius = 1f
        gameItems.clear()
        for (i in 0 until height) {
            for (j in 0 until width) {
                val gameItem = GameItem(mesh)
                gameItem.scale = blockScale
                val rgb: Int = HeightMapMesh.getRGB(i, j, width, buf)
                val incY = rgb / (10 * 255 * 255).toFloat()
                gameItem.setPosition(posX, startY + incY, posZ)
                val textPos = if (Math.random() > 0.5f) 0 else 1
                gameItem.textPos = textPos
                gameItems.add(i * width + j, gameItem)
                posX += inc
            }
            posX = startX
            posZ -= inc
        }
        scene.setGameItems(gameItems)

        // Particles
        val maxParticles = 200
        val particleSpeed = Vector3f(0.0f, 1.0f, 0.0f)
        particleSpeed.mul(2.5f)
        val ttl: Long = 4000
        val creationPeriodMillis: Long = 300
        val range = 0.2f
        val scale = 1.0f
        val particleTexture =
            Texture("textures/particle_anim.png", 4, 4)
        val partMaterial = Material(particleTexture, reflectance)
        val partMesh = OBJLoader.loadMesh("/models/particle.obj", maxParticles, partMaterial)
        val particle = Particle(partMesh, particleSpeed, ttl, 100)
        particle.scale = scale
        val tmpParticleEmitter = FlowParticleEmitter(particle, maxParticles, creationPeriodMillis)
        tmpParticleEmitter.active = true
        tmpParticleEmitter.positionRndRange = range
        tmpParticleEmitter.speedRndRange = range
        tmpParticleEmitter.setAnimRange(10)
        scene.particleEmitters = arrayOf(tmpParticleEmitter)
        particleEmitter = tmpParticleEmitter

        // Shadows
        scene.isRenderShadows = true

        // Fog
        val fogColour = Vector3f(0.5f, 0.5f, 0.5f)
        scene.fog = Fog(true, fogColour, 0.02f)

        // Setup  SkyBox
        val skyBox = SkyBox("/models/skybox.obj", Vector4f(0.65f, 0.65f, 0.65f, 1.0f))
        skyBox.scale = skyBoxScale
        scene.skyBox = skyBox

        // Setup Lights
        camera.position.x = 0.25f
        camera.position.y = 6.5f
        camera.position.z = 6.5f
        camera.rotation.x = 25f
        camera.rotation.y = -1f
        STBImage.stbi_image_free(buf!!)

        // Sounds
        soundMgr.init()
        soundMgr.setAttenuationModel(AL11.AL_EXPONENT_DISTANCE)
        setupSounds()
    }

    @Throws(Exception::class)
    private fun setupSounds() {
        val buffFire = SoundBuffer("/sounds/fire.ogg")
        soundMgr.addSoundBuffer(buffFire)
        val sourceFire = SoundSource(loop = true, relative = false)
        val pos = particleEmitter!!.baseParticle.position
        sourceFire.setPosition(pos)
        sourceFire.setBuffer(buffFire.bufferId)
        soundMgr.addSoundSource(Sounds.FIRE.toString(), sourceFire)
        sourceFire.play()
        soundMgr.listener = SoundListener(Vector3f())
    }

    override fun input(window: Window, mouseInput: MouseInput) {
        sceneChanged = false
        cameraInc[0f, 0f] = 0f
        if (window.isKeyPressed(GLFW.GLFW_KEY_W)) {
            sceneChanged = true
            cameraInc.z = -1f
        } else if (window.isKeyPressed(GLFW.GLFW_KEY_S)) {
            sceneChanged = true
            cameraInc.z = 1f
        }
        if (window.isKeyPressed(GLFW.GLFW_KEY_A)) {
            sceneChanged = true
            cameraInc.x = -1f
        } else if (window.isKeyPressed(GLFW.GLFW_KEY_D)) {
            sceneChanged = true
            cameraInc.x = 1f
        }
        if (window.isKeyPressed(GLFW.GLFW_KEY_Z)) {
            sceneChanged = true
            cameraInc.y = -1f
        } else if (window.isKeyPressed(GLFW.GLFW_KEY_X)) {
            sceneChanged = true
            cameraInc.y = 1f
        }
        when {
            window.isKeyPressed(GLFW.GLFW_KEY_LEFT) -> {
                sceneChanged = true
                angleInc -= 0.05f
            }
            window.isKeyPressed(GLFW.GLFW_KEY_RIGHT) -> {
                sceneChanged = true
                angleInc += 0.05f
            }
            else -> {
                angleInc = 0f
            }
        }
    }

    override fun update(
        interval: Float,
        mouseInput: MouseInput,
        window: Window
    ) {
        if (mouseInput.isRightButtonPressed) {
            // Update camera based on mouse            
            val rotVec = mouseInput.displVec
            camera.moveRotation(
                rotVec.x * MOUSE_SENSITIVITY,
                rotVec.y * MOUSE_SENSITIVITY,
                0f
            )
            sceneChanged = true
        }

        // Update camera position
        camera.movePosition(
            cameraInc.x * CAMERA_POS_STEP,
            cameraInc.y * CAMERA_POS_STEP,
            cameraInc.z * CAMERA_POS_STEP
        )
        lightAngle += angleInc
        if (lightAngle < 0) {
            lightAngle = 0f
        } else if (lightAngle > 180) {
            lightAngle = 180f
        }
        val zValue = cos(Math.toRadians(lightAngle.toDouble())).toFloat()
        val yValue = sin(Math.toRadians(lightAngle.toDouble())).toFloat()
        val lightDirection = scene.sceneLight.directionalLight.direction
        lightDirection.x = 0f
        lightDirection.y = yValue
        lightDirection.z = zValue
        lightDirection.normalize()
        particleEmitter!!.update((interval * 1000).toLong())

        // Update view matrix
        camera.updateViewMatrix()

        // Update sound listener position;
        soundMgr.updateListenerPosition(camera)
        val aux = mouseInput.isLeftButtonPressed
        if (aux && !leftButtonPressed && selectDetector!!.selectGameItem(
                gameItems,
                window,
                mouseInput.currentPos,
                camera
            )
        ) {
            hud!!.incCounter()
        }
        leftButtonPressed = aux
    }

    override fun render(window: Window) {
        if (firstTime) {
            sceneChanged = true
            firstTime = false
        }
        renderer.render(window, camera, scene, sceneChanged)
        hud!!.render(window)
    }

    override fun cleanup() {
        renderer.cleanup()
        soundMgr.cleanup()
        scene.cleanup()
        hud?.cleanup()
    }

    companion object {
        private const val MOUSE_SENSITIVITY = 0.2f
        private const val CAMERA_POS_STEP = 0.10f

        private fun setupLights(): SceneLight {

            // Ambient Light
            val ambientLight = Vector3f(0.3f, 0.3f, 0.3f)
            val skyBoxLight = Vector3f(1.0f, 1.0f, 1.0f)

            // Directional Light
            val lightIntensity = 1.0f
            val lightDirection = Vector3f(0.0f, 1.0f, 1.0f)
            val directionalLight = DirectionalLight(Vector3f(1.0f, 1.0f, 1.0f), lightDirection, lightIntensity)

            return SceneLight(ambientLight, skyBoxLight, directionalLight)
        }
    }
}

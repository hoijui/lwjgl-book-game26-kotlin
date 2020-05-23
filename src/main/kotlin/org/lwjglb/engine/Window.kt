package org.lwjglb.engine

import org.joml.Matrix4f
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil

class Window(
    val title: String,
    var width: Int,
    var height: Int,
    private var vSync: Boolean,
    val options: WindowOptions
) {
    var windowHandle: Long = 0
        private set
    var isResized = false
    val projectionMatrix: Matrix4f
    fun init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }
        GLFW.glfwDefaultWindowHints() // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE) // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        if (options.compatibleProfile) {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE)
        } else {
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)
        }
        var maximized = false
        // If no size has been specified set it to maximized state
        if (width == 0 || height == 0) {
            // Set up a fixed width and height so window initialization does not fail
            width = 100
            height = 100
            GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_TRUE)
            maximized = true
        }

        // Create the window
        windowHandle = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        if (windowHandle == MemoryUtil.NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Setup resize callback
        GLFW.glfwSetFramebufferSizeCallback(
            windowHandle
        ) { window: Long, width: Int, height: Int ->
            this.width = width
            this.height = height
            isResized = true
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        GLFW.glfwSetKeyCallback(
            windowHandle
        ) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
            }
        }
        if (maximized) {
            GLFW.glfwMaximizeWindow(windowHandle)
        } else {
            // Get the resolution of the primary monitor
            val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
            // Center our window
            GLFW.glfwSetWindowPos(
                windowHandle,
                (vidmode!!.width() - width) / 2,
                (vidmode.height() - height) / 2
            )
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(windowHandle)
        if (isvSync()) {
            // Enable v-sync
            GLFW.glfwSwapInterval(1)
        }

        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)
        GL.createCapabilities()

        // Set the clear color
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_STENCIL_TEST)
        if (options.showTriangles) {
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
        }

        // Support for transparencies
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        if (options.cullFace) {
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glCullFace(GL11.GL_BACK)
        }

        // Antialiasing
        if (options.antialiasing) {
            GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4)
        }
    }

    fun restoreState() {
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_STENCIL_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        if (options.cullFace) {
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glCullFace(GL11.GL_BACK)
        }
    }

    var windowTitle: String
        get() = title
        set(title) {
            GLFW.glfwSetWindowTitle(windowHandle, title)
        }

    fun updateProjectionMatrix(): Matrix4f {
        val aspectRatio = width.toFloat() / height.toFloat()
        return projectionMatrix.setPerspective(
            FOV,
            aspectRatio,
            Z_NEAR,
            Z_FAR
        )
    }

    fun setClearColor(r: Float, g: Float, b: Float, alpha: Float) {
        GL11.glClearColor(r, g, b, alpha)
    }

    fun isKeyPressed(keyCode: Int): Boolean {
        return GLFW.glfwGetKey(windowHandle, keyCode) == GLFW.GLFW_PRESS
    }

    fun windowShouldClose(): Boolean {
        return GLFW.glfwWindowShouldClose(windowHandle)
    }

    fun isvSync(): Boolean {
        return vSync
    }

    fun setvSync(vSync: Boolean) {
        this.vSync = vSync
    }

    fun update() {
        GLFW.glfwSwapBuffers(windowHandle)
        GLFW.glfwPollEvents()
    }

    class WindowOptions {
        var cullFace = false
        var showTriangles = false
        var showFps = false
        var compatibleProfile = false
        var antialiasing = false
        var frustumCulling = false
    }

    companion object {
        /**
         * Field of View in Radians
         */
        val FOV = Math.toRadians(60.0).toFloat()

        /**
         * Distance to the near plane
         */
        const val Z_NEAR = 0.01f

        /**
         * Distance to the far plane
         */
        const val Z_FAR = 1000f
        fun updateProjectionMatrix(matrix: Matrix4f, width: Int, height: Int): Matrix4f {
            val aspectRatio = width.toFloat() / height.toFloat()
            return matrix.setPerspective(
                FOV,
                aspectRatio,
                Z_NEAR,
                Z_FAR
            )
        }
    }

    init {
        projectionMatrix = Matrix4f()
    }
}
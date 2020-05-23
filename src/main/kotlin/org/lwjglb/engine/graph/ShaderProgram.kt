package org.lwjglb.engine.graph

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack
import org.lwjglb.engine.graph.lights.DirectionalLight
import org.lwjglb.engine.graph.lights.PointLight
import org.lwjglb.engine.graph.lights.SpotLight
import org.lwjglb.engine.graph.weather.Fog
import java.util.*

class ShaderProgram {

    private val programId: Int = GL20.glCreateProgram()
    private var vertexShaderId = 0
    private var fragmentShaderId = 0
//    private val geometryShaderId = 0
    private val uniforms: MutableMap<String, Int> = HashMap()

    init {
        if (programId == 0) {
            throw Exception("Could not create Shader")
        }
    }

    @Throws(Exception::class)
    fun createUniform(uniformName: String) {
        val uniformLocation = GL20.glGetUniformLocation(programId, uniformName)
        if (uniformLocation < 0) {
            throw Exception("Could not find uniform:$uniformName")
        }
        uniforms[uniformName] = uniformLocation
    }

    @Throws(Exception::class)
    fun createUniform(uniformName: String, size: Int) {
        for (i in 0 until size) {
            createUniform("$uniformName[$i]")
        }
    }

    @Throws(Exception::class)
    fun createPointLightListUniform(uniformName: String, size: Int) {
        for (i in 0 until size) {
            createPointLightUniform("$uniformName[$i]")
        }
    }

    @Throws(Exception::class)
    fun createPointLightUniform(uniformName: String) {
        createUniform("$uniformName.colour")
        createUniform("$uniformName.position")
        createUniform("$uniformName.intensity")
        createUniform("$uniformName.att.constant")
        createUniform("$uniformName.att.linear")
        createUniform("$uniformName.att.exponent")
    }

    @Throws(Exception::class)
    fun createSpotLightListUniform(uniformName: String, size: Int) {
        for (i in 0 until size) {
            createSpotLightUniform("$uniformName[$i]")
        }
    }

    @Throws(Exception::class)
    fun createSpotLightUniform(uniformName: String) {
        createPointLightUniform("$uniformName.pl")
        createUniform("$uniformName.conedir")
        createUniform("$uniformName.cutoff")
    }

    @Throws(Exception::class)
    fun createDirectionalLightUniform(uniformName: String) {
        createUniform("$uniformName.colour")
        createUniform("$uniformName.direction")
        createUniform("$uniformName.intensity")
    }

    @Throws(Exception::class)
    fun createMaterialUniform(uniformName: String) {
        createUniform("$uniformName.ambient")
        createUniform("$uniformName.diffuse")
        createUniform("$uniformName.specular")
        createUniform("$uniformName.hasTexture")
        createUniform("$uniformName.hasNormalMap")
        createUniform("$uniformName.reflectance")
    }

    @Throws(Exception::class)
    fun createFogUniform(uniformName: String) {
        createUniform("$uniformName.activeFog")
        createUniform("$uniformName.colour")
        createUniform("$uniformName.density")
    }

    fun setUniform(uniformName: String, value: Matrix4f) {
        // Dump the matrix into a float buffer
        MemoryStack.stackPush().use { stack ->
            GL20.glUniformMatrix4fv(
                uniforms[uniformName]!!, false,
                value[stack.mallocFloat(16)]
            )
        }
    }

    fun setUniform(uniformName: String, value: Matrix4f, index: Int) {
        setUniform("$uniformName[$index]", value)
    }

    fun setUniform(uniformName: String, matrices: Array<Matrix4f?>?) {
        MemoryStack.stackPush().use { stack ->
            val length = matrices?.size ?: 0
            val fb = stack.mallocFloat(16 * length)
            for (i in 0 until length) {
                matrices!![i]!![16 * i, fb]
            }
            GL20.glUniformMatrix4fv(uniforms[uniformName]!!, false, fb)
        }
    }

    fun setUniform(uniformName: String, value: Int) {
        GL20.glUniform1i(uniforms[uniformName]!!, value)
    }

    fun setUniform(uniformName: String, value: Float) {
        GL20.glUniform1f(uniforms[uniformName]!!, value)
    }

    fun setUniform(uniformName: String, value: Float, index: Int) {
        setUniform("$uniformName[$index]", value)
    }

    fun setUniform(uniformName: String, value: Vector3f?) {
        GL20.glUniform3f(uniforms[uniformName]!!, value!!.x, value.y, value.z)
    }

    fun setUniform(uniformName: String, value: Vector4f?) {
        GL20.glUniform4f(uniforms[uniformName]!!, value!!.x, value.y, value.z, value.w)
    }

    fun setUniform(uniformName: String, pointLights: Array<PointLight>?) {
        pointLights?.forEachIndexed { index, light ->
            setUniform(uniformName, light, index)
        }
    }

    fun setUniform(uniformName: String, pointLight: PointLight, pos: Int) {
        setUniform("$uniformName[$pos]", pointLight)
    }

    fun setUniform(uniformName: String, pointLight: PointLight) {
        setUniform("$uniformName.colour", pointLight.color)
        setUniform("$uniformName.position", pointLight.position)
        setUniform("$uniformName.intensity", pointLight.intensity)
        val att = pointLight.attenuation
        setUniform("$uniformName.att.constant", att.constant)
        setUniform("$uniformName.att.linear", att.linear)
        setUniform("$uniformName.att.exponent", att.exponent)
    }

    fun setUniform(uniformName: String, spotLights: Array<SpotLight>?) {
        spotLights?.forEachIndexed { index, spotLight ->
            setUniform(uniformName, spotLight, index)
        }
    }

    fun setUniform(uniformName: String, spotLight: SpotLight, pos: Int) {
        setUniform("$uniformName[$pos]", spotLight)
    }

    fun setUniform(uniformName: String, spotLight: SpotLight) {
        setUniform("$uniformName.pl", spotLight.pointLight)
        setUniform("$uniformName.conedir", spotLight.coneDirection)
        setUniform("$uniformName.cutoff", spotLight.cutOff)
    }

    fun setUniform(uniformName: String, dirLight: DirectionalLight) {
        setUniform("$uniformName.colour", dirLight.color)
        setUniform("$uniformName.direction", dirLight.direction)
        setUniform("$uniformName.intensity", dirLight.intensity)
    }

    fun setUniform(uniformName: String, material: Material) {
        setUniform("$uniformName.ambient", material.ambientColour)
        setUniform("$uniformName.diffuse", material.diffuseColour)
        setUniform("$uniformName.specular", material.specularColour)
        setUniform("$uniformName.hasTexture", if (material.isTextured) 1 else 0)
        setUniform("$uniformName.hasNormalMap", if (material.hasNormalMap()) 1 else 0)
        setUniform("$uniformName.reflectance", material.reflectance)
    }

    fun setUniform(uniformName: String, fog: Fog) {
        setUniform("$uniformName.activeFog", if (fog.isActive) 1 else 0)
        setUniform("$uniformName.colour", fog.colour)
        setUniform("$uniformName.density", fog.density)
    }

    @Throws(Exception::class)
    fun createVertexShader(shaderCode: String) {
        vertexShaderId = createShader(shaderCode, GL20.GL_VERTEX_SHADER)
    }

    @Throws(Exception::class)
    fun createFragmentShader(shaderCode: String) {
        fragmentShaderId = createShader(shaderCode, GL20.GL_FRAGMENT_SHADER)
    }

    @Throws(Exception::class)
    protected fun createShader(shaderCode: String, shaderType: Int): Int {
        val shaderId = GL20.glCreateShader(shaderType)
        if (shaderId == 0) {
            throw Exception("Error creating shader. Type: $shaderType")
        }
        GL20.glShaderSource(shaderId, shaderCode)
        GL20.glCompileShader(shaderId)
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw Exception("Error compiling Shader code: " + GL20.glGetShaderInfoLog(shaderId, 1024))
        }
        GL20.glAttachShader(programId, shaderId)
        return shaderId
    }

    @Throws(Exception::class)
    fun link() {
        GL20.glLinkProgram(programId)
        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw Exception("Error linking Shader code: " + GL20.glGetProgramInfoLog(programId, 1024))
        }
        if (vertexShaderId != 0) {
            GL20.glDetachShader(programId, vertexShaderId)
        }
//        if (geometryShaderId != 0) {
//            GL20.glDetachShader(programId, geometryShaderId)
//        }
        if (fragmentShaderId != 0) {
            GL20.glDetachShader(programId, fragmentShaderId)
        }
        GL20.glValidateProgram(programId)
        if (GL20.glGetProgrami(programId, GL20.GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating Shader code: " + GL20.glGetProgramInfoLog(programId, 1024))
        }
    }

    fun bind() {
        GL20.glUseProgram(programId)
    }

    fun unbind() {
        GL20.glUseProgram(0)
    }

    fun cleanup() {
        unbind()
        if (programId != 0) {
            GL20.glDeleteProgram(programId)
        }
    }
}
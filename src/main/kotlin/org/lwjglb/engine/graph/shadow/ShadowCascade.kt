package org.lwjglb.engine.graph.shadow

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjglb.engine.Window
import org.lwjglb.engine.graph.Transformation
import org.lwjglb.engine.graph.lights.DirectionalLight
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min

class ShadowCascade(private val zNear: Float, private val zFar: Float) {

    private val projectionViewMatrix: Matrix4f = Matrix4f()
    val orthoProjectionMatrix: Matrix4f = Matrix4f()
    val lightViewMatrix: Matrix4f = Matrix4f()

    /**
     * Center of the view cuboid un world space coordinates.
     */
    private val centroid: Vector3f = Vector3f()
    private val frustumCorners: Array<Vector3f?>
    private val tmpVec: Vector4f

    fun update(window: Window, viewMatrix: Matrix4f, light: DirectionalLight) {
        // Build projection view matrix for this cascade
        val aspectRatio = window.width.toFloat() / window.height.toFloat()
        projectionViewMatrix.setPerspective(Window.FOV, aspectRatio, zNear, zFar)
        projectionViewMatrix.mul(viewMatrix)

        // Calculate frustum corners in world space
        var maxZ = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        for (i in 0 until FRUSTUM_CORNERS) {
            val corner = frustumCorners[i]
            corner!![0f, 0f] = 0f
            projectionViewMatrix.frustumCorner(i, corner)
            centroid.add(corner)
            centroid.div(8.0f)
            minZ = min(minZ, corner.z)
            maxZ = max(maxZ, corner.z)
        }

        // Go back from the centroid up to max.z - min.z in the direction of light
        val lightDirection = light.direction
        val lightPosInc = Vector3f().set(lightDirection)
        val distance = maxZ - minZ
        lightPosInc.mul(distance)
        val lightPosition = Vector3f()
        lightPosition.set(centroid)
        lightPosition.add(lightPosInc)
        updateLightViewMatrix(lightDirection, lightPosition)
        updateLightProjectionMatrix()
    }

    private fun updateLightViewMatrix(lightDirection: Vector3f?, lightPosition: Vector3f) {
        val lightAngleX =
            Math.toDegrees(acos(lightDirection!!.z.toDouble())).toFloat()
        val lightAngleY =
            Math.toDegrees(asin(lightDirection.x.toDouble())).toFloat()
        val lightAngleZ = 0f
        Transformation.updateGenericViewMatrix(
            lightPosition,
            Vector3f(lightAngleX, lightAngleY, lightAngleZ),
            lightViewMatrix
        )
    }

    private fun updateLightProjectionMatrix() {
        // Now calculate frustum dimensions in light space
        var minX = Float.MAX_VALUE
        var maxX = -Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MIN_VALUE
        for (i in 0 until FRUSTUM_CORNERS) {
            val corner = frustumCorners[i]
            tmpVec[corner] = 1f
            tmpVec.mul(lightViewMatrix)
            minX = min(tmpVec.x, minX)
            maxX = max(tmpVec.x, maxX)
            minY = min(tmpVec.y, minY)
            maxY = max(tmpVec.y, maxY)
            minZ = min(tmpVec.z, minZ)
            maxZ = max(tmpVec.z, maxZ)
        }
        val distZ = maxZ - minZ
        orthoProjectionMatrix.setOrtho(minX, maxX, minY, maxY, 0f, distZ)
    }

    companion object {
        private const val FRUSTUM_CORNERS = 8
    }

    init {
        frustumCorners = arrayOfNulls(FRUSTUM_CORNERS)
        for (i in 0 until FRUSTUM_CORNERS) {
            frustumCorners[i] = Vector3f()
        }
        tmpVec = Vector4f()
    }
}
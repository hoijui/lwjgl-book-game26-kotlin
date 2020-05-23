package org.lwjglb.engine.graph

import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

class Camera(val position: Vector3f = Vector3f(), val rotation: Vector3f = Vector3f()) {

    var viewMatrix: Matrix4f = Matrix4f()
        private set

    fun setPosition(x: Float, y: Float, z: Float) {
        position.x = x
        position.y = y
        position.z = z
    }

    fun updateViewMatrix(): Matrix4f {
        return Transformation.updateGenericViewMatrix(position, rotation, viewMatrix)
    }

    fun movePosition(offsetX: Float, offsetY: Float, offsetZ: Float) {
        if (offsetZ != 0f) {
            position.x += sin(Math.toRadians(rotation.y.toDouble())).toFloat() * -1.0f * offsetZ
            position.z += cos(Math.toRadians(rotation.y.toDouble())).toFloat() * offsetZ
        }
        if (offsetX != 0f) {
            position.x += sin(Math.toRadians(rotation.y - 90.toDouble())).toFloat() * -1.0f * offsetX
            position.z += cos(Math.toRadians(rotation.y - 90.toDouble())).toFloat() * offsetX
        }
        position.y += offsetY
    }

    fun setRotation(x: Float, y: Float, z: Float) {
        rotation.x = x
        rotation.y = y
        rotation.z = z
    }

    fun moveRotation(offsetX: Float, offsetY: Float, offsetZ: Float) {
        rotation.x += offsetX
        rotation.y += offsetY
        rotation.z += offsetZ
    }
}

package org.lwjglb.engine.loaders.md5

import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.sqrt

object MD5Utils {
    const val FLOAT_REGEXP = "[+-]?\\d*\\.?\\d*"
    const val VECTOR3_REGEXP =
        "\\(\\s*($FLOAT_REGEXP)\\s*($FLOAT_REGEXP)\\s*($FLOAT_REGEXP)\\s*\\)"

    fun calculateQuaternion(vec: Vector3f): Quaternionf {
        return calculateQuaternion(vec.x, vec.y, vec.z)
    }

    fun calculateQuaternion(x: Float, y: Float, z: Float): Quaternionf {
        val orientation = Quaternionf(x.toDouble(), y.toDouble(), z.toDouble(), 0.0)
        val temp =
            1.0f - orientation.x * orientation.x - orientation.y * orientation.y - orientation.z * orientation.z
        if (temp < 0.0f) {
            orientation.w = 0.0f
        } else {
            orientation.w = (-sqrt(temp.toDouble())).toFloat()
        }
        return orientation
    }
}
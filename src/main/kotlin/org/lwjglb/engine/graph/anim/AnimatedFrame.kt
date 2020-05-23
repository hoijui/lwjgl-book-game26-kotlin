package org.lwjglb.engine.graph.anim

import org.joml.Matrix4f
import java.util.*

class AnimatedFrame {
    val localJointMatrices: Array<Matrix4f?>
    val jointMatrices: Array<Matrix4f?>

    fun setMatrix(pos: Int, localJointMatrix: Matrix4f?, invJointMatrix: Matrix4f?) {
        localJointMatrices[pos] = localJointMatrix
        val mat = Matrix4f(localJointMatrix)
        mat.mul(invJointMatrix)
        jointMatrices[pos] = mat
    }

    companion object {
        const val MAX_JOINTS = 150
        private val IDENTITY_MATRIX = Matrix4f()
    }

    init {
        localJointMatrices = arrayOfNulls(MAX_JOINTS)
        Arrays.fill(localJointMatrices, IDENTITY_MATRIX)
        jointMatrices = arrayOfNulls(MAX_JOINTS)
        Arrays.fill(jointMatrices, IDENTITY_MATRIX)
    }
}
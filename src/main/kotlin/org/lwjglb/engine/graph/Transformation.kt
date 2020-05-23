package org.lwjglb.engine.graph

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjglb.engine.items.GameItem

class Transformation {

    private val modelMatrix: Matrix4f = Matrix4f()
    private val modelViewMatrix: Matrix4f = Matrix4f()
    private val modelLightViewMatrix: Matrix4f = Matrix4f()
    private val lightViewMatrix: Matrix4f = Matrix4f()
    private val ortho2DMatrix: Matrix4f = Matrix4f()
    private val orthoModelMatrix: Matrix4f = Matrix4f()

    fun getLightViewMatrix(): Matrix4f {
        return lightViewMatrix
    }

    fun setLightViewMatrix(lightViewMatrix: Matrix4f) {
        this.lightViewMatrix.set(lightViewMatrix)
    }

    fun updateLightViewMatrix(position: Vector3f, rotation: Vector3f): Matrix4f {
        return updateGenericViewMatrix(position, rotation, lightViewMatrix)
    }

    fun getOrtho2DProjectionMatrix(
        left: Float,
        right: Float,
        bottom: Float,
        top: Float)
            : Matrix4f
    {
        return ortho2DMatrix.setOrtho2D(left, right, bottom, top)
    }

    fun buildModelMatrix(gameItem: GameItem): Matrix4f {
        val rotation = gameItem.rotation
        return modelMatrix.translationRotateScale(
            gameItem.position.x, gameItem.position.y, gameItem.position.z,
            rotation.x, rotation.y, rotation.z, rotation.w,
            gameItem.scale, gameItem.scale, gameItem.scale
        )
    }

    fun buildModelViewMatrix(gameItem: GameItem, viewMatrix: Matrix4f): Matrix4f {
        return buildModelViewMatrix(buildModelMatrix(gameItem), viewMatrix)
    }

    fun buildModelViewMatrix(modelMatrix: Matrix4f, viewMatrix: Matrix4f): Matrix4f {
        return viewMatrix.mulAffine(modelMatrix, modelViewMatrix)
    }

    fun buildModelLightViewMatrix(gameItem: GameItem, lightViewMatrix: Matrix4f): Matrix4f {
        return buildModelViewMatrix(buildModelMatrix(gameItem), lightViewMatrix)
    }

    fun buildModelLightViewMatrix(modelMatrix: Matrix4f, lightViewMatrix: Matrix4f): Matrix4f {
        return lightViewMatrix.mulAffine(modelMatrix, modelLightViewMatrix)
    }

    fun buildOrthoProjModelMatrix(gameItem: GameItem, orthoMatrix: Matrix4f): Matrix4f {
        return orthoMatrix.mulOrthoAffine(buildModelMatrix(gameItem), orthoModelMatrix)
    }

    companion object {
        fun updateGenericViewMatrix(position: Vector3f, rotation: Vector3f, matrix: Matrix4f): Matrix4f {
            // First do the rotation so camera rotates over its position
            return matrix.rotationX(Math.toRadians(rotation.x.toDouble()).toFloat())
                .rotateY(Math.toRadians(rotation.y.toDouble()).toFloat())
                .translate(-position.x, -position.y, -position.z)
        }
    }
}
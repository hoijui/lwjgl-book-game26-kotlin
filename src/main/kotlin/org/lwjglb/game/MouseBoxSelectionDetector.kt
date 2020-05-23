package org.lwjglb.game

import org.joml.Matrix4f
import org.joml.Vector2d
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjglb.engine.Window
import org.lwjglb.engine.graph.Camera
import org.lwjglb.engine.items.GameItem

class MouseBoxSelectionDetector : CameraBoxSelectionDetector() {

    private val invProjectionMatrix: Matrix4f = Matrix4f()
    private val invViewMatrix: Matrix4f = Matrix4f()
    private val mouseDir: Vector3f = Vector3f()
    private val tmpVec: Vector4f = Vector4f()

    fun selectGameItem(
        gameItems: Collection<GameItem>,
        window: Window,
        mousePos: Vector2d?,
        camera: Camera
    ): Boolean {
        // Transform mouse coordinates into normalized spaze [-1, 1]
        val wdwWitdh = window.width
        val wdwHeight = window.height
        val x = (2 * mousePos!!.x).toFloat() / wdwWitdh.toFloat() - 1.0f
        val y = 1.0f - (2 * mousePos.y).toFloat() / wdwHeight.toFloat()
        val z = -1.0f
        invProjectionMatrix.set(window.projectionMatrix)
        invProjectionMatrix.invert()
        tmpVec[x, y, z] = 1.0f
        tmpVec.mul(invProjectionMatrix)
        tmpVec.z = -1.0f
        tmpVec.w = 0.0f
        val viewMatrix = camera.viewMatrix
        invViewMatrix.set(viewMatrix)
        invViewMatrix.invert()
        tmpVec.mul(invViewMatrix)
        mouseDir[tmpVec.x, tmpVec.y] = tmpVec.z
        return selectGameItem(gameItems, camera.position, mouseDir)
    }
}
package org.lwjglb.game

import org.joml.Intersectionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjglb.engine.graph.Camera
import org.lwjglb.engine.items.GameItem

open class CameraBoxSelectionDetector {

    private val max: Vector3f = Vector3f()
    private val min: Vector3f = Vector3f()
    private val nearFar: Vector2f = Vector2f()
    private var dir: Vector3f = Vector3f()

    fun selectGameItem(gameItems: Collection<GameItem>, camera: Camera) {
        dir = camera.viewMatrix.positiveZ(dir).negate()
        selectGameItem(gameItems, camera.position, dir)
    }

    protected fun selectGameItem(gameItems: Collection<GameItem>, center: Vector3f?, dir: Vector3f?): Boolean {
        var selected = false
        var selectedGameItem: GameItem? = null
        var closestDistance = Float.POSITIVE_INFINITY
        for (gameItem in gameItems) {
            gameItem.isSelected = false
            min.set(gameItem.position)
            max.set(gameItem.position)
            min.add(-gameItem.scale, -gameItem.scale, -gameItem.scale)
            max.add(gameItem.scale, gameItem.scale, gameItem.scale)
            if (Intersectionf.intersectRayAab(center, dir, min, max, nearFar) && nearFar.x < closestDistance) {
                closestDistance = nearFar.x
                selectedGameItem = gameItem
            }
        }
        if (selectedGameItem != null) {
            selectedGameItem.isSelected = true
            selected = true
        }
        return selected
    }
}
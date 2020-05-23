package org.lwjglb.engine.graph

import org.joml.FrustumIntersection
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjglb.engine.items.GameItem

class FrustumCullingFilter {

    private val prjViewMatrix: Matrix4f = Matrix4f()
    private val frustumInt: FrustumIntersection = FrustumIntersection()

    fun updateFrustum(projMatrix: Matrix4f, viewMatrix: Matrix4f) {
        // Calculate projection view matrix
        prjViewMatrix.set(projMatrix)
        prjViewMatrix.mul(viewMatrix)
        // Update frustum intersection class
        frustumInt.set(prjViewMatrix)
    }

    fun filter(mapMesh: Map<out Mesh, List<GameItem>?>?) {
        for ((key, gameItems) in mapMesh!!) {
            filter(gameItems, key.boundingRadius)
        }
    }

    fun filter(gameItems: List<GameItem>?, meshBoundingRadius: Float) {
        var boundingRadius: Float
        var pos: Vector3f
        for (gameItem in gameItems!!) {
            if (!gameItem.isDisableFrustumCulling) {
                boundingRadius = gameItem.scale * meshBoundingRadius
                pos = gameItem.position
                gameItem.isInsideFrustum = insideFrustum(pos.x, pos.y, pos.z, boundingRadius)
            }
        }
    }

    fun insideFrustum(x0: Float, y0: Float, z0: Float, boundingRadius: Float): Boolean {
        return frustumInt.testSphere(x0, y0, z0, boundingRadius)
    }
}
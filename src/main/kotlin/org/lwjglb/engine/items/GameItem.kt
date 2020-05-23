package org.lwjglb.engine.items

import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjglb.engine.graph.Mesh

open class GameItem(
    var meshes: MutableList<Mesh>,
    val position: Vector3f = Vector3f(),
    val rotation: Quaternionf = Quaternionf(),
    var scale: Float = 1.0f)
{
    var isSelected = false
    var textPos: Int = 0
    var isDisableFrustumCulling: Boolean = false
    var isInsideFrustum: Boolean = true

    constructor(base: GameItem)
            : this(base.meshes, base.position, base.rotation, base.scale)

    constructor(mesh: Mesh)
            : this(mutableListOf(mesh))

    var mesh: Mesh
        get() = meshes[0]
        set(mesh) {
            meshes.clear()
            meshes.add(mesh)
        }

    fun setPosition(x: Float, y: Float, z: Float) {
        position.x = x
        position.y = y
        position.z = z
    }

    fun setRotation(q: Quaternionf) {
        rotation.set(q)
    }

    fun cleanup() {
        meshes.forEach { mesh ->
            mesh.cleanUp()
        }
    }
}
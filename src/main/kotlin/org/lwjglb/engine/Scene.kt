package org.lwjglb.engine

import org.lwjglb.engine.graph.InstancedMesh
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.graph.particles.IParticleEmitter
import org.lwjglb.engine.graph.weather.Fog
import org.lwjglb.engine.items.GameItem
import org.lwjglb.engine.items.SkyBox
import java.util.*

class Scene(val sceneLight: SceneLight) {

    private val meshMap: MutableMap<Mesh, MutableList<GameItem>> = HashMap()
    private val instancedMeshMap: MutableMap<InstancedMesh, MutableList<GameItem>>
            = HashMap()
    var skyBox: SkyBox? = null
    var fog: Fog = Fog.NOFOG
    var isRenderShadows: Boolean = true
    var particleEmitters: Array<IParticleEmitter>? = null
    val gameMeshes: Map<Mesh, List<GameItem>>
        get() = meshMap
    val gameInstancedMeshes: Map<InstancedMesh, List<GameItem>>
        get() = instancedMeshMap

    fun setGameItems(gameItems: List<GameItem>) {
        // Create a map of meshes to speed up rendering
        for (gameItem in gameItems) {
            val meshes = gameItem.meshes
            for (mesh in meshes) {
                val instancedMesh = mesh is InstancedMesh
                var list: MutableList<GameItem>? =
                    if (instancedMesh) instancedMeshMap[mesh] else meshMap[mesh]
                if (list == null) {
                    list = ArrayList()
                    if (instancedMesh) {
                        instancedMeshMap[mesh as InstancedMesh] = list
                    } else {
                        meshMap[mesh] = list
                    }
                }
                list.add(gameItem)
            }
        }
    }

    fun cleanup() {
        for (mesh in meshMap.keys) {
            mesh.cleanUp()
        }
        for (mesh in instancedMeshMap.keys) {
            mesh.cleanUp()
        }
        if (particleEmitters != null) {
            for (particleEmitter in particleEmitters!!) {
                particleEmitter.cleanup()
            }
        }
    }
}
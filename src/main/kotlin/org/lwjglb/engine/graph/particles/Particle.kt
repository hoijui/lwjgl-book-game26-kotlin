package org.lwjglb.engine.graph.particles

import org.joml.Vector3f
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.items.GameItem

class Particle : GameItem {

    var updateTextureMillis: Long
        private set
    private var currentAnimTimeMillis: Long
    var speed: Vector3f

    /**
     * Time to live for particle in milliseconds.
     */
    var ttl: Long
        private set
    var animFrames: Int = -1
        private set

    constructor(mesh: Mesh, speed: Vector3f?, ttl: Long, updateTextureMillis: Long)
            : super(mesh)
    {
        this.speed = Vector3f(speed)
        this.ttl = ttl
        this.updateTextureMillis = updateTextureMillis
        currentAnimTimeMillis = 0
        mesh.material.texture?.let { texture ->
            animFrames = texture.numCols * texture.numRows
        }
    }

    constructor(baseParticle: Particle) : super(baseParticle) {
        speed = Vector3f(baseParticle.speed)
        ttl = baseParticle.ttl
        updateTextureMillis = baseParticle.updateTextureMillis
        currentAnimTimeMillis = 0
        animFrames = baseParticle.animFrames
    }

    fun setUpdateTextureMills(updateTextureMillis: Long) {
        this.updateTextureMillis = updateTextureMillis
    }

    /**
     * Updates the Particle's TTL
     * @param elapsedTime Elapsed Time in milliseconds
     * @return The Particle's TTL
     */
    fun updateTtl(elapsedTime: Long): Long {
        ttl -= elapsedTime
        currentAnimTimeMillis += elapsedTime
        if (currentAnimTimeMillis >= updateTextureMillis && animFrames > 0) {
            currentAnimTimeMillis = 0
            var pos = textPos
            pos++
            textPos = if (pos < animFrames) {
                pos
            } else {
                0
            }
        }
        return ttl
    }
}
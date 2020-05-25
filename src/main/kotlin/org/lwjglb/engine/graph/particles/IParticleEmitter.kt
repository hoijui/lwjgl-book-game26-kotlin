package org.lwjglb.engine.graph.particles

import org.lwjglb.engine.items.GameItem

interface IParticleEmitter {

    val baseParticle: Particle
    val particles: List<GameItem>

    fun cleanup()
}

package org.lwjglb.engine.graph.particles

import org.joml.Vector3f
import org.lwjglb.engine.items.GameItem
import java.util.*

class FlowParticleEmitter(baseParticle: Particle, maxParticles: Int, creationPeriodMillis: Long) :
    IParticleEmitter {
    var maxParticles: Int
    var active: Boolean
    override val particles: MutableList<GameItem>
    override val baseParticle: Particle
    var creationPeriodMillis: Long
    private var lastCreationTime: Long
    var speedRndRange = 0f
    var positionRndRange = 0f
    var scaleRndRange = 0f
    private var animRange: Long = 0

//    override fun getParticles(): List<GameItem> {
//        return particles
//    }

    fun setAnimRange(animRange: Long) {
        this.animRange = animRange
    }

    fun update(elapsedTime: Long) {
        val now = System.currentTimeMillis()
        if (lastCreationTime == 0L) {
            lastCreationTime = now
        }
        val it = particles.iterator()
        while (it.hasNext()) {
            val particle = it.next() as Particle
            if (particle.updateTtl(elapsedTime) < 0) {
                it.remove()
            } else {
                updatePosition(particle, elapsedTime)
            }
        }
        val length = particles.size
        if (now - lastCreationTime >= creationPeriodMillis && length < maxParticles) {
            createParticle()
            lastCreationTime = now
        }
    }

    private fun createParticle() {
        val particle = Particle(baseParticle)
        // Add a little bit of randomness of the parrticle
        val sign = if (Math.random() > 0.5) -1.0f else 1.0f
        val speedInc = sign * Math.random().toFloat() * speedRndRange
        val posInc = sign * Math.random().toFloat() * positionRndRange
        val scaleInc = sign * Math.random().toFloat() * scaleRndRange
        val updateAnimInc = sign.toLong() * (Math.random() * animRange.toFloat()).toLong()
        particle.position.add(posInc, posInc, posInc)
        particle.speed.add(speedInc, speedInc, speedInc)
        particle.scale = particle.scale + scaleInc
        particle.setUpdateTextureMills(particle.updateTextureMillis + updateAnimInc)
        particles.add(particle)
    }

    /**
     * Updates a particle position
     * @param particle The particle to update
     * @param elapsedTime Elapsed time in milliseconds
     */
    fun updatePosition(particle: Particle, elapsedTime: Long) {
        val speed = particle.speed
        val delta = elapsedTime / 1000.0f
        val dist = Vector3f(
                speed.x * delta,
                speed.y * delta,
                speed.z * delta)
        particle.position.set(particle.position.add(dist))
    }

    override fun cleanup() {
        for (particle in particles) {
            particle.cleanup()
        }
    }

    init {
        particles = ArrayList()
        this.baseParticle = baseParticle
        this.maxParticles = maxParticles
        active = false
        lastCreationTime = 0
        this.creationPeriodMillis = creationPeriodMillis
    }
}
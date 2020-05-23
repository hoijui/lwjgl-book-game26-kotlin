package org.lwjglb.engine.graph.lights

import org.joml.Vector3f

class PointLight(
        var color: Vector3f,
        var position: Vector3f,
        var intensity: Float,
        var attenuation: Attenuation = Attenuation(1.0f, 0.0f, 0.0f))
{
    constructor(base: PointLight)
            : this(
            Vector3f(base.color),
            Vector3f(base.position),
            base.intensity,
            base.attenuation)

    data class Attenuation(var constant: Float, var linear: Float, var exponent: Float)
}
package org.lwjglb.engine.graph.lights

import org.joml.Vector3f
import kotlin.math.cos

class SpotLight(var pointLight: PointLight, var coneDirection: Vector3f, cutOffAngle: Float) {

    var cutOff = 0f

    init {
        setCutOffAngle(cutOffAngle)
    }

    constructor(base: SpotLight)
            : this(
            PointLight(base.pointLight),
            Vector3f(base.coneDirection), 0f)
    {
        cutOff = base.cutOff
    }

    private fun setCutOffAngle(cutOffAngle: Float) {
        cutOff = cos(Math.toRadians(cutOffAngle.toDouble())).toFloat()
    }
}
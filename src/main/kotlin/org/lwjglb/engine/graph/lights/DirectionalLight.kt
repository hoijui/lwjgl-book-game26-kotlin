package org.lwjglb.engine.graph.lights

import org.joml.Vector3f

class DirectionalLight(var color: Vector3f, var direction: Vector3f, var intensity: Float) {

    constructor(base: DirectionalLight)
            : this(
            Vector3f(base.color),
            Vector3f(base.direction),
            base.intensity)

    class OrthoCoords {
        var left = 0f
        var right = 0f
        var bottom = 0f
        var top = 0f
        var near = 0f
        var far = 0f
    }
}
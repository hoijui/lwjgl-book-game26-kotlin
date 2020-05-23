package org.lwjglb.engine.graph.weather

import org.joml.Vector3f

class Fog {
    /**
     * @return the active
     */
    /**
     * @param active the active to set
     */
    var isActive: Boolean
    /**
     * @return the color
     */
    /**
     * @param colour the color to set
     */
    var colour: Vector3f
    /**
     * @return the density
     */
    /**
     * @param density the density to set
     */
    var density: Float

    constructor() {
        isActive = false
        colour = Vector3f()
        density = 0f
    }

    constructor(active: Boolean, colour: Vector3f, density: Float) {
        this.colour = colour
        this.density = density
        isActive = active
    }

    companion object {
        var NOFOG = Fog()
    }
}
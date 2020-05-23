package org.lwjglb.engine.graph.anim

import org.joml.Vector2f
import org.joml.Vector3f
import java.util.Arrays

class AnimVertex(numWeights: Int, var position: Vector3f, var textCoordinates: Vector2f) {

    var normal = Vector3f()
    var weights = FloatArray(numWeights)
    var jointIndices = IntArray(numWeights)

    init {
        Arrays.fill(jointIndices, -1)
        Arrays.fill(weights, -1f)
    }
}
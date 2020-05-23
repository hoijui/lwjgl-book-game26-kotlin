package org.lwjglb.engine.graph.anim

import org.joml.Matrix4f
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.items.GameItem

class AnimGameItem(
    meshes: MutableList<Mesh>,
    private var frames: List<AnimatedFrame>,
    val invJointMatrices: List<Matrix4f>)
    : GameItem(meshes)
{
    private var currentFrame = 0

    fun getCurrentAnimatedFrame(): AnimatedFrame {
        return frames[currentFrame]
    }

    val nextFrame: AnimatedFrame
        get() {
            var nextFrame = currentFrame + 1
            if (nextFrame > frames.size - 1) {
                nextFrame = 0
            }
            return frames[nextFrame]
        }

    fun nextFrame() {
        val nextFrame = currentFrame + 1
        currentFrame = if (nextFrame > frames.size - 1) {
            0
        } else {
            nextFrame
        }
    }
}
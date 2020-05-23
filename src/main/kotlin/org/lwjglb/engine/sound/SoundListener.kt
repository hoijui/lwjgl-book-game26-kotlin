package org.lwjglb.engine.sound

import org.joml.Vector3f
import org.lwjgl.openal.AL10

class SoundListener @JvmOverloads constructor(position: Vector3f = Vector3f()) {
    fun setSpeed(speed: Vector3f) {
        AL10.alListener3f(AL10.AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setPosition(position: Vector3f?) {
        AL10.alListener3f(AL10.AL_POSITION, position!!.x, position.y, position.z)
    }

    fun setOrientation(at: Vector3f, up: Vector3f) {
        val data = FloatArray(6)
        data[0] = at.x
        data[1] = at.y
        data[2] = at.z
        data[3] = up.x
        data[4] = up.y
        data[5] = up.z
        AL10.alListenerfv(AL10.AL_ORIENTATION, data)
    }

    init {
        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z)
        AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f)
    }
}
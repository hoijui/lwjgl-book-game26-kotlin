package org.lwjglb.engine.sound

import org.joml.Vector3f
import org.lwjgl.openal.AL10

class SoundSource(loop: Boolean, relative: Boolean) {

    private val sourceId: Int = AL10.alGenSources()

    init {
        if (loop) {
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_TRUE)
        }
        if (relative) {
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE)
        }
    }

    fun setBuffer(bufferId: Int) {
        stop()
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId)
    }

    fun setPosition(position: Vector3f?) {
        AL10.alSource3f(sourceId, AL10.AL_POSITION, position!!.x, position.y, position.z)
    }

    fun setSpeed(speed: Vector3f) {
        AL10.alSource3f(sourceId, AL10.AL_VELOCITY, speed.x, speed.y, speed.z)
    }

    fun setGain(gain: Float) {
        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain)
    }

    fun setProperty(param: Int, value: Float) {
        AL10.alSourcef(sourceId, param, value)
    }

    fun play() {
        AL10.alSourcePlay(sourceId)
    }

    val isPlaying: Boolean
        get() = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING

    fun pause() {
        AL10.alSourcePause(sourceId)
    }

    fun stop() {
        AL10.alSourceStop(sourceId)
    }

    fun cleanup() {
        stop()
        AL10.alDeleteSources(sourceId)
    }
}
package org.lwjglb.engine.sound

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import org.lwjgl.system.MemoryUtil
import org.lwjglb.engine.graph.Camera
import org.lwjglb.engine.graph.Transformation
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*

class SoundManager {
    private var device: Long = 0
    private var context: Long = 0
    var listener: SoundListener? = null
    private val soundBufferList: MutableList<SoundBuffer>
    private val soundSourceMap: MutableMap<String, SoundSource>
    private val cameraMatrix: Matrix4f

    @Throws(Exception::class)
    fun init() {
        device = ALC10.alcOpenDevice(null as ByteBuffer?)
        check(device != MemoryUtil.NULL) { "Failed to open the default OpenAL device." }
        val deviceCaps = ALC.createCapabilities(device)
        context = ALC10.alcCreateContext(device, null as IntBuffer?)
        check(context != MemoryUtil.NULL) { "Failed to create OpenAL context." }
        ALC10.alcMakeContextCurrent(context)
        AL.createCapabilities(deviceCaps)
    }

    fun addSoundSource(name: String, soundSource: SoundSource) {
        soundSourceMap[name] = soundSource
    }

    fun getSoundSource(name: String): SoundSource? {
        return soundSourceMap[name]
    }

    fun playSoundSource(name: String) {
        val soundSource = soundSourceMap[name]
        if (soundSource != null && !soundSource.isPlaying) {
            soundSource.play()
        }
    }

    fun removeSoundSource(name: String) {
        soundSourceMap.remove(name)
    }

    fun addSoundBuffer(soundBuffer: SoundBuffer) {
        soundBufferList.add(soundBuffer)
    }

    fun updateListenerPosition(camera: Camera) {
        // Update camera matrix with camera data
        Transformation.Companion.updateGenericViewMatrix(camera.position, camera.rotation, cameraMatrix)
        listener!!.setPosition(camera.position)
        val at = Vector3f()
        cameraMatrix.positiveZ(at).negate()
        val up = Vector3f()
        cameraMatrix.positiveY(up)
        listener!!.setOrientation(at, up)
    }

    fun setAttenuationModel(model: Int) {
        AL10.alDistanceModel(model)
    }

    fun cleanup() {
        for (soundSource in soundSourceMap.values) {
            soundSource.cleanup()
        }
        soundSourceMap.clear()
        for (soundBuffer in soundBufferList) {
            soundBuffer.cleanup()
        }
        soundBufferList.clear()
        if (context != MemoryUtil.NULL) {
            ALC10.alcDestroyContext(context)
        }
        if (device != MemoryUtil.NULL) {
            ALC10.alcCloseDevice(device)
        }
    }

    init {
        soundBufferList = ArrayList()
        soundSourceMap = HashMap()
        cameraMatrix = Matrix4f()
    }
}
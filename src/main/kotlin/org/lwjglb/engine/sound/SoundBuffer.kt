package org.lwjglb.engine.sound

import org.lwjgl.openal.AL10
import org.lwjgl.stb.STBVorbis
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjglb.engine.Utils
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class SoundBuffer(file: String) {
    val bufferId: Int
    private var pcm: ShortBuffer? = null
    private var vorbis: ByteBuffer? = null

    fun cleanup() {
        AL10.alDeleteBuffers(bufferId)
        if (pcm != null) {
            MemoryUtil.memFree(pcm)
        }
    }

    @Throws(Exception::class)
    private fun readVorbis(resource: String, bufferSize: Int, info: STBVorbisInfo): ShortBuffer? {
        MemoryStack.stackPush().use { stack ->
            vorbis = Utils.ioResourceToByteBuffer(resource, bufferSize)
            val error = stack.mallocInt(1)
            val decoder = STBVorbis.stb_vorbis_open_memory(vorbis!!, error, null)
            if (decoder == MemoryUtil.NULL) {
                throw RuntimeException("Failed to open Ogg Vorbis file. Error: " + error[0])
            }
            STBVorbis.stb_vorbis_get_info(decoder, info)
            val channels = info.channels()
            val lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder)
            pcm = MemoryUtil.memAllocShort(lengthSamples)
            pcm!!.limit(STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm!!) * channels)
            STBVorbis.stb_vorbis_close(decoder)
            return pcm
        }
    }

    init {
        bufferId = AL10.alGenBuffers()
        STBVorbisInfo.malloc().use { info ->
            val pcm = readVorbis(file, 32 * 1024, info)

            // Copy to buffer
            AL10.alBufferData(
                bufferId,
                if (info.channels() == 1) AL10.AL_FORMAT_MONO16 else AL10.AL_FORMAT_STEREO16,
                pcm!!,
                info.sample_rate()
            )
        }
    }
}
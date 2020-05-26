package org.lwjglb.engine

import org.lwjgl.BufferUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Utils {
    @Throws(Exception::class)
    fun loadResource(fileName: String): String {
        Utils::class.java.getResourceAsStream(fileName).use { res ->
            Scanner(res, StandardCharsets.UTF_8.name()).use { scanner ->
                return scanner.useDelimiter("\\A").next()
            }
        }
    }

    @Throws(Exception::class)
    fun readAllLines(fileName: String): List<String> {

        return BufferedReader(
                InputStreamReader(
                        Class.forName(Utils::class.java.name).getResourceAsStream(fileName)
                )
        )
                .lineSequence()
                .toCollection(ArrayList())
    }

    fun listIntToArray(list: List<Int>): IntArray {
        return list.stream().mapToInt { v: Int? -> v!! }.toArray()
    }

    fun listToArray(list: List<Float>?): FloatArray {
        val size = list?.size ?: 0
        val floatArr = FloatArray(size)
        for (i in 0 until size) {
            floatArr[i] = list!![i]
        }
        return floatArr
    }

    fun existsResourceFile(fileName: String): Boolean {
        try {
            Utils::class.java.getResourceAsStream(fileName).use { res ->
                return res != null
            }
        } catch (excp: Exception) {
            return false
        }
    }

    @Throws(IOException::class)
    fun ioResourceToByteBuffer(resource: String, bufferSize: Int): ByteBuffer {

        val path = Paths.get(resource)
        val buffer = if (Files.isReadable(path)) {
            Files.newByteChannel(path).use { fc ->
                val tmpBuff = BufferUtils.createByteBuffer(fc.size().toInt() + 1)
                while (fc.read(tmpBuff) != -1);
                tmpBuff
            }
        } else {
            Utils::class.java.getResourceAsStream(resource).use { source ->
                Channels.newChannel(source).use { rbc ->
                    var tmpBuff = BufferUtils.createByteBuffer(bufferSize)
                    while (true) {
                        val bytes = rbc.read(tmpBuff)
                        if (bytes == -1) {
                            break
                        }
                        if (tmpBuff.remaining() == 0) {
                            tmpBuff = resizeBuffer(tmpBuff, tmpBuff.capacity() * 2)
                        }
                    }
                    tmpBuff
                }
            }
        }
        buffer.flip()
        return buffer
    }

    private fun resizeBuffer(buffer: ByteBuffer, newCapacity: Int): ByteBuffer {
        val newBuffer = BufferUtils.createByteBuffer(newCapacity)
        buffer.flip()
        newBuffer.put(buffer)
        return newBuffer
    }
}

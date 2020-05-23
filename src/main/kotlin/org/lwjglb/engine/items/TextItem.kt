package org.lwjglb.engine.items

import org.lwjglb.engine.Utils
import org.lwjglb.engine.graph.FontTexture
import org.lwjglb.engine.graph.Material
import org.lwjglb.engine.graph.Mesh
import java.util.*

class TextItem(private var text: String, private val fontTexture: FontTexture)
    : GameItem(ArrayList())
{
    private fun buildMesh(): Mesh {
        val positions: MutableList<Float> = ArrayList()
        val textCoordinates: MutableList<Float> = ArrayList()
        val normals = FloatArray(0)
        val indices: MutableList<Int> = ArrayList()
        val characters = text.toCharArray()
        var startx = 0f
        characters.forEachIndexed { i, chr ->
            val charInfo = fontTexture.getCharInfo(chr) ?: throw IllegalStateException("No fontTexture found for '$chr'")

            // Build a character tile composed by two triangles

            // Left Top vertex
            positions.add(startx) // x
            positions.add(0.0f) //y
            positions.add(Z_POS) //z
            textCoordinates.add(charInfo.startX.toFloat() / fontTexture.width.toFloat())
            textCoordinates.add(0.0f)
            indices.add(i * VERTICES_PER_QUAD)

            // Left Bottom vertex
            positions.add(startx) // x
            positions.add(fontTexture.height.toFloat()) //y
            positions.add(Z_POS) //z
            textCoordinates.add(charInfo.startX.toFloat() / fontTexture.width.toFloat())
            textCoordinates.add(1.0f)
            indices.add(i * VERTICES_PER_QUAD + 1)

            // Right Bottom vertex
            positions.add(startx + charInfo.width) // x
            positions.add(fontTexture.height.toFloat()) //y
            positions.add(Z_POS) //z
            textCoordinates.add((charInfo.startX + charInfo.width).toFloat() / fontTexture.width.toFloat())
            textCoordinates.add(1.0f)
            indices.add(i * VERTICES_PER_QUAD + 2)

            // Right Top vertex
            positions.add(startx + charInfo.width) // x
            positions.add(0.0f) //y
            positions.add(Z_POS) //z
            textCoordinates.add((charInfo.startX + charInfo.width).toFloat() / fontTexture.width.toFloat())
            textCoordinates.add(0.0f)
            indices.add(i * VERTICES_PER_QUAD + 3)

            // Add indices por left top and bottom right vertices
            indices.add(i * VERTICES_PER_QUAD)
            indices.add(i * VERTICES_PER_QUAD + 2)
            startx += charInfo.width.toFloat()
        }
        val posArr = Utils.listToArray(positions)
        val textCoordinatesArr = Utils.listToArray(textCoordinates)
        val indicesArr = indices.stream().mapToInt { i: Int? -> i!! }.toArray()
        val material = Material(fontTexture.texture)
        return Mesh(posArr, textCoordinatesArr, normals, indicesArr, material=material)
    }

    fun getText(): String {
        return text
    }

    fun setText(text: String) {
        this.text = text
        this.mesh.deleteBuffers()
        this.mesh = buildMesh()
    }

    companion object {
        private const val Z_POS = 0.0f
        private const val VERTICES_PER_QUAD = 4
    }

    init {
        mesh = buildMesh()
    }
}
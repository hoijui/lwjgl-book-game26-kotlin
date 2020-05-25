package org.lwjglb.engine.loaders.obj

import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjglb.engine.Utils
import org.lwjglb.engine.graph.InstancedMesh
import org.lwjglb.engine.graph.Material
import org.lwjglb.engine.graph.Mesh
import java.util.*
import java.util.regex.Pattern

object OBJLoader {

    private val spaceRegex : Pattern = Pattern.compile("\\s+")

    @JvmOverloads
    @Throws(Exception::class)
    // TODO instead of fileName only, also support resourceName (Class.getResource(resourceName))
    fun loadMesh(fileName: String, instances: Int = 1, material: Material): Mesh {
        val lines = Utils.readAllLines(fileName)
        val vertices: MutableList<Vector3f> = ArrayList()
        val textures: MutableList<Vector2f> = ArrayList()
        val normals: MutableList<Vector3f> = ArrayList()
        val faces: MutableList<Face> = ArrayList()
        for (line in lines) {
            val tokens = line.split(spaceRegex).toTypedArray()
            when (tokens[0]) {
                "v" -> {
                    // Geometric vertex
                    val vec3f =
                        Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    vertices.add(vec3f)
                }
                "vt" -> {
                    // Texture coordinate
                    val vec2f = Vector2f(tokens[1].toFloat(), tokens[2].toFloat())
                    textures.add(vec2f)
                }
                "vn" -> {
                    // Vertex normal
                    val vec3fNorm =
                        Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat())
                    normals.add(vec3fNorm)
                }
                "f" -> {
                    val face = Face(tokens[1], tokens[2], tokens[3])
                    faces.add(face)
                }
                else -> {
                }
            }
        }
        return reorderLists(vertices, textures, normals, faces, instances, material)
    }

    private fun reorderLists(
        posList: List<Vector3f>,
        textCoordList: List<Vector2f>,
        normList: List<Vector3f>,
        facesList: List<Face>,
        instances: Int,
        material: Material)
            : Mesh
    {
        val indices: MutableList<Int> = ArrayList()
        // Create position array in the order it has been declared
        val posArr = FloatArray(posList.size * 3)
        for ((i, pos) in posList.withIndex()) {
            posArr[i * 3] = pos.x
            posArr[i * 3 + 1] = pos.y
            posArr[i * 3 + 2] = pos.z
        }
        val textCoordArr = FloatArray(posList.size * 2)
        val normArr = FloatArray(posList.size * 3)
        for (face in facesList) {
            val faceVertexIndices = face.faceVertexIndices
            for (indValue in faceVertexIndices) {
                processFaceVertex(
                    indValue, textCoordList, normList,
                    indices, textCoordArr, normArr
                )
            }
        }
        val indicesArr = Utils.listIntToArray(indices)
        val mesh: Mesh
        mesh = if (instances > 1) {
            InstancedMesh(posArr, textCoordArr, normArr, indicesArr, instances, material)
        } else {
            Mesh(posArr, textCoordArr, normArr, indicesArr, material=material)
        }
        return mesh
    }

    private fun processFaceVertex(
        indices: IdxGroup?,
        textCoordList: List<Vector2f>,
        normList: List<Vector3f>,
        indicesList: MutableList<Int>,
        texCoordArr: FloatArray,
        normArr: FloatArray)
    {
        // Set index for vertex coordinates
        val posIndex = indices!!.idxPos
        indicesList.add(posIndex)

        // Reorder texture coordinates
        if (indices.idxTextCoord >= 0) {
            val textCoord = textCoordList[indices.idxTextCoord]
            texCoordArr[posIndex * 2] = textCoord.x
            texCoordArr[posIndex * 2 + 1] = 1 - textCoord.y
        }
        if (indices.idxVecNormal >= 0) {
            // Reorder vectornormals
            val vecNorm = normList[indices.idxVecNormal]
            normArr[posIndex * 3] = vecNorm.x
            normArr[posIndex * 3 + 1] = vecNorm.y
            normArr[posIndex * 3 + 2] = vecNorm.z
        }
    }

    private class Face(v1: String, v2: String, v3: String) {

        /**
         * List of idxGroup groups for a face triangle (3 vertices per face).
         */
        var faceVertexIndices = arrayOfNulls<IdxGroup>(3)

        private fun parseLine(line: String): IdxGroup {
            val idxGroup = IdxGroup()
            val lineTokens = line.split("/").toTypedArray()
            val length = lineTokens.size
            idxGroup.idxPos = lineTokens[0].toInt() - 1
            if (length > 1) {
                // It can be empty if the obj does not define text coordinates
                val textCoord = lineTokens[1]
                idxGroup.idxTextCoord = if (textCoord.isNotEmpty()) textCoord.toInt() - 1 else IdxGroup.NO_VALUE
                if (length > 2 && lineTokens[2].isNotEmpty()) {
                    idxGroup.idxVecNormal = lineTokens[2].toInt() - 1
                }
            }
            return idxGroup
        }

        init {
            faceVertexIndices = arrayOfNulls(3)
            // Parse the lines
            faceVertexIndices[0] = parseLine(v1)
            faceVertexIndices[1] = parseLine(v2)
            faceVertexIndices[2] = parseLine(v3)
        }
    }

    private class IdxGroup {
        var idxPos: Int
        var idxTextCoord: Int
        var idxVecNormal: Int

        companion object {
            const val NO_VALUE = -1
        }

        init {
            idxPos = NO_VALUE
            idxTextCoord = NO_VALUE
            idxVecNormal = NO_VALUE
        }
    }
}

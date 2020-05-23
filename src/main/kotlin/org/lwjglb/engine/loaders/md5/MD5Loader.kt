package org.lwjglb.engine.loaders.md5

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjglb.engine.Utils
import org.lwjglb.engine.graph.Material
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.graph.Texture
import org.lwjglb.engine.graph.anim.AnimGameItem
import org.lwjglb.engine.graph.anim.AnimVertex
import org.lwjglb.engine.graph.anim.AnimatedFrame
import java.util.*

object MD5Loader {

    /**
     * Constructs an AnimGameItem instance based on a MD5 Model an MD5 Animation
     *
     * @param md5Model The MD5 Model
     * @param animModel The MD5 Animation
     * @param defaultColour Default colour to use if there are no textures
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun process(md5Model: MD5Model, animModel: MD5AnimModel, defaultColour: Vector4f/*, material: Material?*/): AnimGameItem {
        val invJointMatrices = calcInJointMatrices(md5Model)
        val animatedFrames =
            processAnimationFrames(md5Model, animModel, invJointMatrices)
        val list: MutableList<Mesh> = ArrayList()
        for (md5Mesh in md5Model.meshes) {
            val material = createTexture(md5Mesh, defaultColour)
            val mesh = generateMesh(md5Model, md5Mesh, material)
            list.add(mesh)
        }
        return AnimGameItem(list, animatedFrames, invJointMatrices)
    }

    private fun calcInJointMatrices(md5Model: MD5Model): List<Matrix4f> {
        val result: MutableList<Matrix4f> = ArrayList()
        val joints = md5Model.jointInfo.joints
        for (joint in joints) {
            // Calculate translation matrix using joint position
            // Calculates rotation matrix using joint orientation
            // Gets transformation matrix bu multiplying translation matrix by rotation matrix
            // Instead of multiplying we can apply rotation which is optimized internally
            val mat = Matrix4f()
                .translate(joint.position)
                .rotate(joint.orientation)
                .invert()
            result.add(mat)
        }
        return result
    }

    private fun generateMesh(md5Model: MD5Model, md5Mesh: MD5Mesh, material: Material): Mesh {
        val vertices: MutableList<AnimVertex> = ArrayList()
        val indices: MutableList<Int> = ArrayList()
        val md5Vertices = md5Mesh.vertices
        val weights = md5Mesh.weights
        val joints = md5Model.jointInfo.joints
        for (md5Vertex in md5Vertices) {
            val startWeight = md5Vertex.startWeight
            val numWeights = md5Vertex.weightCount
            val vertex = AnimVertex(numWeights, Vector3f(), md5Vertex.textCoordinates)
            vertices.add(vertex)
//            vertex.position = Vector3f()
//            vertex.textCoordinates = md5Vertex.textCoordinates
//            vertex.jointIndices = IntArray(numWeights)
//            Arrays.fill(vertex.jointIndices, -1)
//            vertex.weights = FloatArray(numWeights)
//            Arrays.fill(vertex.weights, -1f)
            for (i in startWeight until startWeight + numWeights) {
                val weight = weights[i]
                val joint = joints[weight.jointIndex]
                val rotatedPos = Vector3f(weight.position).rotate(joint.orientation)
                val acumPos = Vector3f(joint.position).add(rotatedPos)
                acumPos.mul(weight.bias)
                vertex.position.add(acumPos)
                vertex.jointIndices[i - startWeight] = weight.jointIndex
                vertex.weights[i - startWeight] = weight.bias
            }
        }
        for (tri in md5Mesh.triangles) {
            indices.add(tri.vertex0)
            indices.add(tri.vertex1)
            indices.add(tri.vertex2)

            // Normals
            val v0 = vertices[tri.vertex0]
            val v1 = vertices[tri.vertex1]
            val v2 = vertices[tri.vertex2]
            val pos0 = v0.position
            val pos1 = v1.position
            val pos2 = v2.position
            val normal = Vector3f(pos2).sub(pos0).cross(Vector3f(pos1).sub(pos0))
            v0.normal.add(normal)
            v1.normal.add(normal)
            v2.normal.add(normal)
        }

        // Once the contributions have been added, normalize the result
        for (v in vertices) {
            v.normal.normalize()
        }
        return createMesh(vertices, indices, material)
    }

    private fun processAnimationFrames(
        md5Model: MD5Model,
        animModel: MD5AnimModel,
        invJointMatrices: List<Matrix4f>
    ): List<AnimatedFrame> {
        val animatedFrames: MutableList<AnimatedFrame> = ArrayList()
        val frames = animModel.frames
        for (frame in frames) {
            val data = processAnimationFrame(md5Model, animModel, frame, invJointMatrices)
            animatedFrames.add(data)
        }
        return animatedFrames
    }

    private fun processAnimationFrame(
        md5Model: MD5Model,
        animModel: MD5AnimModel,
        frame: MD5Frame,
        invJointMatrices: List<Matrix4f>)
            : AnimatedFrame
    {
        val result = AnimatedFrame()
        val baseFrame = animModel.baseFrame
        val hierarchyList = animModel.hierarchy.hierarchyDataList
        val joints = md5Model.jointInfo.joints
        val numJoints = joints.size
        val frameData = frame.frameData
        for (i in 0 until numJoints) {
            val joint = joints[i]
            val baseFrameData = baseFrame.frameDataList[i]
            val position = baseFrameData.position
            var orientation = baseFrameData.orientation
            val flags = hierarchyList!![i].flags
            var startIndex = hierarchyList[i].startIndex
            if (flags and 1 > 0) {
                position!!.x = frameData[startIndex++]
            }
            if (flags and 2 > 0) {
                position!!.y = frameData[startIndex++]
            }
            if (flags and 4 > 0) {
                position!!.z = frameData[startIndex++]
            }
            if (flags and 8 > 0) {
                orientation!!.x = frameData[startIndex++]
            }
            if (flags and 16 > 0) {
                orientation!!.y = frameData[startIndex++]
            }
            if (flags and 32 > 0) {
                orientation!!.z = frameData[startIndex]
            }
            // Update Quaternion's w component
            orientation = MD5Utils.calculateQuaternion(orientation!!.x, orientation.y, orientation.z)

            // Calculate translation and rotation matrices for this joint
            val translateMat = Matrix4f().translate(position)
            val rotationMat = Matrix4f().rotate(orientation)
            var jointMat = translateMat.mul(rotationMat)

            // Joint position is relative to joint's parent index position. Use parent matrices
            // to transform it to model space
            if (joint.parentIndex > -1) {
                val parentMatrix = result.localJointMatrices[joint.parentIndex]
                jointMat = Matrix4f(parentMatrix).mul(jointMat)
            }
            result.setMatrix(i, jointMat, invJointMatrices[i])
        }
        return result
    }

    private fun createMesh(
        vertices: List<AnimVertex>,
        indices: List<Int>,
        material: Material
    ): Mesh {
        val positions: MutableList<Float> = ArrayList()
        val textCoords: MutableList<Float> = ArrayList()
        val normals: MutableList<Float> = ArrayList()
        val jointIndices: MutableList<Int> = ArrayList()
        val weights: MutableList<Float> = ArrayList()
        for (vertex in vertices) {
            positions.add(vertex.position.x)
            positions.add(vertex.position.y)
            positions.add(vertex.position.z)
            textCoords.add(vertex.textCoordinates.x)
            textCoords.add(vertex.textCoordinates.y)
            normals.add(vertex.normal.x)
            normals.add(vertex.normal.y)
            normals.add(vertex.normal.z)
            val numWeights = vertex.weights.size
            for (i in 0 until Mesh.MAX_WEIGHTS) {
                if (i < numWeights) {
                    jointIndices.add(vertex.jointIndices[i])
                    weights.add(vertex.weights[i])
                } else {
                    jointIndices.add(-1)
                    weights.add(-1.0f)
                }
            }
        }
        val positionsArr = Utils.listToArray(positions)
        val textCoordinatesArr = Utils.listToArray(textCoords)
        val normalsArr = Utils.listToArray(normals)
        val indicesArr = Utils.listIntToArray(indices)
        val jointIndicesArr = Utils.listIntToArray(jointIndices)
        val weightsArr = Utils.listToArray(weights)
        return Mesh(
            positionsArr,
            textCoordinatesArr,
            normalsArr,
            indicesArr,
            jointIndicesArr,
            weightsArr,
            material
        )
    }

    @Throws(Exception::class)
    private fun createTexture(
        md5Mesh: MD5Mesh,
        defaultColour: Vector4f)
            : Material
    {
        val texturePath = md5Mesh.texture
        return if (texturePath != null && texturePath.isNotEmpty()) {
            val texture = Texture(texturePath)
            val material = Material(texture)

            // Handle normal Maps;
            val pos = texturePath.lastIndexOf(".")
            if (pos > 0) {
                val basePath = texturePath.substring(0, pos)
                val extension = texturePath.substring(pos, texturePath.length)
                val normalMapFileName = basePath + "_local" + extension
                if (Utils.existsResourceFile(normalMapFileName)) {
                    val normalMap = Texture(normalMapFileName)
                    material.normalMap = normalMap
                }
            }
            material
        } else {
            Material(defaultColour, 1.0f)
        }
    }
}
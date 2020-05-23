package org.lwjglb.engine.loaders.md5

import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.regex.Pattern

class MD5JointInfo(val joints: List<MD5JointData>) {

    override fun toString(): String {
        val str = StringBuilder("joints [" + System.lineSeparator())
        for (joint in joints) {
            str.append(joint).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    class MD5JointData {
        var name: String? = null
        var parentIndex = 0
        var position: Vector3f? = null
        var orientation: Quaternionf? = null
            private set

        fun setOrientation(vec: Vector3f) {
            orientation = MD5Utils.calculateQuaternion(vec)
        }

        override fun toString(): String {
            return "[name: $name, parentIndex: $parentIndex, position: $position, orientation: $orientation]"
        }

        companion object {
            private const val PARENT_INDEX_REGEXP = "([-]?\\d+)"
            private const val NAME_REGEXP = "\\\"([^\\\"]+)\\\""
            private const val JOINT_REGEXP =
                ("\\s*" + NAME_REGEXP + "\\s*" + PARENT_INDEX_REGEXP + "\\s*"
                        + MD5Utils.VECTOR3_REGEXP + "\\s*" + MD5Utils.VECTOR3_REGEXP + ".*")
            private val PATTERN_JOINT =
                Pattern.compile(JOINT_REGEXP)

            fun parseLine(line: String): MD5JointData? {
                var result: MD5JointData? = null
                val matcher = PATTERN_JOINT.matcher(line)
                if (matcher.matches()) {
                    result = MD5JointData()
                    result.name = matcher.group(1)
                    result.parentIndex = matcher.group(2).toInt()
                    var x = matcher.group(3).toFloat()
                    var y = matcher.group(4).toFloat()
                    var z = matcher.group(5).toFloat()
                    result.position = Vector3f(x, y, z)
                    x = matcher.group(6).toFloat()
                    y = matcher.group(7).toFloat()
                    z = matcher.group(8).toFloat()
                    result.setOrientation(Vector3f(x, y, z))
                }
                return result
            }
        }
    }

    companion object {
        fun parse(blockBody: List<String>): MD5JointInfo {
            val joints: MutableList<MD5JointData> = ArrayList()
            for (line in blockBody) {
                val jointData = MD5JointData.parseLine(line)
                if (jointData != null) {
                    joints.add(jointData)
                }
            }
            return MD5JointInfo(joints)
        }
    }
}
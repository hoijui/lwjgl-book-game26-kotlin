package org.lwjglb.engine.loaders.md5

import java.util.*
import java.util.regex.Pattern

class MD5Hierarchy {
    var hierarchyDataList: List<MD5HierarchyData>? = null

    override fun toString(): String {
        val str = StringBuilder("hierarchy [" + System.lineSeparator())
        for (hierarchyData in hierarchyDataList!!) {
            str.append(hierarchyData).append(System.lineSeparator())
        }
        str.append("]").append(System.lineSeparator())
        return str.toString()
    }

    class MD5HierarchyData {
        var name: String? = null
        var parentIndex = 0
        var flags = 0
        var startIndex = 0

        override fun toString(): String {
            return "[name: $name, parentIndex: $parentIndex, flags: $flags, startIndex: $startIndex]"
        }

        companion object {
            private val PATTERN_HIERARCHY =
                Pattern.compile("\\s*\\\"([^\\\"]+)\\\"\\s*([-]?\\d+)\\s*(\\d+)\\s*(\\d+).*")

            fun parseLine(line: String?): MD5HierarchyData? {
                var result: MD5HierarchyData? = null
                val matcher = PATTERN_HIERARCHY.matcher(line)
                if (matcher.matches()) {
                    result = MD5HierarchyData()
                    result.name = matcher.group(1)
                    result.parentIndex = matcher.group(2).toInt()
                    result.flags = matcher.group(3).toInt()
                    result.startIndex = matcher.group(4).toInt()
                }
                return result
            }
        }
    }

    companion object {
        fun parse(blockBody: List<String?>): MD5Hierarchy {
            val result = MD5Hierarchy()
            val hierarchyDataList: MutableList<MD5HierarchyData> =
                ArrayList()
            result.hierarchyDataList = hierarchyDataList
            for (line in blockBody) {
                val hierarchyData = MD5HierarchyData.parseLine(line)
                if (hierarchyData != null) {
                    hierarchyDataList.add(hierarchyData)
                }
            }
            return result
        }
    }
}
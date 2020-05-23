package org.lwjglb.engine

import org.joml.Vector3f
import org.lwjglb.engine.graph.lights.DirectionalLight
import org.lwjglb.engine.graph.lights.PointLight
import org.lwjglb.engine.graph.lights.SpotLight

class SceneLight(
    val ambientLight: Vector3f,
    val skyBoxLight: Vector3f,
    val directionalLight: DirectionalLight)
{
    var pointLightList: MutableList<PointLight> = ArrayList()
    var spotLightList: MutableList<SpotLight> = ArrayList()
}
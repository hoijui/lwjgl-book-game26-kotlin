package org.lwjglb.engine.items

import org.joml.Vector4f
import org.lwjglb.engine.graph.Material
import org.lwjglb.engine.graph.Mesh
import org.lwjglb.engine.graph.Texture
import org.lwjglb.engine.loaders.obj.OBJLoader

class SkyBox : GameItem {

    constructor(objModel: String, textureFile: String)
            : super(OBJLoader.loadMesh(objModel, material=Material(Texture(textureFile), 0.0f)))
    {
        setPosition(0.0f, 0.0f, 0.0f)
    }

    constructor(objModel: String, colour: Vector4f)
            : super(OBJLoader.loadMesh(objModel, material=Material(colour, 0.0f)))
    {
        setPosition(0.0f, 0.0f, 0.0f)
    }
}
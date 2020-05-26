package org.lwjglb.game

import org.lwjgl.system.Configuration
import org.lwjglb.engine.GameEngine
import org.lwjglb.engine.IGameLogic
import org.lwjglb.engine.Window.WindowOptions

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val vSync = true
            val gameLogic: IGameLogic = DummyGame()
            val opts = WindowOptions()
            opts.cullFace = true
            opts.showFps = true
            opts.compatibleProfile = false
            opts.antialiasing = true
            opts.frustumCulling = true
            val gameEng = GameEngine("GAME", vSync, opts, gameLogic)
            gameEng.run()
        } catch (excp: Exception) {
            excp.printStackTrace()
            System.exit(-1)
        }
    }
}

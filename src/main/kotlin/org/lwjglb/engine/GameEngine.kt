package org.lwjglb.engine

import org.lwjglb.engine.Window.WindowOptions

class GameEngine(
    private val windowTitle: String,
    width: Int,
    height: Int,
    vSync: Boolean,
    opts: WindowOptions,
    private val gameLogic: IGameLogic)
    : Runnable
{
    private val window = Window(windowTitle, width, height, vSync, opts)
    private val timer = Timer()
    private val mouseInput: MouseInput = MouseInput()
    private var lastFps = 0.0
    private var fps = 0

    constructor(windowTitle: String, vSync: Boolean, opts: WindowOptions, gameLogic: IGameLogic) : this(
        windowTitle,
        0,
        0,
        vSync,
        opts,
        gameLogic
    )

    override fun run() {
        try {
            init()
            gameLoop()
        } catch (excp: Exception) {
            excp.printStackTrace()
        } finally {
            cleanup()
        }
    }

    @Throws(Exception::class)
    protected fun init() {
        window.init()
        timer.init()
        mouseInput.init(window)
        gameLogic.init(window)
        lastFps = timer.time
        fps = 0
    }

    protected fun gameLoop() {

        var elapsedTime: Float
        var accumulator = 0.0f
        val interval = 1.0f / TARGET_UPS
        val running = true
        while (running && !window.windowShouldClose()) {
            elapsedTime = timer.elapsedTime
            accumulator += elapsedTime
            input()
            while (accumulator >= interval) {
                update(interval)
                accumulator -= interval
            }
            render()
            if (!window.isvSync()) {
                sync()
            }
        }
    }

    protected fun cleanup() {
        gameLogic.cleanup()
    }

    private fun sync() {
        val loopSlot = 1f / TARGET_FPS
        val endTime = timer.lastLoopTime + loopSlot
        while (timer.time < endTime) {
            try {
                Thread.sleep(1)
            } catch (ie: InterruptedException) {
            }
        }
    }

    protected fun input() {
        mouseInput.input(window)
        gameLogic.input(window, mouseInput)
    }

    protected fun update(interval: Float) {
        gameLogic.update(interval, mouseInput, window)
    }

    protected fun render() {
        if (window.options.showFps && timer.lastLoopTime - lastFps > 1) {
            lastFps = timer.lastLoopTime
            window.windowTitle = "$windowTitle - $fps FPS"
            fps = 0
        }
        fps++
        gameLogic.render(window)
        window.update()
    }

    companion object {
        const val TARGET_FPS = 75
        const val TARGET_UPS = 30
    }
}
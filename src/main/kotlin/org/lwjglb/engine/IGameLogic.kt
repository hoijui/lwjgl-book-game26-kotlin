package org.lwjglb.engine

interface IGameLogic {
    @Throws(Exception::class)
    fun init(window: Window)
    fun input(window: Window, mouseInput: MouseInput)
    fun update(interval: Float, mouseInput: MouseInput, window: Window)
    fun render(window: Window)
    fun cleanup()
}
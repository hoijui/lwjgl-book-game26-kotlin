package org.lwjglb.engine

class Timer {
    var lastLoopTime = 0.0
        private set

    fun init() {
        lastLoopTime = time
    }

    /**
     * @return Current time in seconds
     */
    val time: Double
        get() = System.nanoTime() / 1000000000.0

    val elapsedTime: Float
        get() {
            val time = time
            val elapsedTime = (time - lastLoopTime).toFloat()
            lastLoopTime = time
            return elapsedTime
        }

}
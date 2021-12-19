package me.hufman.idriveconnectaddons.futuristiccar

import android.os.Handler

class SpeedCalculator(val handler: Handler, val engineWeights: EngineWeights, val pedalState: PedalState) {
	var lastTime = 0L
	val updateInterval = 50L
	var currentSpeed = 0.5

	var listener: ((speed: Double) -> Unit)? = null

	val updateCallback = Runnable {
		update()
	}
	fun start() {
		handler.removeCallbacks(updateCallback)
		handler.postDelayed(updateCallback, updateInterval)
	}
	fun update() {
		val difference = pedalState.targetSpeed - currentSpeed
		val factor = if (difference > 0) {       // accelerating
			engineWeights.speedUp
		} else {
			engineWeights.speedDown
		}
		if (lastTime > 0) {
			val timeDifference = System.currentTimeMillis() - lastTime
			val change = difference * factor * timeDifference / 1000.0
			currentSpeed += change
		}
		lastTime = System.currentTimeMillis()

		handler.postDelayed(updateCallback, updateInterval)

		listener?.invoke(currentSpeed)
	}
	fun stop() {
		handler.removeCallbacks(updateCallback)
	}
}
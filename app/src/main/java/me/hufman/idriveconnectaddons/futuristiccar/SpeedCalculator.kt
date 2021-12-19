package me.hufman.idriveconnectaddons.futuristiccar

import android.os.Handler
import kotlin.math.sign

class SpeedCalculator(val handler: Handler, val engineWeights: EngineWeights, val carState: CarState) {
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
		val difference = carState.getTargetSpeed() - currentSpeed
		val factor = if (difference > 0) {       // accelerating
			engineWeights.speedUp
		} else {
			engineWeights.speedDown
		}
		val change = sign(difference) * factor * updateInterval / 1000.0
		currentSpeed += change

		handler.postDelayed(updateCallback, updateInterval)

		listener?.invoke(currentSpeed)
	}
	fun stop() {
		handler.removeCallbacks(updateCallback)
	}
}
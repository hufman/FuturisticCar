package me.hufman.idriveconnectaddons.futuristiccar

class CarState {
	var running = true
	var drivingGear = false     // whether we are in a driving gear, as opposed to park/neutral

	var pedalState: Double = 0.5        // how far down the pedal is pushed
	var speedometer: Double = 0.0       // how fast we are going

	fun getTargetSpeed(): Double {
		return if (!running) {
			0.0
		} else if (!drivingGear) {
			// while in park/neutral, map the accelerator pedal
			mapPercentage(pedalState, 0.35)
		} else {
			// in driving gear, use the speedometer
			mapPercentage(speedometer, 0.30)
		}
	}

	private fun mapPercentage(input: Double, min: Double): Double {
		// simplified from https://rosettacode.org/wiki/Map_range
		// maps 0..1 to min..1
		return min + input * (1-min)
	}
}
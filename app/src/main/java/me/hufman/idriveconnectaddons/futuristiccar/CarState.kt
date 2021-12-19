package me.hufman.idriveconnectaddons.futuristiccar

class CarState {
	var running = true
	var drivingGear = false     // whether we are in a driving gear, as opposed to park/neutral

	var pedalState: Double = 0.5        // how far down the pedal is pushed
	var speedometer: Double = 0.0       // how fast we are going

	fun getTargetSpeed(): Double {
		return if (!running) {
			0.0
		} else {
			mapPedalState(pedalState)
		}
	}

	private fun mapPedalState(input: Double): Double {
		// simplified from https://rosettacode.org/wiki/Map_range
		// maps 0..1 to 0.3..1
		return 0.35 + input * 0.65
	}
}
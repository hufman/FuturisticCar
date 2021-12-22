package me.hufman.idriveconnectaddons.futuristiccar

class CarState {
	val SPEED_TIERS = listOf(       // speedometer thresholds to bump up to the next tier
		0.0,
		0.30,
		0.60,
		1000.0
	)
	var running = true
	var drivingGear = false     // whether we are in a driving gear, as opposed to park/neutral

	var pedalState: Double = 0.5        // how far down the pedal is pushed
	var speedometer: Double = 0.0       // how fast we are going
	var speedTier = 0

	fun getTargetSpeed(): Double {
		adjustSpeedTier()
		return if (!running) {
			0.0
		} else if (!drivingGear) {
			// while in park/neutral, map the accelerator pedal
			mapPercentage(pedalState, 0.35)
		} else {
			// in driving gear, use the speedometer
			val speedThreshold = if (speedTier + 1 < SPEED_TIERS.size) { SPEED_TIERS[speedTier+1] * 1.3 } else 1.0
			val rev = speedometer / speedThreshold
			mapPercentage(rev, 0.30)
		}
	}

	private fun adjustSpeedTier() {
		if (speedTier < SPEED_TIERS.size-1 && speedometer > SPEED_TIERS[speedTier+1] * 1.2) {
			speedTier += 1
		}
		if (speedTier > 0 && speedometer < SPEED_TIERS[speedTier]) {
			speedTier -= 1
		}
	}

	private fun mapPercentage(input: Double, min: Double): Double {
		// simplified from https://rosettacode.org/wiki/Map_range
		// maps 0..1 to min..1
		return min + input * (1-min)
	}
}
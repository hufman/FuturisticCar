package me.hufman.idriveconnectaddons.futuristiccar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar

class MainActivity : AppCompatActivity() {
	val carState = CarState()
	val soundController by lazy { SoundControllerPlayback(this.resources, carState) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		findViewById<SeekBar>(R.id.speed).setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
				if (fromUser) {
					carState.pedalState = progress / 100.0
				}
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}
			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})
	}

	override fun onResume() {
		super.onResume()
		soundController.play()
	}

	override fun onPause() {
		super.onPause()
		soundController.pause()
	}
}
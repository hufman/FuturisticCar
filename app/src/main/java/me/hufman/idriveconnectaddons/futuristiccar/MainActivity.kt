package me.hufman.idriveconnectaddons.futuristiccar

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import io.bimmergestalt.idriveconnectkit.CDSProperty
import me.hufman.idriveconnectaddons.futuristiccar.lib.CDSLiveData
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsDouble
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsInt
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsJsonObject
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsJsonPrimitive

class MainActivity : AppCompatActivity() {
	val carState = CarState()
	val speedCalculator = SpeedCalculator(Handler(Looper.getMainLooper()), EngineWeights(), carState)
	lateinit var audioClip: AudioTrack

	val carRunning by lazy { CDSLiveData(this, CDSProperty.DRIVING_KEYPOSITION) }
	val carPedalPosition by lazy { CDSLiveData(this, CDSProperty.DRIVING_ACCELERATORPEDAL) }
	val carGear by lazy { CDSLiveData(this, CDSProperty.DRIVING_GEAR) }
	val carSpeed by lazy { CDSLiveData(this, CDSProperty.DRIVING_SPEEDACTUAL) }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		loadAudioClip()
		speedCalculator.listener = {
			audioClip.playbackRate = (65000 * it).toInt()
			val acceleration = carState.getTargetSpeed() - speedCalculator.currentSpeed
			val volumeAdjust = if (acceleration > 0) {
				acceleration * 2
			} else { 0.0 }
			val volume = it + 0.5 + volumeAdjust
			audioClip.setVolume(volume.toFloat())
		}

		carRunning.observe(this) {
			val running = it?.tryAsJsonObject("keyPosition")?.tryAsJsonPrimitive("running")?.tryAsInt
			if (running != null) {
				carState.running = running == 1
			}
		}
		carGear.observe(this) {
			val gear = it?.tryAsJsonPrimitive("gear")?.tryAsInt
			if (gear != null) {
				carState.drivingGear = gear != 1 && gear != 3
			}
		}
		carPedalPosition.observe(this) {
			val position = it?.tryAsJsonObject("acceleratorPedal")?.tryAsJsonPrimitive("position")?.tryAsDouble
			if (position != null) {
				carState.pedalState = position / 100.0
				findViewById<SeekBar>(R.id.speed).progress = position.toInt()
			}
		}
		carSpeed.observe(this) {
			val speed = it?.tryAsJsonPrimitive("speedActual")?.tryAsInt
			if (speed != null) {
				carState.speedometer = speed / 100.0
			}
		}

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

	private fun loadAudioClip() {
		val data = resources.openRawResource(R.raw.jetsonscar)
		val audioAttributes = AudioAttributes.Builder()
			.setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
			.setUsage(AudioAttributes.USAGE_GAME)
			.build()
		val audioFormat = AudioFormat.Builder()
			.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
			.setSampleRate(44100)
			.setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
			.build()
		audioClip = AudioTrack(audioAttributes, audioFormat, 16384, AudioTrack.MODE_STATIC, 0)
		data.skip(46)
		val dataRead = data.readBytes()
		audioClip.write(dataRead, 0, dataRead.size)
		data.close()
		audioClip.setLoopPoints(0, dataRead.size / 2, -1)
		audioClip.playbackRate = (65000 * speedCalculator.currentSpeed).toInt()
	}

	override fun onResume() {
		super.onResume()
		speedCalculator.start()
		audioClip.play()
	}

	override fun onPause() {
		super.onPause()
		speedCalculator.stop()
		audioClip.stop()
	}
}
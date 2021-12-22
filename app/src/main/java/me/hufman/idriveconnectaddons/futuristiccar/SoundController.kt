package me.hufman.idriveconnectaddons.futuristiccar

import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper

interface SoundController {
	val isPlaying: Boolean
	fun togglePlayback()
	fun play()
	fun pause()
}

class SoundControllerPlayback(val resources: Resources, val carState: CarState) : SoundController {
	override var isPlaying: Boolean = false
		private set

	val speedCalculator = SpeedCalculator(Handler(Looper.getMainLooper()), EngineWeights(), carState)
	val audioClip: AudioTrack

	init {
		audioClip = loadAudioClip()
		speedCalculator.listener = {
			audioClip.playbackRate = (65000 * it).toInt()
		}
	}

	private fun loadAudioClip(): AudioTrack {
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
		val audioClip = AudioTrack(audioAttributes, audioFormat, 16384, AudioTrack.MODE_STATIC, 0)
		data.skip(46)
		val dataRead = data.readBytes()
		audioClip.write(dataRead, 0, dataRead.size)
		data.close()
		audioClip.setLoopPoints(0, dataRead.size / 2, -1)
		audioClip.playbackRate = (65000 * speedCalculator.currentSpeed).toInt()
		return audioClip
	}

	override fun togglePlayback() {
		if (isPlaying) {
			pause()
		} else {
			play()
		}
	}

	override fun play() {
		isPlaying = true
		speedCalculator.start()
		audioClip.play()
	}
	override fun pause() {
		isPlaying = false
		speedCalculator.stop()
		audioClip.stop()
	}
}
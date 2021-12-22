package me.hufman.idriveconnectaddons.futuristiccar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat

class SoundControllerNotification(val context: Context, val controller: SoundController): SoundController {
	val notificationManager by lazy { context.getSystemService(NotificationManager::class.java) }
	val CLOSE_INTENT_ACTION = "STOP_SOUND"
	val CLOSE_INTENT_ID = 1234
	val NOTIFICATION_ID = 23453
	val CHANNEL_ID = "PlayingNotification"

	val closeReceiver = object: BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			pause()
		}
	}

	override val isPlaying
		get() = controller.isPlaying

	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(CHANNEL_ID,
				context.getString(R.string.notification_channel_name),
				NotificationManager.IMPORTANCE_MIN)
			notificationManager.createNotificationChannel(channel)
		}
	}

	private fun startNotification() {
		context.registerReceiver(closeReceiver, IntentFilter(CLOSE_INTENT_ACTION))

		createNotificationChannel()
		val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
			.setChannelId(CHANNEL_ID)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setContentTitle(context.getText(R.string.notification_title))
		val closeIntent = Intent(CLOSE_INTENT_ACTION)
		val closePendingIntent = PendingIntent.getBroadcast(context, CLOSE_INTENT_ID, closeIntent, 0)
		notificationBuilder.setDeleteIntent(closePendingIntent)
		val notification = notificationBuilder.build()
		notificationManager.notify(NOTIFICATION_ID, notification)
	}
	private fun stopNotification() {
		context.unregisterReceiver(closeReceiver)
		notificationManager.cancel(NOTIFICATION_ID)
	}

	override fun togglePlayback() {
		if (isPlaying) {
			pause()
		} else {
			play()
		}
	}

	override fun play() {
		controller.play()
		startNotification()
	}

	override fun pause() {
		controller.pause()
		stopNotification()
	}
}
package io.xbird.library.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import io.xbird.library.BuildConfig
import io.xbird.library.R
import java.text.SimpleDateFormat
import java.util.*


fun log(msg: String) {
    Log.d("Library:", msg)
}

fun getFormatedDate(date: Date): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    return simpleDateFormat.format(date)
}

fun createNotification(
    context: Context,
    notificationManager: NotificationManager,
    notificationChannelId: String = BuildConfig.APPLICATION_ID,
    title: String = "Free Fall Event",
    ticket: String = "",
    description: String = "Free fall is running to detect...",
    isCancelable: Boolean = true,
    pendingIntent: PendingIntent? = null
): Notification {

    // depending on the Android API that we're dealing with we will have
    // to use a specific method to create the notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            notificationChannelId,
            "${notificationChannelId}.notify",
            NotificationManager.IMPORTANCE_HIGH
        ).let {

            val attrs = AudioAttributes.Builder()
            attrs.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            attrs.setUsage(AudioAttributes.USAGE_NOTIFICATION)

            it.description = title
            it.enableLights(true)
            it.lightColor = Color.RED
            it.enableVibration(true)
            it.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attrs.build())
            it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            it
        }
        notificationManager.createNotificationChannel(channel)
    }

    val builder: Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            context,
            notificationChannelId
        ) else Notification.Builder(context)

    builder
        .setAutoCancel(isCancelable)
        .setContentTitle(title)
        .setContentText(description)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setTicker(ticket)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        builder.setPriority(Notification.PRIORITY_HIGH)
    }
    if (pendingIntent != null) {
        builder.addAction(android.R.drawable.ic_media_pause, "Stop", pendingIntent)
    }
    return builder.build()
}
package de.drachenfels.gcontrl.modules

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.drachenfels.gcontrl.R
import java.util.*

object Notifications {

    val id: Int = UUID.randomUUID().hashCode()
    private lateinit var notification: Notification

    fun createChannel(
        context: Context,
        channelId: String = context.getString(R.string.app_name),
        channelName: Int,
        channelDescription: Int,
        importanceLevel: Int = NotificationManager.IMPORTANCE_HIGH
    ) {
        val channel = NotificationChannel(
            channelId,
            context.getString(channelName),
            importanceLevel
        ).apply {
            description = context.getString(channelDescription)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun create(
        context: Context,
        title: String,
        content: String,
    ): Notification {
        val builder = NotificationCompat.Builder(context, context.getString(R.string.app_name))
        notification = builder
            .setOngoing(true)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_gc_foreground)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setOnlyAlertOnce(true)
            .build()
        return notification
    }

    @SuppressLint("MissingPermission")
    fun update(
        context: Context,
        title: String,
        content: String,
    ): Notification {
        val notification = create(context, title, content)
        NotificationManagerCompat.from(context)
            .notify(
                id,
                notification
            )
        return notification
    }

}
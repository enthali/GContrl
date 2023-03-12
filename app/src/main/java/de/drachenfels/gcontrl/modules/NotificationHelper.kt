package de.drachenfels.gcontrl.modules

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import de.drachenfels.gcontrl.MainActivity
import de.drachenfels.gcontrl.R

/**
 * Helper class for creating and showing notifications.
 *
 * @property context The context to use for creating the notifications.
 */
class NotificationHelper(private val context: Context) {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "de.drachenfels.gcontrl.notification_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "GContrL Notification Channel"
        private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notification Channel for GContrL App"
        private const val NOTIFICATION_ID = 1
    }

    private val notificationManager = getSystemService(context, NotificationManager::class.java)!!

    /**
     * Creates a new notification with the given title and message.
     *
     * @param title The title of the notification.
     * @param message The message of the notification.
     * @return The created notification.
     */
    fun createNotification(title: String, message: String): Notification {
        // Create the notification channel (not needed for SDK_INT > 26)
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = NOTIFICATION_CHANNEL_DESCRIPTION
        }
        notificationManager.createNotificationChannel(notificationChannel)

        // Create the notification using the NotificationCompat.Builder
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setOnlyAlertOnce(true)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_dialog_info,
                context.getString(R.string.notification_button_text),
                pendingIntent
            )
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Displays a notification with the given title and message, using the specified notification ID.
     *
     * If a notification with the same ID is already being displayed, it will be updated with the new information.
     *
     * @param title The title of the notification.
     * @param message The message of the notification.
     * @param notificationId The ID to use for the notification.
     */
    fun showNotification(title: String, message: String, notificationId: Int) {
        // Create the notification
        val notification = createNotification(title, message)

        // Check if the notification with the given ID is already being displayed
        val activeNotifications = notificationManager.activeNotifications

        activeNotifications.forEach {
            if (it.id == notificationId) {
                // If an existing notification is found, update it with the new information
                notificationManager.notify(notificationId, notification)
                return
            }
        }

        // Otherwise, display the new notification as usual
        notificationManager.notify(notificationId, notification)
    }
}

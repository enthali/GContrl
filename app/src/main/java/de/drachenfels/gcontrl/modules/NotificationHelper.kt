/*
 * Copyright 2023 Georg Doll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    // Declare constants used by the NotificationHelper
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "de.drachenfels.gcontrl.notification_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "GContrL Notification Channel"
        private const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notification Channel for GContrL App"
        private const val NOTIFICATION_ID = 1
    }

    // Declare notification manager variable
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
            .setOngoing(true) // Notification cannot be dismissed by the user
            .setCategory(Notification.CATEGORY_SERVICE) // Notification category
            .setPriority(NotificationManager.IMPORTANCE_MIN) // Notification priority
            .setOnlyAlertOnce(true) // Only alert the user once
            .setContentTitle(title) // Notification title
            .setContentText(message) // Notification message
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Notification icon
            .setAutoCancel(true) // Remove notification when clicked
            .addAction(
                android.R.drawable.ic_dialog_info, // Icon for the notification action
                context.getString(R.string.notification_button_text), // Text for the notification action
                pendingIntent // Intent to launch when the notification action is clicked
            )
            .setContentIntent(pendingIntent) // Intent to launch when the notification is clicked
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

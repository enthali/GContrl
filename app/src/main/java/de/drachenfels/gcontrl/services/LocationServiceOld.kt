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

package de.drachenfels.gcontrl.services

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import de.drachenfels.gcontrl.MainActivity
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.modules.*
import kotlin.math.roundToInt

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class LocationServiceOld : Service() {


    /*
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var locationCallback: LocationCallback

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var timerHandler: Handler
    private var notificationCounter = 0

    private val updateNotificationTask = object : Runnable {
        override fun run() {
            initiateLocationRequest()
            if (serviceRunningInForeground) {
                updateNotification()
            }
            timerHandler.postDelayed(this, TIMER_HANDLER_DELAY_MILLIS)
        }
    }

    private fun updateNotification() {
        Log.d(TAG, "Timer Task : updateNotification()")
        //       if (notificationsEnabled && appPreferences.notifications) {
        with(NotificationManagerCompat.from(this)) {

            notificationCounter++

            //          val message = notificationCounter.toString()

            notificationManager.notify(
                NOTIFICATION_ID,
                generateNotification()
            )
//            statsNotification.setContentText(message)
//            foregroundServiceNotification.setContentText(message)
//            notify(STATS_NOTIFICATION_ID, statsNotification.build())
//            notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
        }
//        } else if (notificationsEnabled && !appPreferences.notifications) {
//            notificationsEnabled = false
//            with(NotificationManagerCompat.from(this)) {
//                cancel(STATS_NOTIFICATION_ID)
//            }
//            foregroundServiceNotification.setContentText(getString(R.string.foreground_service_info))
//            NotificationManagerCompat.from(this).notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
//        } else if (!notificationsEnabled && appPreferences.notifications) {
//            notificationsEnabled = true
//        }
    }


    override fun onCreate() {
        Log.d(TAG, "onCreate()")

        locationCount = 0

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

//        locationRequest =
//            LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(2))
//                .apply {
//                    //setMinUpdateDistanceMeters(2F)
//                    setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
//                    setWaitForAccurateLocation(true)
//                    setMaxUpdates(10)
//                }
//                .build()


        // register a location callback
        locationCallback =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    onLocationUpdate(locationResult.lastLocation)
                    super.onLocationResult(locationResult)
                    // call the local onLocationUpdate member function

                }
            }


    }

    private fun onLocationUpdate(_lastLocation: Location?) {
        Log.d(TAG, "onLocationUpdate()")

        if (_lastLocation != null) {
            val lastLocation: Location = _lastLocation

            // calculate the distance to home
            val homeLocation = Location("homeLocation")
            homeLocation.latitude =
                sharedPreferences.getString(
                    getString(R.string.prf_key_geo_latitude),
                    "0.0"
                ).toString().toDouble()
            homeLocation.longitude =
                sharedPreferences.getString(
                    getString(R.string.prf_key_geo_longitude),
                    "0.0"
                ).toString().toDouble()

            // home location doesn't store the altitude for distance calculation use current altitude
            homeLocation.altitude = lastLocation.altitude

            // check preferences on auto door control before updating LiveData
            enableAutoDoorControl = de.drachenfels.gcontrl.modules.sharedPreferences.getBoolean(
                getString(R.string.prf_key_geo_auto_control),
                false
            )
            val newDistance = lastLocation.distanceTo(homeLocation).roundToInt()
            val oldDistance = distanceToHome.value!!
            val fence =
                sharedPreferences.getString(getString(R.string.prf_key_geo_fence_size), "1")
                    .toString()
                    .toInt()

            if (locationCount > DISMISS_LOCATIONS) {

                // check if the distance just got bigger then the fence -> leaving home 1
                if ((oldDistance > fence) && (newDistance < fence)) {
                    if (fenceWatcher.value != HOME_ZONE_ENTERING) {
                        fenceWatcher.postValue(HOME_ZONE_ENTERING)
                        onFenceStateChange(HOME_ZONE_ENTERING)
                    }
                }
                if ((oldDistance > fence) && (newDistance > fence)) {
                    if (fenceWatcher.value != HOME_ZONE_OUTSIDE)
                        fenceWatcher.postValue(HOME_ZONE_OUTSIDE)
                }
                if ((oldDistance < fence) && (newDistance < fence)) {
                    if (fenceWatcher.value != HOME_ZONE_INSIDE)
                        fenceWatcher.postValue(HOME_ZONE_INSIDE)
                }
                if ((oldDistance < fence) && (newDistance > fence)) {
                    if (fenceWatcher.value != HOME_ZONE_LEAVING) {
                        fenceWatcher.postValue(HOME_ZONE_LEAVING)
                        onFenceStateChange(HOME_ZONE_LEAVING)
                    }
                }

            } else {
                locationCount++
            }
            if (newDistance != oldDistance) {
                // post the new distance
                distanceToHome.postValue(newDistance)
                // post the location available for other functions
                currentLocation.postValue(lastLocation)
            }


            // Updates notification content
//            // if this service is running as a foreground  service.
            if (serviceRunningInForeground) {
                notificationManager.notify(
                    NOTIFICATION_ID,
                    generateNotification()
                )
            }
        } else
        // reset the location counter - something's wrong and we want at least
        // DISMISS_LOCATIONS before we trigger any door action
            locationCount = 0
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")

        locationCount = 0

        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
        // return START_NOT_STICKY
        // Try if this works better
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind()")

        // MainActivity (client) comes into foreground and binds to service, so the service can
        // become a background services.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        Log.d(TAG, "onRebind()")

        // MainActivity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
//        // NOTE: If this method is called due to a configuration change in MainActivity,
//        // we do nothing.
        if (!configurationChange && sharedPreferences.getBoolean(
                getString(R.string.prf_key_geo_enable_location_features),
                false
            )
        ) {
            Log.d(TAG, "Start foreground service")
            val notification = generateNotification()
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }

        // Ensures onRebind() is called if MainActivity (client) rebinds.
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged()")
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    fun subscribeToLocationUpdates() {
        Log.d(TAG, "subscribeToLocationUpdates()")

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, LocationService::class.java))

        initiateLocationRequest()

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateNotificationTask)
    }

    private fun initiateLocationRequest() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {

//            sharedPreferences.edit().putBoolean("subscribe_location_services", false).apply()
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }


    private fun unsubscribeToLocationUpdates() {
        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            val removeTask = fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
            //   sharedPreferences.edit().putBoolean("subscribe_location_services", false).apply()
        } catch (unlikely: SecurityException) {
            //   sharedPreferences.edit().putBoolean("subscribe_location_services", true).apply()
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }
    }

    /*
     * Generates a BIG_TEXT_STYLE Notification that represent latest location.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun generateNotification(): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = distanceToText(distanceToHome.value!!)

        //val mainNotificationText = location?.toText() ?: getString(R.string.no_location_text)
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, LocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            //this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher_gc)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                /* icon = */ R.mipmap.ic_launcher_gc,
                /* title = */ getString(R.string.launch_activity),
                /* intent = */ activityPendingIntent
            )
            .addAction(
                /* icon = */ R.mipmap.ic_launcher_gc,
                /* title = */ getString(R.string.stop_location_service_notification_text),
                /* intent = */ servicePendingIntent
            )
            .build()
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: LocationServiceOld
            get() = this@LocationServiceOld
    }

    companion object {
        private const val TAG = "GeoLocationService"

        private const val PACKAGE_NAME = "de.drachenfels.gcontrol"

        const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "channel_01"

        private const val TIMER_HANDLER_DELAY_MILLIS = 5000L

    }
}

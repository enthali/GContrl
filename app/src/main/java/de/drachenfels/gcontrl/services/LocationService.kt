package de.drachenfels.gcontrl.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import de.drachenfels.gcontrl.R
import de.drachenfels.gcontrl.modules.*
import de.drachenfels.gcontrl.receiver.RestartBackgroundService
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class LocationService : Service() {

    private val TAG = "LocationService"

    override fun onCreate() {
        super.onCreate()
        createNotificationChanel()
        requestLocationUpdates()
    }

    private fun createNotificationChanel() {
        val NOTIFICATION_CHANNEL_ID = "de.drachenfels.gcontrol"
        val channelName = "Location Service"
        val chan = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentTitle("App is running ")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimerTask()
        val broadcastIntent = Intent()
        broadcastIntent.action = "restartservice"
        broadcastIntent.setClass(this, RestartBackgroundService::class.java)
        this.sendBroadcast(broadcastIntent)
    }

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private fun startTimer() {
        Log.d(TAG, "startTimer()")
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                Log.d(
                    "TimerTask", currentLocation.value.toString().plus(
                        " Distance to home : ".plus(
                            distanceToHome.value.toString()
                        )
                    )
                )
            }
        }
        timer!!.schedule(
            timerTask,
            0,
            1000
        ) //1 * 60 * 1000 1 minute
    }

    private fun stopTimerTask() {
        Log.d(TAG, "stopTimerTask()")
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun requestLocationUpdates() {

        val locationRequest =
            LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(2))
                .apply {
                    // here's where specific parameters can be set -
                    // https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest
                    setWaitForAccurateLocation(true)
                }.build()

        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permission == PackageManager.PERMISSION_GRANTED) { // Request location updates
            client.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        Log.d("Location Service", "location update $location")
                        if ((location.latitude != 0.0) && (location.longitude != 0.0)) onLocationUpdate(
                            location
                        )
                    }
                }
            }, null)
        }
    }

    private fun onLocationUpdate(lastLocation: Location) {
        Log.d(TAG, "onLocationUpdate()")

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
        enableAutoDoorControl = sharedPreferences.getBoolean(
            getString(R.string.prf_key_geo_auto_control),
            false
        )
        val newDistance = lastLocation.distanceTo(homeLocation).roundToInt()
        val oldDistance = distanceToHome.value!!

        val fence =
            sharedPreferences.getString(getString(R.string.prf_key_geo_fence_size), "1")
                .toString()
                .toInt()

        // at startup old distance is not initialised so it will be 0
        // this would trigger a departure signal - which is wrong -> skip the state machine update
        if (oldDistance != 0) {

            // if the new distance is 0 most likely the home location was set
            // however this should not trigger an arrival signal -> skip the state machine update
            if (newDistance != 0) {

                // check if the distance just got bigger then the fence -> leaving home 1
                if ((oldDistance > fence) && (newDistance < fence)) {
                    if (fenceWatcher.value != HOME_ZONE_ENTERING) {
                        fenceWatcher.postValue(HOME_ZONE_ENTERING)
                        Log.d(TAG, "OPEN THE DOOR")
                        onFenceStateChange(HOME_ZONE_ENTERING)
                    }
                }

                // check if the new distance and old distance are outside -> we are outside
                if ((oldDistance > fence) && (newDistance > fence)) {
                    if (fenceWatcher.value != HOME_ZONE_OUTSIDE)
                        fenceWatcher.postValue(HOME_ZONE_OUTSIDE)
                }

                // check if the new distance and the old distance are inside the fence -> still at home
                if ((oldDistance < fence) && (newDistance < fence)) {
                    if (fenceWatcher.value != HOME_ZONE_INSIDE)
                        fenceWatcher.postValue(HOME_ZONE_INSIDE)
                }

                // check if the old distance is inside but the new distance is outside the fence -> we are leaving
                if ((oldDistance < fence) && (newDistance > fence)) {
                    if (fenceWatcher.value != HOME_ZONE_LEAVING) {
                        fenceWatcher.postValue(HOME_ZONE_LEAVING)
                        Log.d(TAG, "CLOSE THE DOOR")
                        onFenceStateChange(HOME_ZONE_LEAVING)
                    }
                }
            }
        }

        // update the new distance to home live data
        if (newDistance != oldDistance) {
            // post the new distance
            distanceToHome.postValue(newDistance)
            // post the location available for other functions
            currentLocation.postValue(lastLocation)
        }

        // Updates notification content
        // TODO, here's where the notification should be updated to show the current distance to home

    }
}
package de.drachenfels.gcontrl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.Task
import java.util.*
import java.util.concurrent.TimeUnit

class GeoServices(_viewModel: GControlViewModel) {


    private val timeTag: String = "Timer"

    private val viewModel = _viewModel

    private var setLocation: Boolean = false

    // Location handling

    private val permissionId = 2
    private var timerLooper = false

    //    // the location is updated every now and then
    private var locationPollTime: Long = 2

    init {
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    geoService()
                }
            },
            0,
            TimeUnit.SECONDS.toMillis(locationPollTime)
        ) //put here time 1000 milliseconds=1 second
    }

    fun geoService() {

        // check if geo services are enabled in the preferences if not, we'll do nothing
        if (viewModel.sharedPref.getBoolean("geo_enable_location_features", false)) {


            // get the current location. this might take some time
            getLocation()
            if (!timerLooper) {
                timerLooper = true
                Looper.prepare()
            }

            val homeLocation = Location("Home")
            val homeZoneRadius: Float


            homeLocation.longitude =
                viewModel.sharedPref.getString("geo_longitude", "").toString().toDouble()
            homeLocation.latitude =
                viewModel.sharedPref.getString("geo_latitude", "").toString().toDouble()

            val distanceTemp: Float = viewModel.currentLocation.distanceTo(homeLocation)

            // only do something if the distance has changed
            if (distanceTemp != viewModel.distanceToHome.value) {

                // update distance
                viewModel.distanceToHome.value = viewModel.currentLocation.distanceTo(homeLocation)

                // home zone handling
                homeZoneRadius = viewModel.sharedPref.getString("geo_fence_size", "20")
                    .toString()
                    .toInt()
                    .toFloat()


                val homeDistance: Float = viewModel.distanceToHome.value!!

                if (viewModel.insideFence) {
                    if (homeDistance > homeZoneRadius
                    ) {
                        // leaving the home zone
                        viewModel.insideFence = false
                        Log.v(timeTag, "leaving the home zone")
                    }
                } else {
                    if (homeDistance < homeZoneRadius) {
                        // entering home zone
                        viewModel.insideFence = true
                        Log.v(timeTag, "entering the home zone")
                    }
                }
            }

        }
    }

    fun setCurrentHomeLocation(): Boolean {
        // getLocation might not have been called yet, so call it once now
        setLocation = true
        getLocation()
        return true
    }

    /**
     * run task to get lastLocation and call setLocation() to perform what
     * ever the app want's to do with the location
     */
    @SuppressLint("MissingPermission")
    private fun getLocation() {
        // check if location access is permitted by the user
        if (checkPermissions()) {
            // check if the location service is enabled on the device
            if (isLocationEnabled()) {
                // get the last known location and execute setLocation()
                viewModel.mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    onLocationReady(task)
                }
            } else {
                // request the user to turn on the location service on his device
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                //                startActivity(intent)
                viewModel.getApplication<Application>().startActivity(intent)

            }
        } else {
            // ask the user to allow permission
            requestPermissions()
        }
    }


    private fun onLocationReady(task: Task<Location>) {
        // get the current location
        viewModel.currentLocation = task.result
        viewModel.preferenceFragment?.onLocationSetComplete()

//        Toast.makeText(
//            viewModel.getApplication(),
//            "Distance to home : ".plus(viewModel.distanceToHome.toString()),
//            Toast.LENGTH_SHORT
//        ).show()
    }

//    /**
//    * here's what we do when we get a location from the fused location client task
//    * in this app we simply show the current location in the UI
//    * but feel free to do what ever you desire here
//    */
//
//    @SuppressLint("SetTextI18n")
//    private fun setLocation(lastLoc: Location) {
//
//        // get the object for the default Geocoder
//        val geocoder = Geocoder(this, Locale.getDefault())
//
//        // translate lan / lat to street address - call is depreciated !!
//        @Suppress("DEPRECATION")
//        val list = geocoder.getFromLocation(lastLoc.latitude, lastLoc.longitude, 1)
//
//        // present the result on the screen
//        mainBinding.apply {
//            lastLatitude.text = "Latitude\n${list?.get(0)?.latitude}"
//            lastLongitude.text = "Longitude\n${list?.get(0)?.longitude}"
//            lastCountry.text = "Country Name\n${list?.get(0)?.countryName}"
//            lastLocality.text = "Locality\n${list?.get(0)?.locality}"
//            lastAddress.text = "Address\n${list?.get(0)?.getAddressLine(0)}"
//        }
//    }

    /**
     * check if the location services are enabled on the device
     */
    private fun isLocationEnabled(): Boolean {
        val retVal: Boolean
        val locationManager: LocationManager =
            viewModel.getApplication<Application>()
                .getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

        retVal = (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                )

        if (!retVal)
            Toast.makeText(
                viewModel.activity,
                "Please turn on location services on your device",
                Toast.LENGTH_LONG
            )
                .show()
        return retVal
    }

    /**
     * check if the user granted the app permission to the location services
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                viewModel.getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                viewModel.getApplication(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    /**
     * ask the user to grant permissions for the app
     */
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            viewModel.activity!!, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ), permissionId
        )
    }

}
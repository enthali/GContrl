package de.drachenfels.gcontrl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import java.util.concurrent.TimeUnit

class GeoServices(_appActivity: Activity?) {

    private val appActivity: Activity = _appActivity!!

    // Location handling
    private var mFusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appActivity)
    private val permissionId = 2

    // the current location, polled every now and then
    private lateinit var currentLocation: Location

    // the location is updated every now and then
    private var locationPollTime: Long = 10

    init {
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    getLocation()
                }
            },
            0,
            TimeUnit.SECONDS.toMillis(locationPollTime)
        ) //put here time 1000 milliseconds=1 second
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
                mFusedLocationClient.lastLocation.addOnCompleteListener { task ->
                    //   val lastLoc: Location = task.result
                    currentLocation = task.result
                    Toast.makeText(
                        appActivity,
                        "latitude : ".plus(currentLocation.latitude.toString()),
                        Toast.LENGTH_LONG
                    ).show()
//                    setLocation(task.result)
                }
            } else {
                // request the user to turn on the location service on his device
                Toast.makeText(appActivity, "Please turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                //                startActivity(intent)
                appActivity.startActivity(intent)

            }
        } else {
            // ask the user to allow permission
            requestPermissions()
        }
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
        val locationManager: LocationManager =
            appActivity.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * check if the user granted the app permission to the location services
     */
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                appActivity, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                appActivity, Manifest.permission.ACCESS_FINE_LOCATION
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
            appActivity, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
            ), permissionId
        )
    }

//    /**
//     * if the permissions are given, get the latest location now
//     */
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<String>, grantResults: IntArray
//    ) {
//        if (requestCode == permissionId) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                getLocation()
//            }
//        }
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }
}
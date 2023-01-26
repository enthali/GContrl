package de.drachenfels.gcontrl

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.location.Location
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient

class GControlViewModel(application: Application) : AndroidViewModel(application) {


    @SuppressLint("StaticFieldLeak")
    var activity: FragmentActivity? = null

    lateinit var mqttServer: MQTTConnection

    /**
     * 0 - ok
     * 1 - connection failed
     * 2 - publish failed
     */
    var statusMQTT = 0


    lateinit var geoService: GeoServices

    var preferenceFragment: PreferencesFragment? = null

    lateinit var sharedPref: SharedPreferences //sharedPreferences

    /**
     * location values
     */
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    var currentLocation: Location = Location("initial")

    var insideFence = true
    var distanceToHome = 1F

    init {
        // ViewModel init section
    }

    fun initViewModel() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplication())
        geoService = GeoServices(this)
        mqttServer = MQTTConnection(this)
    }
}
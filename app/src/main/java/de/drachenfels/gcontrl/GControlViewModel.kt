package de.drachenfels.gcontrl

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient

class GControlViewModel(application: Application) : AndroidViewModel(application) {


    @SuppressLint("StaticFieldLeak")
    var activity: FragmentActivity? = null

    /**
     * shared preferences - store user preferences persitemt
     */
    lateinit var sharedPref: SharedPreferences //sharedPreferences


    /**
     * MQTT communication related variables
     */

    /**
     * 0 - ok
     * 1 - connection failed
     * 2 - publish failed
     */
    private var _statusMQTT = MutableLiveData(0)
    var statusMQTT: MutableLiveData<Int>
        get() = _statusMQTT
        set(value) {
            _statusMQTT = value
        }

    // instance of the MQTT server connection
    lateinit var mqttServer: MQTTConnection


    /**
     * location related variables
     */
    lateinit var geoService: GeoServices

    var preferenceFragment: PreferencesFragment? = null

    lateinit var mFusedLocationClient: FusedLocationProviderClient
    var currentLocation: Location = Location("initial")

    var insideFence = true


    // distance live data
    private var _distanceToHome = MutableLiveData(0f)
    var distanceToHome: MutableLiveData<Float>
        get() = _distanceToHome
        set(value) {
            _distanceToHome = value
        }

    fun initViewModel() {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplication())
        geoService = GeoServices(this)
        mqttServer = MQTTConnection(this)
    }

}
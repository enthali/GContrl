package de.drachenfels.gcontrl

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.location.Location
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.preference.PreferenceManager

class GControlViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    lateinit var activity: FragmentActivity

    lateinit var mqttServer: MQTTConnection
    lateinit var geoService: GeoServices

    var preferenceFragment: PreferencesFragment? = null

    lateinit var sharedPref: SharedPreferences //sharedPreferences
    lateinit var currentLocation: Location

    init {
        // ViewModel init section
    }

    fun initViewModel()
    {
        mqttServer = MQTTConnection(this)
        geoService = GeoServices(this)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplication())
    }
}
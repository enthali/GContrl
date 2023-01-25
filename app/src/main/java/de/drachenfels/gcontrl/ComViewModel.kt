package de.drachenfels.gcontrl

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel

class ComViewModel : ViewModel() {


    @SuppressLint("StaticFieldLeak")
    lateinit var activity: FragmentActivity
    lateinit var mqttServer: MQTTConnection
    lateinit var geoService: GeoServices
    var preferenceFragment: PreferencesFragment? = null
    lateinit var sp: SharedPreferences //sharedPreferences
    lateinit var currentLocation: Location

}
package de.drachenfels.gcontrl

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class ControlViewModel(application: Application) : AndroidViewModel(application) {


    /**
     * shared preferences - store user preferences persitemt
     */
    lateinit var sharedPreferences: SharedPreferences // TODO investigate = PreferenceManager.getDefaultSharedPreferences(getApplication())


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

    // var preferenceFragment: PreferencesFragment? = null

    // var currentLocation: Location = Location("initial")

    // distance live data
    private var _distanceToHome = MutableLiveData(0f)
    var distanceToHome: MutableLiveData<Float>
        get() = _distanceToHome
        set(value) {
            _distanceToHome = value
        }

    fun initViewModel() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication())
        // geoService = GeoServices(this)
        mqttServer = MQTTConnection(this)
    }

}
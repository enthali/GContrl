package de.drachenfels.gcontrl

import androidx.lifecycle.ViewModel

class ComViewModel : ViewModel() {
    lateinit var mqttServer: MQTTConnection
    lateinit var geoService: GeoServices
}
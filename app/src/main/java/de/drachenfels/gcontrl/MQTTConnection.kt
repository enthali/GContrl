package de.drachenfels.gcontrl

import android.util.Log
import de.drachenfels.gcontrl.modules.*
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MQTTConnection {

    init {
        // register to the distance update
        fenceWatcher.observeForever { fenceState ->
            onFenceStateChange(fenceState)
        }
    }

    private fun onFenceStateChange(fenceState: Int?) {
        // TODO something's wrong here, why is sharedPreferences.... always returning false?
        val enable = true //sharedPreferences.getBoolean("prf_key_geo_auto_control",false)
        if (enable) {
            if (fenceState == HOME_ZONE_ENTERING) {
                sendMessage("open")
            }
            if (fenceState == HOME_ZONE_LEAVING) {
                sendMessage("close")
            }
        }
    }

    // private val viewModel = _viewModel
    private var client: MqttClient? = null

    private fun connect(): Boolean {
        try {
            client = MqttClient(
                getUriFromPreferences(),
                sharedPreferences.getString("mqtt_clientId", ""),
                null
            )
            client!!.setCallback(null)
        } catch (exception: MqttException) {
            exception.printStackTrace()
        }

        // set default option
        val options = MqttConnectOptions()
        // set application specific options
        options.userName = sharedPreferences.getString("mqtt_user", "").toString()
        options.password = sharedPreferences.getString("mqtt_password", "").toString().toCharArray()
        options.isHttpsHostnameVerificationEnabled = false

        try {
            client!!.connect(options)
        } catch (e: MqttException) {
            Log.d(
                javaClass.canonicalName,
                "Connection attempt failed with reason code = " + e.reasonCode + ":" + e.cause
            )
            return false
        }
        return true
    }

    private fun getUriFromPreferences(): String {
        return if (sharedPreferences.getBoolean("mqtt_ssl", false)) {
            "ssl://"
        } else {
            "tcp://"
        }.plus(sharedPreferences.getString("mqtt_uri", "").toString())
            .plus(":")
            .plus(sharedPreferences.getString("mqtt_port", "").toString())
    }

    fun sendMessage(payload: String): Boolean {
        // Now, try to publish a message
        var retVal = false
        if (statusMQTT.value != MQTT_STATUS_OK) statusMQTT.value = MQTT_STATUS_OK
        if (connect()) {
            try {
                val message = MqttMessage()
                val tp = client!!.clientId
                    .plus("/")
                    .plus(sharedPreferences.getString("mqtt_topic", "").toString())
                message.qos = 1
                message.payload = payload.toByteArray()
                client!!.publish(tp, message)
            } catch (e: MqttException) {
                statusMQTT.value = MQTT_STATUS_PUBLISH_FAILD
            }
            retVal = true
        } else {
            statusMQTT.value = MQTT_STATUS_CONNECTION_FAILED
        }
        client!!.disconnect()
        return retVal
    }
}
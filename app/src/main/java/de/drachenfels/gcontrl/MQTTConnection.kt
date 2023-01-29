package de.drachenfels.gcontrl

import android.util.Log
import de.drachenfels.gcontrl.modules.sharedPreferences
import de.drachenfels.gcontrl.modules.statusMQTT
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MQTTConnection {

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
                // publish failed
                statusMQTT.value = 2
            }
            retVal = true
        } else {
            // connection failed
            statusMQTT.value = 1
        }
        client!!.disconnect()
        return retVal
    }
}
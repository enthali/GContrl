package de.drachenfels.gcontrl

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MQTTConnection(_viewModel: GControlViewModel) {

    private val viewModel = _viewModel

    private var client: MqttClient? = null

    private fun connect(): Boolean {
        try {
            client = MqttClient(
                getUriFromPreferences(),
                viewModel.sharedPreferences.getString("mqtt_clientId", ""),
                null
            )
            client!!.setCallback(null)
        } catch (exception: MqttException) {
            exception.printStackTrace()
        }

        // set default option
        val options = MqttConnectOptions()
        // set application specific options
        options.userName = viewModel.sharedPreferences.getString("mqtt_user", "").toString()
        options.password = viewModel.sharedPreferences.getString("mqtt_password", "").toString().toCharArray()
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
        return if (viewModel.sharedPreferences.getBoolean("mqtt_ssl", false) == true) {
            "ssl://"
        } else {
            "tcp://"
        }.plus(viewModel.sharedPreferences.getString("mqtt_uri", "").toString())
            .plus(":")
            .plus(viewModel.sharedPreferences.getString("mqtt_port", "").toString())
    }

     fun sendMessage(payload: String): Boolean {
        // Now, try to publish a message
        var retVal = false
        if (connect()) {
            try {
                val message = MqttMessage()
                val tp = client!!.clientId
                    .plus("/")
                    .plus(viewModel.sharedPreferences.getString("mqtt_topic", "").toString())
                message.qos = 1
                message.payload = payload.toByteArray()
                client!!.publish(tp, message)
            } catch (e: MqttException) {
                // publish failed
                viewModel.statusMQTT.value = 2
            }
            retVal = true
        } else {
            // connection failed
            viewModel.statusMQTT.value = 1
        }
        client!!.disconnect()
        return retVal
    }
/*
At this point mqtt call back function are not needed for this app

    fun connectionLost(cause: Throwable) {
        Log.d("MQTT", "MQTT Server connection lost" + cause.message)
    }

    fun messageArrived(topic: String, message: MqttMessage) {
        Log.d("MQTT", "Message arrived:$topic:$message")
    }

    fun deliveryComplete(token: IMqttDeliveryToken?) {
        Log.d("MQTT", "Delivery complete")
    }
    */

}
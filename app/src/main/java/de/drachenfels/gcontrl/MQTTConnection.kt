package de.drachenfels.gcontrl

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

class MQTTConnection {

    private var client: MqttClient? = null

    fun connect(serverURI: String, clientId: String, username: String, password: String): Boolean {
        try {
            client = MqttClient(serverURI, clientId, null)
            client!!.setCallback(null)
        } catch (exception: MqttException) {
            exception.printStackTrace()
        }

        // set default option
        val options = MqttConnectOptions()
        // set application specific options
        options.userName = username
        options.password = password.toCharArray()
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

    fun disconnect() {
        client!!.disconnect()
    }

    fun sendMessage(topic: String, payload: String): Boolean {
        // Now, try to publish a message
        try {
            val message = MqttMessage()
            val tp = client!!.clientId + "/" + topic
            message.qos = 1
            message.payload = payload.toByteArray()
            client!!.publish(tp, message)
        } catch (e: MqttException) {
            Log.d(javaClass.canonicalName, "Publish failed with reason code = " + e.reasonCode)
            return false
        }
        return true
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
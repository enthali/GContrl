package de.drachenfels.gcontrl.modules

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


var enableAutoDoorControl = false

fun onFenceStateChange(fenceState: Int?) {
    if (enableAutoDoorControl) {
        if (fenceState == HOME_ZONE_ENTERING) {
            mqttSendMessage("open")
        }
        if (fenceState == HOME_ZONE_LEAVING) {
            mqttSendMessage("close")
        }
    }
}

// private val viewModel = _viewModel
private var client: MqttClient? = null

private fun mqttConnect(): Boolean {
    try {
        client = MqttClient(
            mqttGetUriFromPreferences(),
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
        statusMQTT.postValue(MQTT_STATUS_CONNECTION_FAILED)
//            Log.d(
//                javaClass.canonicalName,
//                "Connection attempt failed with reason code = " + e.reasonCode + ":" + e.cause
//            )
        return false
    }
    return true
}

private fun mqttGetUriFromPreferences(): String {
    return if (sharedPreferences.getBoolean("mqtt_ssl", false)) {
        "ssl://"
    } else {
        "tcp://"
    }.plus(sharedPreferences.getString("mqtt_uri", "").toString())
        .plus(":")
        .plus(sharedPreferences.getString("mqtt_port", "").toString())
}

fun mqttSendMessage(payload: String): Boolean {
    // Now, try to publish a message
    var retVal = false
    if (statusMQTT.value != MQTT_STATUS_OK) statusMQTT.value = MQTT_STATUS_OK
    if (mqttConnect()) {
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

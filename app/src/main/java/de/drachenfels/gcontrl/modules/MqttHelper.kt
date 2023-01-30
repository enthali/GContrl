package de.drachenfels.gcontrl.modules

import androidx.lifecycle.MutableLiveData
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage

const val MQTT_STATUS_OK = 0
const val MQTT_DOOR_OPEN = 1
const val MQTT_DOOR_CLOSE = 2
const val MQTT_STATUS_CONNECTION_FAILED = 16
const val MQTT_STATUS_PUBLISH_FAILD = 32
private var _statusMQTT = MutableLiveData(0)
/**
 * MQTT status can be observed to provide feedback to users
 * 0 - ok
 * 1 - connection failed
 * 2 - publish failed
 */
var statusMQTT: MutableLiveData<Int>
    get() = _statusMQTT
    set(value) {
        _statusMQTT = value
    }
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
            // there's no feedback from the door, but at least there's feedback from the MQTT Server
            // which is currently used to represent the door status
            // the command chain past the MQTT server is not transparent to the app
            if (payload == "open") {
                statusMQTT.postValue(MQTT_DOOR_OPEN)
            } else
            {
                statusMQTT.postValue(MQTT_DOOR_CLOSE)
            }
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

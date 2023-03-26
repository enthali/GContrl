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
 * 1 - error
 * 2 - door open
 * 4 - door close
 * 8 - ***
 * 16 - connection failed
 * 32 - publish failed
 */
var statusMQTT: MutableLiveData<Int>
    get() = _statusMQTT
    set(value) {
        _statusMQTT = value
    }

var enableAutoDoorControl = false

/**
 * Handles automatic door control based on the fence state change.
 * The onFenceStateChange function checks if the enableAutoDoorControl flag is set to true,
 * indicating that automatic door control is enabled.
 * If so, it checks the new fenceState parameter passed to the function.
 * If the state is HOME_ZONE_ENTERING, it sends a message to open the door using the mqttSendMessage function.
 * If the state is HOME_ZONE_LEAVING, it sends a message to close the door using the same function.
 * @param fenceState the new fence state
 */
fun onFenceStateChange(fenceState: Int?) {
    if (enableAutoDoorControl) {
        if (fenceState == HOME_ZONE_ENTERING) {
            // send a message to open the door when entering home zone
            mqttSendMessage("open")
        }
        if (fenceState == HOME_ZONE_LEAVING) {
            // send a message to close the door when leaving home zone
            mqttSendMessage("close")
        }
    }
}


// private val viewModel = _viewModel
private var client: MqttClient? = null

/**
 * Establishes a connection to the MQTT broker.
 *
 * @return `true` if the connection was successful, `false` otherwise
 */
private fun mqttConnect(): Boolean {
    try {
        // Create an instance of the MqttClient with the URI, client ID, and persistence provided
        client = MqttClient(
            mqttGetUriFromPreferences(),
            sharedPreferences.getString("mqtt_clientId", ""),
            null
        )

        // Set the callback function to null (not used)
        client!!.setCallback(null)
    } catch (exception: MqttException) {
        // Handle any exceptions thrown during initialization
        exception.printStackTrace()
        statusMQTT.postValue(MQTT_STATUS_CONNECTION_FAILED)
        return false
    }

    // Set up the connection options with the username, password, and SSL verification settings
    val options = MqttConnectOptions()
    options.userName = sharedPreferences.getString("mqtt_user", "").toString()
    options.password = sharedPreferences.getString("mqtt_password", "").toString().toCharArray()
    options.isHttpsHostnameVerificationEnabled = false

    try {
        // Connect to the broker with the specified options
        client!!.connect(options)
    } catch (e: MqttException) {
        // Handle any exceptions thrown during connection
        e.printStackTrace()
        statusMQTT.postValue(MQTT_STATUS_CONNECTION_FAILED)
        return false
    }

    // If the connection is successful, return true
    return true
}


/**
 * Builds the MQTT broker URI from the shared preferences.
 * This function determines whether to use SSL or not based on the mqtt_ssl preference
 * in the shared preferences. It then builds the URI string by concatenating the protocol,
 * the URI, and the port. The URI and port are obtained from the mqtt_uri
 * and mqtt_port preferences, respectively.
 * @return The URI for the MQTT broker.
 */
private fun mqttGetUriFromPreferences(): String {
    // Determine whether to use SSL or not.
    val protocol = if (sharedPreferences.getBoolean("mqtt_ssl", false)) {
        "ssl://"
    } else {
        "tcp://"
    }

    // Build the URI string.
    return protocol.plus(sharedPreferences.getString("mqtt_uri", "").toString())
        .plus(":")
        .plus(sharedPreferences.getString("mqtt_port", "").toString())
}


/**
 * Sends an MQTT message with the specified payload to the broker.
 * Publishes a message to an MQTT broker based on the user's MQTT configuration.
 * If the message is "open", updates the MQTT status to MQTT_DOOR_OPEN.
 * If the message is "close", updates the MQTT status to MQTT_DOOR_CLOSE.
 * If a connection cannot be established to the MQTT broker, updates the MQTT status to MQTT_STATUS_CONNECTION_FAILED.
 * If the message fails to be published, updates the MQTT status to MQTT_STATUS_PUBLISH_FAILD.
 *
 * @param payload the message payload to publish
 * @return true if the message was successfully published, false otherwise
*/
fun mqttSendMessage(payload: String): Boolean {
    // Set default return value to false
    var retVal = false

    // If MQTT status is not OK, set it to OK
    if (statusMQTT.value != MQTT_STATUS_OK) statusMQTT.value = MQTT_STATUS_OK

    try {
        // Connect to the MQTT broker
        if (mqttConnect()) {
            // Create a new message with the specified payload
            val message = MqttMessage()
            // Set the message quality of service to 1 (at least once)
            message.qos = 1
            // Set the message payload
            message.payload = payload.toByteArray()
            // Construct the topic string
            val tp = client!!.clientId
                .plus("/")
                .plus(sharedPreferences.getString("mqtt_topic", "").toString())
            // Publish the message to the broker
            client!!.publish(tp, message)

            // Set the MQTT status to indicate that the door is opening or closing
            if (payload == "open") {
                statusMQTT.postValue(MQTT_DOOR_OPEN)
            } else {
                statusMQTT.postValue(MQTT_DOOR_CLOSE)
            }

            // Disconnect from the MQTT broker
            client!!.disconnect()
            // Set the return value to true
            retVal = true
        } else {
            // If MQTT connection fails, set the status to indicate the failure
            statusMQTT.postValue(MQTT_STATUS_CONNECTION_FAILED)
        }
    } catch (e: MqttException) {
        // If an exception occurs, print the stack trace and set the status to indicate the failure
        e.printStackTrace()
        statusMQTT.postValue(MQTT_STATUS_PUBLISH_FAILD)
    }

    // Return the result of the operation
    return retVal
}


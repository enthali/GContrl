/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.drachenfels.gcontrl.modules

import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.MutableLiveData
import de.drachenfels.gcontrl.MQTTConnection

/**
 * Returns the `location` object as a human readable string.
 */
fun Location?.toText(): String {
    return if (this != null) {
        "($latitude, $longitude)"
    } else {
        "Unknown location"
    }
}

/**
 * the application wide shared preferences will be available during ControlFragment onCreate
 */
lateinit var sharedPreferences: SharedPreferences


/**
 * the mqtt server is initialized in ControlFragment onCreate and application wide
 * available afterwards
 */
lateinit var mqttServer: MQTTConnection

/**
 * MQTT status can be observed to provide feedback to users
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

/**
 * The object contains shared resources used between the ControlView and the Location Service
 */
internal object SharedLocationResources {

    private var privateCurrentLocation = MutableLiveData<Location>(Location("initLocation"))
    var currentLocation: MutableLiveData<Location>
        get() = privateCurrentLocation
        set(value) {
            privateCurrentLocation = value
        }

    // distance live data
    private var privateDistanceToHome = MutableLiveData(0f)

    /**
     * any changes to the calculation of the distance to home can be observed applicatoin wide
     */
    var distanceToHome: MutableLiveData<Float>
        get() = privateDistanceToHome
        set(value) {
            privateDistanceToHome = value
        }
}
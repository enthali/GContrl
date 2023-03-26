/*
 * Copyright 2023 Georg Doll
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

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.MutableLiveData
import de.drachenfels.gcontrl.R

/**
 * Declare a global variable to hold the application-wide SharedPreferences object.
 * The `lateinit` modifier indicates that this variable will be initialized later.
 */
lateinit var sharedPreferences: SharedPreferences

/**
 * Declare a global variable to hold the current location, as a MutableLiveData<Location>.
 * The `private` modifier indicates that this variable can only be accessed within this module.
 * The `lateinit` modifier indicates that this variable will be initialized later.
 */
private var privateCurrentLocation = MutableLiveData(Location("initLocation"))
var currentLocation: MutableLiveData<Location>
    get() = privateCurrentLocation
    set(value) {
        privateCurrentLocation = value
    }

/**
 * Declare a global variable to hold the distance to home, as a MutableLiveData<Int>.
 * The `private` modifier indicates that this variable can only be accessed within this module.
 * The initial value of this variable is 0.
 */
private var privateDistanceToHome = MutableLiveData(0)

/**
 * Declare a global variable to hold the distance to home, as a MutableLiveData<Int>.
 * This variable is public and can be accessed from other modules.
 */
var distanceToHome: MutableLiveData<Int>
    get() = privateDistanceToHome
    set(value) {
        privateDistanceToHome = value
    }

/**
 * This function takes a distance value and returns a compiled distance to home string.
 * The distance string is constructed based on the input distance value.
 */
fun distanceToText(context: Context, distance: Int): String {
    return context.getString(R.string.distance_to_home_text)
        .plus(
            when (distance) {
                in 0..999 ->
                    (distance.toString())
                        .plus("m")
                in 1000..49999 ->
                    (distance / 1000).toString()
                        .plus(".")
                        .plus((distance % 1000).div(100))
                        .plus("km")
                else ->
                    (distance / 1000).toString()
                        .plus("km")
            }
        )
}

/**
 * Declare a global variable to count the number of locations that have been checked.
 * This variable is initialized to 0.
 */
var locationCount = 0

/**
 * Declare a set of constants that define the different states of the home fence.
 * These constants can be accessed from other modules.
 */
const val HOME_ZONE_INSIDE = 0
const val HOME_ZONE_LEAVING = 1
const val HOME_ZONE_ENTERING = 2
const val HOME_ZONE_OUTSIDE = 4
const val HOME_ZONE_UNKNOWN = 99

/**
 * Declare a global variable to hold the fence watcher, as a MutableLiveData<Int>.
 * The `private` modifier indicates that this variable can only be accessed within this module.
 * The initial value of this variable is HOME_ZONE_UNKNOWN.
 */
private var privateFenceWatcher = MutableLiveData(HOME_ZONE_UNKNOWN)


/**
 * any changes to the fence watcher can be observed
 */
var fenceWatcher: MutableLiveData<Int>
    get() = privateFenceWatcher
    set(value) {
        privateFenceWatcher = value
    }



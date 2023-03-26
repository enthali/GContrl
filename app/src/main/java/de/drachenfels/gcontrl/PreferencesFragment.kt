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
package de.drachenfels.gcontrl

import android.os.Bundle
import android.text.InputType
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import de.drachenfels.gcontrl.modules.currentLocation
import de.drachenfels.gcontrl.modules.locationCount
import de.drachenfels.gcontrl.modules.sharedPreferences

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        /**
         * show the current build version and text in the preferences
         */
        val versionPref: Preference? = findPreference(getString(R.string.prf_key_version))
        val versionCode: Int = BuildConfig.VERSION_CODE
        versionPref?.title = "Version ".plus(versionCode.toString())
        versionPref?.summary = BuildConfig.VERSION_NAME

        val portPreference: EditTextPreference? = findPreference("port")
        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        /**
         * geo services activities on preference switch
         */

        // switch view of geo preferences visibility according to Enable location based Features flag
//        val enabled =
//            sharedPreferences.getBoolean(
//                getString(R.string.prf_key_geo_enable_location_features),
//                false
//            )

        val geoServiceEnabled: SwitchPreference? =
            findPreference(getString(R.string.prf_key_geo_enable_location_features))
        geoServiceEnabled?.setOnPreferenceClickListener { refreshFragment() }

        val geoSetHomeLocation: Preference? =
            findPreference(getString(R.string.prf_key_geo_set_home_location))
//        geoSetHomeLocation?.isVisible = enabled
        geoSetHomeLocation?.summary = getString(R.string.geo_setHomeLocationSummary)
            .plus("\nCurrent Home Location : ")
            .plus("\nLat : ")
            .plus(
                sharedPreferences.getString(
                    getString(R.string.prf_key_geo_latitude),
                    "null"
                ).toString()
            )
            .plus("\nLon :")
            .plus(
                sharedPreferences.getString(
                    getString(R.string.prf_key_geo_longitude),
                    "null"
                ).toString()
            )

//        val geoFenceSize: EditTextPreference? =
//            findPreference(getString(R.string.prf_key_geo_fence_size))
//        geoFenceSize?.isVisible = enabled
//
//        val geoEnableProtect: SwitchPreference? =
//            findPreference(getString(R.string.prf_key_geo_enable_protect))
//        geoEnableProtect?.isVisible = enabled
//
//        val geoAutoControl: SwitchPreference? =
//            findPreference(getString(R.string.prf_key_geo_auto_control))
//        geoAutoControl?.isVisible = enabled

        /**
         * handle setting the geo home location
         * bind the setCurrentHomeLocation in the GeoService class object to the
         * preferences on click Listener
         */
        geoSetHomeLocation?.setOnPreferenceClickListener {

            // prevent the location state machine to detect a fence change
            // by ignoring the next 2 location updates
            locationCount = 0

            // publish the new locations
            sharedPreferences.edit().putString(
                getString(R.string.prf_key_geo_latitude),
                currentLocation.value?.latitude.toString()
            ).apply()
            sharedPreferences.edit().putString(
                getString(R.string.prf_key_geo_longitude),
                currentLocation.value?.longitude.toString()
            ).apply()
            refreshFragment()
        }
    }

    private fun refreshFragment(): Boolean {
        // This method refreshes the fragment
        findNavController().run {
            popBackStack()
            navigate(R.id.settingsFragment)
        }
        return true
    }

}



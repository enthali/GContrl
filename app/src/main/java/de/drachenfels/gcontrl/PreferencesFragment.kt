package de.drachenfels.gcontrl

import android.os.Bundle
import android.text.InputType
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import de.drachenfels.gcontrl.modules.*

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val portPreference: EditTextPreference? = findPreference("port")
        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        /**
         * geo services activities on preference switch
         */

        // switch view of geo preferences visibility according to Enable location based Features flag
        val enabled =
            sharedPreferences.getBoolean(
                getString(R.string.prf_key_geo_enable_location_features),
                false
            )

        val geoServiceEnabled: SwitchPreference? =
            findPreference(getString(R.string.prf_key_geo_enable_location_features))
        geoServiceEnabled?.setOnPreferenceClickListener { refreshFragment() }

        val geoSetHomeLocation: Preference? =
            findPreference(getString(R.string.prf_key_geo_set_home_location))
        geoSetHomeLocation?.isVisible = enabled
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

        val geoFenceSize: EditTextPreference? =
            findPreference(getString(R.string.prf_key_geo_fence_size))
        geoFenceSize?.isVisible = enabled

        val geoEnableProtect: SwitchPreference? =
            findPreference(getString(R.string.prf_key_geo_enable_protect))
        geoEnableProtect?.isVisible = enabled

        val geoAutoControl: SwitchPreference? =
            findPreference(getString(R.string.prf_key_geo_auto_control))
        geoAutoControl?.isVisible = enabled

        /**
         * handle setting the geo home location
         * bind the setCurrentHomeLocation in the GeoService class object to the
         * preferences on click Listener
         */
        geoSetHomeLocation?.setOnPreferenceClickListener {
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



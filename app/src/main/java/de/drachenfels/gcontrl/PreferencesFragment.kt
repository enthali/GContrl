package de.drachenfels.gcontrl

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference


class PreferencesFragment : PreferenceFragmentCompat() {

    private val viewModel: ControlViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val portPreference: EditTextPreference? = findPreference("port")
        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        /**
         * geo services activities on preference switch
         */
//        val serviceIntent = Intent(activity, ForegroundOnlyLocationService::class.java)
//        serviceIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        /**
         * switch view of geo preferences visibility according to Enable location based Features flag
         */
        val enabled =
            viewModel.sharedPreferences.getBoolean("geo_enable_location_features", false)

        val geoServiceEnabled: SwitchPreference? = findPreference("geo_enable_location_features")
        geoServiceEnabled?.setOnPreferenceClickListener { refreshFragment() }

        val geoSetHomeLocation: Preference? = findPreference("geo_setHomeLocation")
        geoSetHomeLocation?.isVisible = enabled
        geoSetHomeLocation?.summary = getString(R.string.geo_setHomeLocationSummary)
            .plus("\nCurrent Home Location : ")
            .plus("\nLat : ")
            .plus(viewModel.sharedPreferences.getString("geo_latitude", "null").toString())
            .plus("\nLon :")
            .plus(viewModel.sharedPreferences.getString("geo_longitude", "null").toString())

        val geoFenceSize: EditTextPreference? = findPreference("geo_fence_size")
        geoFenceSize?.isVisible = enabled

        val geoEnableProtect: SwitchPreference? = findPreference("geo_enable_protect")
        geoEnableProtect?.isVisible = enabled

        val geoAutoControl: SwitchPreference? = findPreference("geo_autoControl")
        geoAutoControl?.isVisible = enabled


        /**
         * handle setting the geo home location
         * bind the setCurrentHomeLocation in the GeoService class object to the
         * preferences on click Listener
         */
        geoSetHomeLocation?.setOnPreferenceClickListener {
            viewModel.sharedPreferences.edit().putString("geo_latitude", viewModel.currentLocation.latitude.toString()).apply()
            viewModel.sharedPreferences.edit().putString("geo_longitude", viewModel.currentLocation.longitude.toString()).apply()
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



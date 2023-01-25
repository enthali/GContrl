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

    private val viewModel: ComViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val portPreference: EditTextPreference? = findPreference("port")
        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }
        /**
         * hide the geo services if they aren't enabled
         */

        val geoServiceEnabled: SwitchPreference? = findPreference("geo_enable_location_features")
        geoServiceEnabled?.setOnPreferenceClickListener { refreshFragment() }

        val geoSetHomeLocation: Preference? = findPreference("geo_setHomeLocation")
        geoSetHomeLocation?.isVisible =
            viewModel.sp.getBoolean("geo_enable_location_features", false) == true
        geoSetHomeLocation?.summary = getString(R.string.geo_setHomeLocationSummary)
            .plus("\nCurrent Home Location : ")
            .plus("\nLat : ")
            .plus(viewModel.sp.getString("geo_latitude", "null").toString())
            .plus("\nLon :")
            .plus(viewModel.sp.getString("geo_longitude", "null").toString())

        val geoFenceSize: EditTextPreference? = findPreference("geo_fence_size")
        geoFenceSize?.isVisible =
            viewModel.sp.getBoolean("geo_enable_location_features", false) == true

        val geoEnableProtect: SwitchPreference? = findPreference("geo_enable_protect")
        geoEnableProtect?.isVisible =
            viewModel.sp.getBoolean("geo_enable_location_features", false) == true

        val geoAutoControl: SwitchPreference? = findPreference("geo_autoControl")
        geoAutoControl?.isVisible =
            viewModel.sp.getBoolean("geo_enable_location_features", false) == true


        /**
         * handle setting the geo home location
         * bind the setCurrentHomeLocation in the GeoService class object to the
         * preferences on click Listener
         */
        geoSetHomeLocation?.setOnPreferenceClickListener {
            setCurrentLocation()
        }
    }

    private fun setCurrentLocation() :Boolean {
        viewModel.preferenceFragment = this
        val retVal = viewModel.geoService.setCurrentHomeLocation()
        refreshFragment()
        return retVal
    }

    fun onLocationSetComplete() {
        viewModel.sp.edit().putString("geo_latitude", viewModel.currentLocation.latitude.toString()).apply()
        viewModel.sp.edit().putString("geo_longitude", viewModel.currentLocation.longitude.toString()).apply()
        viewModel.preferenceFragment = null
        // TODO figure out how to refresh the screen in this call back
    }

    fun refreshFragment(): Boolean {
        // This method refreshes the fragment
        findNavController().run {
            popBackStack()
            navigate(R.id.settingsFragment)
        }
        return true
    }

}



package de.drachenfels.gcontrl

import android.os.Bundle
import android.text.InputType
import androidx.navigation.fragment.findNavController
import androidx.preference.*


class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val sharedPreferences =
            context?.let { PreferenceManager.getDefaultSharedPreferences(it /* Activity context */) }

        val portPreference: EditTextPreference? = findPreference("port")
        portPreference?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
        }

        val geoServiceEnabled: SwitchPreference? = findPreference("enable_location_features")
        geoServiceEnabled?.setOnPreferenceClickListener { refreshFragment() }

        val geoSetHomeLocation: Preference? = findPreference("geo_setHomeLocation")
        geoSetHomeLocation?.isVisible =
            sharedPreferences?.getBoolean("enable_location_features", false) == true

        val geoFenceSize: EditTextPreference? = findPreference("geo_fence_size")
        geoFenceSize?.isVisible =
            sharedPreferences?.getBoolean("enable_location_features", false) == true

        val geoEnableProtect: SwitchPreference? = findPreference("geo_enable_protect")
        geoEnableProtect?.isVisible =
            sharedPreferences?.getBoolean("enable_location_features", false) == true

        val geoAutoControl: SwitchPreference? = findPreference("geo_autocontrol")
        geoAutoControl?.isVisible =
            sharedPreferences?.getBoolean("enable_location_features", false) == true

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


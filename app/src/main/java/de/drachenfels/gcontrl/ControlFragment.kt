package de.drachenfels.gcontrl

import android.Manifest
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drachenfels.gcontrl.databinding.FragmentControlBinding
import de.drachenfels.gcontrl.modules.*

private const val TAG = "ControlFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

/**
 * a fragment to control the garage door directly though two buttons "open" and "close"
 */
class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView()")
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_direct_control, container, false)
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated()")
        super.onViewCreated(view, savedInstanceState)
        // bind the buttons
        binding.openbutton.setOnClickListener { manageDoor(1) }
        binding.closebutton.setOnClickListener { manageDoor(0) }

        distanceToHome.observe(viewLifecycleOwner) { newDistance ->
            onDistanceChange(newDistance)
        }

        statusMQTT.observe(viewLifecycleOwner) { newStatus ->
            onMqttStatusChange(newStatus)
        }

        // make sure we get location permissions if they are enabled
        if (sharedPreferences.getBoolean(
                getString(R.string.prf_key_geo_enable_location_features), false
            )
        ) requestForegroundPermissions()
    }

    /**
     * update the UI with the new distance
     */
    private fun onDistanceChange(distance: Int) {
        binding.distanceText.text = distanceToText(requireContext(),distance)
        binding.distanceBar.max = distance
        binding.distanceBar.progress =
            sharedPreferences.getString(getString(R.string.prf_key_geo_fence_size), "0").toString()
                .toInt()
    }

    /**
     * toast the server status to the UI
     */
    private fun onMqttStatusChange(status: Int?) {
        when (status) {

            MQTT_DOOR_CLOSE, MQTT_DOOR_OPEN -> {
                Toast.makeText(
                    activity?.applicationContext,
                    "The door is ".plus(if (status == MQTT_DOOR_OPEN) "open" else "close"),
                    Toast.LENGTH_LONG
                ).show()
                // reset the MQTT to status Ok
                statusMQTT.postValue(MQTT_STATUS_OK)
            }

            MQTT_STATUS_CONNECTION_FAILED -> {
                Toast.makeText(
                    activity?.applicationContext, "connection to server failed", Toast.LENGTH_LONG
                ).show()
            }

            MQTT_STATUS_PUBLISH_FAILD -> {
                Toast.makeText(
                    activity?.applicationContext, "door command send failed", Toast.LENGTH_LONG
                ).show()
            }

            else -> {}
        }
    }

    /**
     * onStart() is called each time the fragment comes back into focus
     * the best place to reset the private variables from the preferences
     */
    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()

        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext, "Network connection required !!", Toast.LENGTH_LONG
            ).show()
        }
//        enableLocationServiceView()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
//        enableLocationServiceView()
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext, "Network connection required !!", Toast.LENGTH_LONG
            ).show()
        }
    }


//    /**
//     * switch view of location services view according to 'Enable location based Features' flag
//     */
//    private fun enableLocationServiceView() {
//        Log.d(TAG, "enableLocationServiceView()")
//        val enabled = sharedPreferences.getBoolean(
//            getString(R.string.prf_key_geo_enable_location_features),
//            false
//        )
//        // hide the location related information on the screen
//        // if location services are disabled in the preferences
//        // adopt the layout if geo-services are enabled or not
//        if (enabled) {
//            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 1.0f
//        } else {
//            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 0.0f
//        }
//    }

    /**
     *  Review Permissions: Method checks if permissions approved.
     */
    private fun foregroundPermissionApproved(): Boolean {
        Log.d(TAG, "foregroundPermissionApproved()")

        val result = PackageManager.PERMISSION_GRANTED == activity?.let {
            ActivityCompat.checkSelfPermission(
                it, Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        Log.d(TAG, "foregroundPermissionApproved() result : ".plus(result.toString()))
        return result
    }

    /**
     * Review Permissions: Method requests permissions.
     */
    private fun requestForegroundPermissions() {
        Log.d(TAG, "requestForegroundPermission()")
        val provideRationale = foregroundPermissionApproved()
        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
//            Snackbar.make(
//                requireView(),
//                //findViewById(R.id.directControl),
//                R.string.permission_rationale,
//                Snackbar.LENGTH_LONG
//            )
//                .setAction(R.string.ok) {
//                    // Request permission
//                    activity?.let { it1 ->
//                        ActivityCompat.requestPermissions(
//                            it1,
//                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
//                        )
//                    }
//                }
//                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    /**
     * door handling
     */
    private fun manageDoor(cmd: Int) {
        Log.d(TAG, "manageDoor()")
        val cmdString = when (cmd) {
            0 -> "close"
            1 -> "open"
            else -> "status"
        }
        if (isConnected()) {

            val enableButtons: Boolean = if (sharedPreferences.getBoolean(
                    getString(R.string.prf_key_geo_enable_location_features), false
                ) && sharedPreferences.getBoolean(
                    getString(R.string.prf_key_geo_enable_protect), false
                )
            ) {
                fenceWatcher.value == HOME_ZONE_INSIDE
            } else {
                true
            }

            if (enableButtons) mqttSendMessage(cmdString)
        }
    }

    /**
     * check if there is access to the internet
     */
    private fun isConnected(): Boolean {
        Log.d(TAG, "isConnected()")
        var result = false
        val cm = activity?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        if (capabilities != null) {
            result = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(
                    NetworkCapabilities.TRANSPORT_CELLULAR
                ) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
                else -> false
            }
        }
        if (!result) {
            Toast.makeText(
                activity?.applicationContext,
                "No network connection - please try again",
                Toast.LENGTH_LONG
            ).show()
        }
        return result
    }
}
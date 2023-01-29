@file:Suppress("DEPRECATION")

package de.drachenfels.gcontrl

import android.Manifest
import android.content.*
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import de.drachenfels.gcontrl.databinding.FragmentControlBinding
import de.drachenfels.gcontrl.modules.*
import de.drachenfels.gcontrl.services.LocationService


private const val TAG = "ControlFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

/**
 * a fragment to control the garage door directly though two buttons "open" and "close"
 */
class ControlFragment : Fragment() {

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    // >>>>>> LOCATION <<<<<<
    private var foregroundOnlyLocationServiceBound = false

    // Provides location updates for while-in-use feature.
    private var locationService: LocationService? = null

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "onServiceConnected()")
            val binder = service as LocationService.LocalBinder
            locationService = binder.service
            foregroundOnlyLocationServiceBound = true

            // enable the location updates
            locationService?.subscribeToLocationUpdates()

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "onServiceDisconnected()")
            locationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }
    // <<<<<< LOCATION >>>>>>

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
//        mqttServer = MQTTConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

        distanceToHome.observe(
            viewLifecycleOwner
        ) { newDistance ->
            binding.distanceText.text = distanceToText(newDistance)
            binding.distanceBar.max = newDistance.toInt()
            binding.distanceBar.progress =
                sharedPreferences.getString(getString(R.string.prf_key_geo_fence_size), "0")
                    .toString().toInt()
        }

        // make sure we get location permissions if they are enabled
        if (sharedPreferences.getBoolean(
                getString(R.string.prf_key_geo_enable_location_features),
                false
            )
        )
            requestForegroundPermissions()
    }

    /**
     * onStart() is called each time the fragment comes back into focus
     * the best place to reset the private variables from the preferences
     */
    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()

        val serviceIntent = Intent(activity, LocationService::class.java)

        requireActivity().bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )

        // register live data - updated by the service
        // SharedLocationResources.currentLocation.observe(this) { onLocationUpdate() }

        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext,
                "Network connection required !!",
                Toast.LENGTH_LONG
            ).show()
        }
        enableLocationServiceView()
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        enableLocationServiceView()
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext,
                "Network connection required !!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause()")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop()")
        if (foregroundOnlyLocationServiceBound) {
            activity?.unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        super.onStop()
    }

    /**
     * switch view of location services view according to 'Enable location based Features' flag
     */
    private fun enableLocationServiceView() {
        Log.d(TAG, "enableLocationServiceView()")
        val enabled = sharedPreferences.getBoolean(
            getString(R.string.prf_key_geo_enable_location_features),
            false
        )
        // hide the location related information on the screen
        // if location services are disabled in the preferences
        // adopt the layout if geo-services are enabled or not
        if (enabled) {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 1.0f
        } else {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 0.0f
        }
    }

    /**
     *  Review Permissions: Method checks if permissions approved.
     */
    private fun foregroundPermissionApproved(): Boolean {
        Log.d(TAG, "foregroundPermissionApproved()")
        // TOTO does the .let call work ??
        val result = PackageManager.PERMISSION_GRANTED == activity?.let {
            ActivityCompat.checkSelfPermission(
                it,
                Manifest.permission.ACCESS_FINE_LOCATION
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
            Snackbar.make(
                requireView(),
                //findViewById(R.id.directControl),
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                // TOTO does the .let call work ??
                .setAction(R.string.ok) {
                    // Request permission
                    activity?.let { it1 ->
                        ActivityCompat.requestPermissions(
                            it1,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                        )
                    }
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            // TODO check if this .let call works ??
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }


    // TODO work out on how to handle depreciated call
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult()")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    locationService?.subscribeToLocationUpdates()
                else -> {
                    // Permission denied.
                    Snackbar.make(
                        requireView(),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("settings") {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
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

            val enableButtons: Boolean

            if (sharedPreferences.getBoolean(
                    getString(R.string.prf_key_geo_enable_location_features),
                    false
                )
                && sharedPreferences.getBoolean(
                    getString(R.string.prf_key_geo_enable_protect),
                    false
                )
            ) {
                enableButtons = fenceWatcher.value == HOME_ZONE_INSIDE
            } else {
                enableButtons = true
            }

            if (enableButtons)
                if (mqttSendMessage(cmdString)) {
                    Toast.makeText(
                        activity?.applicationContext,
                        cmdString.plus(" the door"),
                        Toast.LENGTH_LONG
                    ).show()
                }
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
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
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
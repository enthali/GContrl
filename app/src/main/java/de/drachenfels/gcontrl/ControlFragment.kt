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

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drachenfels.gcontrl.databinding.FragmentControlBinding
import de.drachenfels.gcontrl.modules.HOME_ZONE_INSIDE
import de.drachenfels.gcontrl.modules.MQTT_DOOR_CLOSE
import de.drachenfels.gcontrl.modules.MQTT_DOOR_OPEN
import de.drachenfels.gcontrl.modules.MQTT_STATUS_CONNECTION_FAILED
import de.drachenfels.gcontrl.modules.MQTT_STATUS_OK
import de.drachenfels.gcontrl.modules.MQTT_STATUS_PUBLISH_FAILD
import de.drachenfels.gcontrl.modules.distanceToHome
import de.drachenfels.gcontrl.modules.distanceToText
import de.drachenfels.gcontrl.modules.fenceWatcher
import de.drachenfels.gcontrl.modules.mqttSendMessage
import de.drachenfels.gcontrl.modules.sharedPreferences
import de.drachenfels.gcontrl.modules.statusMQTT

private const val TAG = "ControlFragment"

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
    }

    override fun onResume() {
        Log.d(TAG, "onResume()")
        super.onResume()
        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext, "Network connection required !!", Toast.LENGTH_LONG
            ).show()
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
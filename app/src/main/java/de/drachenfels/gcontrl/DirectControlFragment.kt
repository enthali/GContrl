package de.drachenfels.gcontrl

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drachenfels.gcontrl.databinding.FragmentDirectControlBinding


/**
 * a fragment to control the garage door directly though two buttons "open" and "close"
 */
class DirectControlFragment : Fragment() {

    // private variables - connection information
    private lateinit var serverURI : String
    private lateinit var clientId : String
    private lateinit var username : String
    private lateinit var password : String

    //
    private lateinit var mqttServer: MQTTConnection

    private var _binding: FragmentDirectControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext,
                "Network connection required !!",
                Toast.LENGTH_LONG
            ).show()
        }
        mqttServer = MQTTConnection()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_direct_control, container, false)

        _binding = FragmentDirectControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // connect the buttons
        binding.openbutton.setOnClickListener { openDoor() }

        binding.closebutton.setOnClickListener { closeDoor() }

    }

    /**
     * onStart() is called each time the fragment comes back into focus
     * the best place to reset the private variables from the preferences
     */
    override fun onStart() {

        val sharedPreferences =
            context?.let { PreferenceManager.getDefaultSharedPreferences(it /* Activity context */) }

        // compile the server URI
        serverURI = if (sharedPreferences?.getBoolean("ssl", false) == true) {
            "ssl://"
        } else {
            "tcp://"
        }
        serverURI = serverURI.plus(sharedPreferences?.getString("uri", "").toString())
        serverURI = serverURI.plus(":")
        serverURI = serverURI.plus(sharedPreferences?.getString("port", "").toString())

        // get the clientId
        clientId = sharedPreferences?.getString("clientId", "").toString()
        // get the user name
        username = sharedPreferences?.getString("user", "").toString()
        // get the user password
        password = sharedPreferences?.getString("password", "").toString()

        super.onStart()
    }

    /**
     * sent the open command to the MQTT server
     */
    private fun openDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "open")) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "opening the door",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        activity?.applicationContext,
                        "command delivery failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mqttServer.disconnect()
            } else {
                Toast.makeText(
                    activity?.applicationContext,
                    "Server connection failed - please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                activity?.applicationContext,
                "No network connection - please try again",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * send the close command to the MQTT server
     */
    private fun closeDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "close")) {
                    Toast.makeText(
                        activity?.applicationContext,
                        "closing the door",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        activity?.applicationContext,
                        "command delivery failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mqttServer.disconnect()
            } else {
                Toast.makeText(
                    activity?.applicationContext,
                    "Server connection failed - please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                activity?.applicationContext,
                "No network connection - please try again",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * check if there is access to the internet
     */
    private fun isConnected(): Boolean {
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
        return result
    }
}
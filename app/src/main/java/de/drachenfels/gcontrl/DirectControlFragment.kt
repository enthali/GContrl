package de.drachenfels.gcontrl

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drachenfels.gcontrl.databinding.FragmentDirectControlBinding


/**
 * a fragment to control the garage door directly though two buttons "open" and "close"
 */
class DirectControlFragment : Fragment() {

    private lateinit var mqttServer: MQTTConnection
    private lateinit var geoService: GeoServices

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
        mqttServer = MQTTConnection(activity)
        geoService = GeoServices(activity)
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
        binding.openbutton.setOnClickListener { manageDoor(1) }
        binding.closebutton.setOnClickListener { manageDoor(0) }
    }

    /**
     * onStart() is called each time the fragment comes back into focus
     * the best place to reset the private variables from the preferences
     */
    override fun onStart() {
        super.onStart()
        getPreferences()
    }

    override fun onResume() {
        super.onResume()
        getPreferences()
    }

    private fun getPreferences() {
        val sharedPreferences =
            context?.let { PreferenceManager.getDefaultSharedPreferences(it /* Activity context */) }

        if (sharedPreferences?.getBoolean("geo_enable_location_features", false) == true) {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 1.0f
        } else {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 0.0f
        }
    }

    private fun manageDoor(cmd: Int) {
        val cmdString = when (cmd) {
            0 -> "close"
            1 -> "open"
            else -> "status"
        }
        if (isConnected()) {

            if (mqttServer.sendMessage(cmdString)) {
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
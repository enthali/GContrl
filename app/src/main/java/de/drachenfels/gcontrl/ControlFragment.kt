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
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationServices
import de.drachenfels.gcontrl.databinding.FragmentDirectControlBinding


/**
 * a fragment to control the garage door directly though two buttons "open" and "close"
 */
class ControlFragment : Fragment() {

    private var _binding: FragmentDirectControlBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GControlViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.activity = activity

        viewModel.mFusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        viewModel.initViewModel()
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
        // bind the buttons
        binding.openbutton.setOnClickListener { manageDoor(1) }
        binding.closebutton.setOnClickListener { manageDoor(0) }

        viewModel.distanceToHome.observe(viewLifecycleOwner
        ) { newDistance ->
            binding.distanceText.text = newDistance.toString()
        }
    }

    /**
     * onStart() is called each time the fragment comes back into focus
     * the best place to reset the private variables from the preferences
     */
    override fun onStart() {
        super.onStart()
        viewLocationServiceSection()
        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext,
                "Network connection required !!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewLocationServiceSection()
        //  Check if Internet connection is available
        //  to remind the user that we'll need an internet connection
        if (!isConnected()) {
            Toast.makeText(
                activity?.applicationContext,
                "Network connection required !!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * viewLocationServiceSelection hides the location related information on the screen
     * if location services are disabled in the preferences
     */
    private fun viewLocationServiceSection() {
        val sharedPreferences =
            context?.let { PreferenceManager.getDefaultSharedPreferences(it /* Activity context */) }

        // adopt the layout if geo-services are enabled or not
        if (sharedPreferences?.getBoolean("geo_enable_location_features", false) == true) {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 1.0f
        } else {
            (binding.controlTableLayout.layoutParams as LinearLayout.LayoutParams).weight = 0.0f
        }    }


    private fun manageDoor(cmd: Int) {
        val cmdString = when (cmd) {
            0 -> "close"
            1 -> "open"
            else -> "status"
        }
        if (isConnected()) {

            if (viewModel.mqttServer.sendMessage(cmdString)) {
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
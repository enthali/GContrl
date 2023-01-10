package de.drachenfels.gcontrl

import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import de.drachenfels.gcontrl.databinding.FragmentDirectControlBinding


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
// private const val ARG_PARAM1 = "param1"

/**
 * A simple [Fragment] subclass.
 * Use the [DirectControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DirectControlFragment : Fragment() {
    // TODO: Rename and change types of parameters
    // private var param1: String? = null

    // TODO: make the variables configurable though the setup
    private val serverURI = "ssl://mqtt.drachen-fels.de:8883"
    private val clientId = "AndroidTraveler"
    private val username = "traveler"
    private val password = "traveler"

    private lateinit var mqttServer : MQTTConnection

    private var _binding: FragmentDirectControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        arguments?.let {
//            param1 = it.getString(ARG_PARAM1)
//        }
        /**
         *  Check if Internet connection is available
         *  to remind the user that we'll need an internet connection
         *  */
        if (!isConnected()) {
            Toast.makeText(activity?.applicationContext, "Network connection required !!", Toast.LENGTH_LONG).show()
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

        /**
         *  connect the buttons
         *  */
        binding.openbutton.setOnClickListener { openDoor() }

        binding.closebutton.setOnClickListener { closeDoor() }

    }

    private fun openDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "open")) {
                    Toast.makeText(activity?.applicationContext, "opening the door", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(activity?.applicationContext, "command delivery failed", Toast.LENGTH_LONG).show()
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

    private fun closeDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "close")) {
                    Toast.makeText(activity?.applicationContext, "closing the door", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(activity?.applicationContext, "command delivery failed", Toast.LENGTH_LONG).show()
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

    // check the if we have an internet connection
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

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * // @param param1 Parameter 1.
         * @return A new instance of fragment DirectControlFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(/*param1: String*/ ) =
            DirectControlFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                }
           }
    }
}
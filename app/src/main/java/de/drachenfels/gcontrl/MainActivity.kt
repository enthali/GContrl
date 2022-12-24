package de.drachenfels.gcontrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    // TODO: make the variables configurable though the setup
    private val serverURI = "ssl://mqtt.drachen-fels.de:8883"
    private val clientId = "AndroidTraveler"
    private val username = "traveler"
    private val password = "traveler"

    private val mqttServer = MQTTConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         *  Check if Internet connection is available
         *  to remind the user that we'll need an internet connection
         *  */
        if (!isConnected()) {
            Toast.makeText(applicationContext, "Network connection required !!", Toast.LENGTH_LONG).show()
        }

        /**
         *  connect the buttons
         *  */
        val openButton: Button = findViewById(R.id.openbutton)
        openButton.setOnClickListener { openDoor() }

        val closeButton: Button = findViewById(R.id.closebutton)
        closeButton.setOnClickListener { closeDoor() }

    }

    private fun openDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "open")) {
                    Toast.makeText(applicationContext, "opening the door", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(applicationContext, "command delivery failed", Toast.LENGTH_LONG).show()
                }
                mqttServer.disconnect()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Server connection failed - please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "No network connection - please try again",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun closeDoor() {
        if (isConnected()) {
            if (mqttServer.connect(serverURI, clientId, username, password)) {
                if (mqttServer.sendMessage("garage", "close")) {
                    Toast.makeText(applicationContext, "closing the door", Toast.LENGTH_LONG).show()
                }else{
                    Toast.makeText(applicationContext, "command delivery failed", Toast.LENGTH_LONG).show()
                }
                mqttServer.disconnect()
            } else {
                Toast.makeText(
                    applicationContext,
                    "Server connection failed - please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "No network connection - please try again",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // check the if we have an internet connection
    private fun isConnected(): Boolean {
        var result = false
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
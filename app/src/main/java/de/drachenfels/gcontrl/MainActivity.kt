package de.drachenfels.gcontrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    // TODO: make the variables configurable though the setup
    private val serverURI = "tcp://mqtt.drachen-fels.de:1883"
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
        isConnected()

        /**
         *  connect the buttons
         *  */
        val openButton: Button = findViewById(R.id.openbutton)
        openButton.setOnClickListener { openDoor() }

        val closeButton: Button = findViewById(R.id.closebutton)
        closeButton.setOnClickListener { closeDoor() }

        mqttServer.connect(serverURI, clientId, username, password)
    }

    private fun openDoor() {
        if (isConnected()) {
            Toast.makeText(applicationContext, "open the door", Toast.LENGTH_LONG).show()
            mqttServer.sendMessage("garage", "open")
        }
    }

    private fun closeDoor() {
        if (isConnected()) {
            Toast.makeText(applicationContext, "close the door", Toast.LENGTH_LONG).show()
            mqttServer.sendMessage("garage", "close")
        }
    }

    // check the if we have an internet connection

    // TODO:move to connection class possibly rename
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
        if (!result) {
            Log.d(this.javaClass.name, "Internet connection NOT available")
            Toast.makeText(
                applicationContext,
                "Internet connection NOT available",
                Toast.LENGTH_LONG
            ).show()
        }
        return result
    }
}
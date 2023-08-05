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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import de.drachenfels.gcontrl.databinding.ActivityMainBinding
import de.drachenfels.gcontrl.services.LocationService

/**
 * This is the main activity of the app.
 *
 * It starts the location service and handles navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var locationServiceIntent: Intent

    /**
     * This is the app bar configuration.
     */
    private lateinit var appBarConfiguration: AppBarConfiguration

    /**
     * This is the navigation controller.
     */
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Bind the activity main
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Activate the action bar
        setSupportActionBar(binding.toolbar)

        // Get the nav host running
        val navHostController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Set the variables for navController and appBarConfiguration
        navController = navHostController.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)

        // Get the appBar up and running
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Check if the app has location permissions
        var locationPermission =
            ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")

        // If the app doesn't have location permissions, prompt the user to grant them
        if (locationPermission != PackageManager.PERMISSION_GRANTED) {
            // This line of code prompts the user to grant the location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.ACCESS_FINE_LOCATION"),
                1
            )

            // This line of code re-checks the location permissions after the user has been prompted
            locationPermission =
                ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION")

            // If the user has granted the location permissions, restart the app
            if (locationPermission == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            }
        }

        // Create the location service object
        locationServiceIntent = Intent(this, LocationService::class.java)

        // Start the service - it should always run
        startService(locationServiceIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_preferences -> {
                // Don't go to settings if the settingsFragment is already in focus
                if (navController.currentDestination?.id == R.id.settingsFragment) {
                    // nothing to do here
                    return true
                } else {
                    // lets to to settings
                    navController.navigate(R.id.settingsFragment)
                    return true
                }
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        // Stop the location service
        stopService(locationServiceIntent)
        super.onDestroy()
    }
}
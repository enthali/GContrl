package de.drachenfels.gcontrl

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import de.drachenfels.gcontrl.databinding.ActivityMainBinding
import de.drachenfels.gcontrl.services.LocationService

class MainActivity : AppCompatActivity() {

    var mLocationService: LocationService = LocationService()
    lateinit var mServiceIntent: Intent
    lateinit var mActivity: Activity

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    //    private late init var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // bind the activity main
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // activate the action bar
        setSupportActionBar(binding.toolbar)

        // get the nav host running
        val navHostController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // set the variables for navController and appBarConfiguration
        navController = navHostController.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)

        // get the appBar up and running
        setupActionBarWithNavController(navController, appBarConfiguration)

        // get the activity
        mActivity = this@MainActivity

        // create the location service object
        mLocationService = LocationService()

        // configure the start intent
        mServiceIntent = Intent(this,mLocationService.javaClass)

        // start the service  - it should always run
        startService(mServiceIntent)
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
                // Dont go to settings if the settingsFragment is already in focus
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
        if (::mServiceIntent.isInitialized) {
            stopService(mServiceIntent)
        }
        super.onDestroy()
    }
}
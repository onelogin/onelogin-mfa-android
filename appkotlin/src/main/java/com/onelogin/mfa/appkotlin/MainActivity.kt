package com.onelogin.mfa.appkotlin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.onelogin.mfa.appkotlin.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpNavigation()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.navigation_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun setUpNavigation() {
        val navController = findNavController(R.id.navigation_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf (
                R.id.navigation_register,
                R.id.navigation_home,
                R.id.navigation_factors
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val navHost = supportFragmentManager.findFragmentById(R.id.navigation_host_fragment)
        navHost?.let { navFragment ->
            navFragment.childFragmentManager.primaryNavigationFragment?.onRequestPermissionsResult(
                    requestCode, permissions, grantResults
            )
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}

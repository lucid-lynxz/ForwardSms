package org.lynxz.forwardsms.ui.activity

import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.databinding.ActivityMain2Binding
import org.lynxz.forwardsms.ui.BaseBindingActivity

class Main2Activity : BaseBindingActivity<ActivityMain2Binding>() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun getLayoutRes() = R.layout.activity_main2

    override fun afterViewCreated() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_sms, R.id.nav_forward_setting, R.id.nav_other_setting),
            dataBinding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        dataBinding.navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main2, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
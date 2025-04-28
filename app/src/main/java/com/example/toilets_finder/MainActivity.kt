package com.example.toilets_finder

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.toilets_finder.fragments.HomeFragment
import com.example.toilets_finder.fragments.LoadFragment
import com.example.toilets_finder.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val homeFragment = HomeFragment()
        val settingsFragment = SettingsFragment()
        val loadFragment = LoadFragment()

        userId = getId()
        if (userId == null) makeCurrentFragment(settingsFragment) else makeCurrentFragment(loadFragment)
        Supabase.init()

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                //R.id.ic_home -> makeCurrentFragment(homeFragment)
                R.id.ic_map -> makeCurrentFragment(loadFragment)
                R.id.ic_settings -> makeCurrentFragment(settingsFragment)
            }
            true
        }
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fl_wrapper, fragment)
            commit()
        }


    private fun getId():String? {
        val sharedPref = this.getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

}
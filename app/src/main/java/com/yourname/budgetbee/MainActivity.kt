package com.yourname.budgetbee


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourname.budgetbee.ui.SummaryFragment
import com.yourname.budgetbee.ui.TransactionsFragment
import com.yourname.budgetbee.ui.ChartFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load default fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SummaryFragment())
            .commit()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener {
            val selectedFragment: Fragment = when (it.itemId) {
                R.id.nav_summary -> SummaryFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_chart -> ChartFragment()
                else -> SummaryFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, selectedFragment)
                .commit()

            true
        }
    }
}

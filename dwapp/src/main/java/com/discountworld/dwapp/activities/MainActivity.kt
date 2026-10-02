package com.discountworld.dwapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ActivityMainBinding
import com.discountworld.dwapp.fragments.LoginFragment
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.discountworld.dwapp.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sessionManager = SessionManager(this)
        hideStatusBar()

        handleBackgroundAuth(intent)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        viewModel.isBottomNavVisible.observe(this) { isVisible ->
            binding.bottomNavigation.visibility = if (isVisible) View.VISIBLE else View.GONE
        }

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val hideBottomNav = arguments?.getBoolean("hideBottomNav", false) ?: false
            viewModel.updateBottomNavVisibility(destination.id, hideBottomNav)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleBackgroundAuth(intent)
    }

    private fun handleBackgroundAuth(launchIntent: Intent?) {
        val intentUniqueId = launchIntent?.getStringExtra("unique_id")
        val intentTier = launchIntent?.getStringExtra("customer_tier")
        val intentApiKey = launchIntent?.getStringExtra("api_key") ?: launchIntent?.getStringExtra("apiKey")

        intentApiKey?.let {
            if (it.isNotBlank()) {
                RedemptionStubClient.setApiKey(it)
            }
        }

        val uniqueIdToUse = intentUniqueId ?: LoginFragment.UNIQUE_ID
        val tierToUse = intentTier ?: LoginFragment.CUSTOMER_TIER

        if (uniqueIdToUse.isEmpty() || tierToUse.isEmpty()) {
            finish()
            return
        }

        val currentSavedUniqueId = sessionManager.getUniqueId()
        val currentSavedTier = sessionManager.getCustomerTier()

        val needsReAuth = !currentSavedUniqueId.equals(uniqueIdToUse, ignoreCase = true) ||
                !currentSavedTier.equals(tierToUse, ignoreCase = true) ||
                !sessionManager.isLoggedIn()

        if (needsReAuth) {
            sessionManager.clearSession()
            sessionManager.saveUniqueId(uniqueIdToUse)
            sessionManager.saveCustomerTier(tierToUse)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val repo = RedemptionRepository()
                    val authResp = repo.authenticateByUniqueId(uniqueIdToUse, tierToUse)
                    if (authResp != null) {
                        sessionManager.saveAuthToken(authResp.accessToken)
                        sessionManager.saveUniqueId(uniqueIdToUse)
                        val tier = authResp.customer.customerTier.ifEmpty { tierToUse }
                        sessionManager.saveCustomerTier(tier)
                        if (authResp.customer.phoneNumber.isNotEmpty()) {
                            sessionManager.savePhone(authResp.customer.phoneNumber)
                        }
                        RedemptionStubClient.setToken(authResp.accessToken)
                    }
                } catch (_: Exception) { }
            }
        } else {
            sessionManager.getAuthToken()?.let { token ->
                RedemptionStubClient.setToken(token)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }

    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

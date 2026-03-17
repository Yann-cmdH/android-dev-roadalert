package com.roadalert.cameroun.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.databinding.ActivitySplashBinding
import com.roadalert.cameroun.ui.home.HomeActivity
import com.roadalert.cameroun.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val viewModel: SplashViewModel by viewModels {
        SplashViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.checkProfile()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isProfileComplete.collect { isComplete ->
                isComplete?.let {
                    if (it) navigateToHome()
                    else navigateToOnboarding()
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun navigateToOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }
}
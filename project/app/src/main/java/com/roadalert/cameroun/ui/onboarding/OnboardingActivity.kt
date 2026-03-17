package com.roadalert.cameroun.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.databinding.ActivityOnboardingBinding
import com.roadalert.cameroun.ui.profile.ProfileSetupActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onLocationGranted()
        else showPermissionDenied()
    }

    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onSmsGranted()
        else showPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
        checkExistingPermissions()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                updateStepIndicator(step)
                updateContent(step)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener {
            when (viewModel.currentStep.value) {
                1 -> requestLocationPermission.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                2 -> {
                    requestSmsPermission.launch(Manifest.permission.SEND_SMS)
                    requestBatteryExemption()
                }
                3 -> navigateToProfile()
            }
        }
    }

    private fun updateStepIndicator(step: Int) {
        val activeColor = getColor(com.roadalert.cameroun.R.color.red_primary)
        val inactiveColor = getColor(com.roadalert.cameroun.R.color.border)
        binding.step1.setBackgroundColor(if (step >= 1) activeColor else inactiveColor)
        binding.step2.setBackgroundColor(if (step >= 2) activeColor else inactiveColor)
        binding.step3.setBackgroundColor(if (step >= 3) activeColor else inactiveColor)
    }

    private fun updateContent(step: Int) {
        when (step) {
            1 -> {
                binding.tvPermName.setText(com.roadalert.cameroun.R.string.onboarding_gps_name)
                binding.tvPermWhy.setText(com.roadalert.cameroun.R.string.onboarding_gps_why)
                binding.tvPermIcon.text = "📍"
                binding.btnAction.setText(com.roadalert.cameroun.R.string.onboarding_btn_allow)
            }
            2 -> {
                binding.tvPermName.setText(com.roadalert.cameroun.R.string.onboarding_sms_name)
                binding.tvPermWhy.setText(com.roadalert.cameroun.R.string.onboarding_sms_why)
                binding.tvPermIcon.text = "✉"
                binding.btnAction.setText(com.roadalert.cameroun.R.string.onboarding_btn_allow)
            }
            3 -> {
                binding.tvPermName.setText(com.roadalert.cameroun.R.string.onboarding_ready_title)
                binding.tvPermWhy.setText(com.roadalert.cameroun.R.string.onboarding_ready_msg)
                binding.tvPermIcon.text = "✓"
                binding.btnAction.setText(com.roadalert.cameroun.R.string.onboarding_btn_next)
            }
        }
    }

    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
        viewModel.onBatteryExempted()
    }

    private fun checkExistingPermissions() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) viewModel.onLocationGranted()
    }

    private fun showPermissionDenied() {
        android.widget.Toast.makeText(
            this,
            "Cette permission est nécessaire pour le fonctionnement de l'app",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private fun navigateToProfile() {
        startActivity(Intent(this, ProfileSetupActivity::class.java))
        finish()
    }
}
package com.roadalert.cameroun.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.R
import com.roadalert.cameroun.databinding.ActivityOnboardingBinding
import com.roadalert.cameroun.ui.profile.ProfileSetupActivity
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()

    // ── Permission launchers ──────────────────────────────────

    private val requestLocationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) viewModel.onLocationGranted()
            else showPermissionDenied()
        }

    private val requestSmsPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) viewModel.onSmsGranted()
            else showPermissionDenied()
        }

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackNavigation()
        observeViewModel()
        setupClickListeners()
        checkExistingPermissions()
    }

    // ── Bouton retour — pas de boucle infinie ─────────────────

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Onboarding = étape obligatoire
                    // L'utilisateur ne peut pas revenir
                    // en arrière depuis l'onboarding
                    // On ignore le bouton retour
                }
            }
        )
    }

    // ── Observe ViewModel ─────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                updateStepIndicator(step)
                updateContent(step)
            }
        }
    }

    // ── Click listeners ───────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener {
            when (viewModel.currentStep.value) {
                1 -> requestLocationPermission.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                2 -> {
                    requestSmsPermission.launch(
                        Manifest.permission.SEND_SMS
                    )
                    requestBatteryExemption()
                }
                3 -> navigateToProfile()
            }
        }
    }

    // ── Progress indicator ────────────────────────────────────

    private fun updateStepIndicator(step: Int) {
        val activeColor = getColor(R.color.red_primary)
        val inactiveColor = getColor(R.color.border)
        binding.step1.setBackgroundColor(
            if (step >= 1) activeColor else inactiveColor
        )
        binding.step2.setBackgroundColor(
            if (step >= 2) activeColor else inactiveColor
        )
        binding.step3.setBackgroundColor(
            if (step >= 3) activeColor else inactiveColor
        )
    }

    // ── Contenu par étape — emojis corrects ───────────────────

    private fun updateContent(step: Int) {
        when (step) {
            1 -> {
                binding.tvPermName.setText(
                    R.string.onboarding_gps_name
                )
                binding.tvPermWhy.setText(
                    R.string.onboarding_gps_why
                )
                binding.tvPermIcon.text = "📍"
                binding.btnAction.setText(
                    R.string.onboarding_btn_allow
                )
            }
            2 -> {
                binding.tvPermName.setText(
                    R.string.onboarding_sms_name
                )
                binding.tvPermWhy.setText(
                    R.string.onboarding_sms_why
                )
                binding.tvPermIcon.text = "✉"
                binding.btnAction.setText(
                    R.string.onboarding_btn_allow
                )
            }
            3 -> {
                binding.tvPermName.setText(
                    R.string.onboarding_ready_title
                )
                binding.tvPermWhy.setText(
                    R.string.onboarding_ready_msg
                )
                binding.tvPermIcon.text = "✓"
                binding.btnAction.setText(
                    R.string.onboarding_btn_next
                )
            }
        }
    }

    // ── Battery exemption ─────────────────────────────────────

    private fun requestBatteryExemption() {
        try {
            val pm = getSystemService(
                POWER_SERVICE
            ) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(
                    Settings
                        .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Ignoré — certains appareils ne supportent pas
        }
        viewModel.onBatteryExempted()
    }

    // ── Vérification permissions existantes ───────────────────

    private fun checkExistingPermissions() {
        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        when {
            hasLocation && hasSms -> {
                // Toutes les permissions déjà accordées
                // Passer directement à l'étape 3
                viewModel.onLocationGranted()
                viewModel.onSmsGranted()
            }
            hasLocation -> {
                // GPS déjà accordé — passer à SMS
                viewModel.onLocationGranted()
            }
            else -> {
                // Commencer depuis le début
            }
        }
    }

    // ── Permission refusée ────────────────────────────────────

    private fun showPermissionDenied() {
        Toast.makeText(
            this,
            getString(R.string.onboarding_permission_denied),
            Toast.LENGTH_LONG
        ).show()
    }

    // ── Navigation ────────────────────────────────────────────

    private fun navigateToProfile() {
        startActivity(
            Intent(this, ProfileSetupActivity::class.java)
        )
        finish()
    }
}
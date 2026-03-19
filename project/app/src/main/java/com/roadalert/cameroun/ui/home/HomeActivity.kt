package com.roadalert.cameroun.ui.home

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.BuildConfig
import com.roadalert.cameroun.R
import com.roadalert.cameroun.databinding.ActivityHomeBinding
import com.roadalert.cameroun.detection.AccidentDetectionService
import com.roadalert.cameroun.ui.countdown.CountdownActivity
import com.roadalert.cameroun.ui.history.HistoryActivity
import com.roadalert.cameroun.ui.settings.SettingsActivity
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(applicationContext)
    }

    // ── Receivers ─────────────────────────────────────────────

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==
                ServiceActions.ACTION_SERVICE_STATE_CHANGED) {
                val isRunning = intent.getBooleanExtra(
                    ServiceActions.EXTRA_SERVICE_RUNNING, false
                )
                viewModel.updateServiceState(isRunning)
            }
        }
    }

    private val accidentDetectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==
                ServiceActions.ACTION_ACCIDENT_DETECTED) {
                navigateToCountdown()
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()

        if (BuildConfig.DEBUG) {
            setupDebugButton()
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceivers()
        checkPermissionsAndUpdateUI()
        viewModel.loadUser()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceivers()
    }

    // ── Vérification permissions ──────────────────────────────

    private fun checkPermissionsAndUpdateUI() {
        val hasGps = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        when {
            !hasSms -> showSmsBanner()
            !hasGps -> showGpsBanner()
            else -> {
                showNormalBanner()
                checkAndStartService()
            }
        }
    }

    // ── Bannières permissions ─────────────────────────────────

    private fun showNormalBanner() {
        binding.cardService.visibility = View.VISIBLE
        binding.bannerOrange.visibility = View.GONE
        binding.bannerRed.visibility = View.GONE
        binding.btnSOS.isEnabled = true
        binding.btnSOS.alpha = 1.0f
    }

    private fun showGpsBanner() {
        binding.cardService.visibility = View.GONE
        binding.bannerOrange.visibility = View.VISIBLE
        binding.bannerRed.visibility = View.GONE
        binding.btnSOS.isEnabled = true
        binding.btnSOS.alpha = 1.0f
        checkAndStartService()
    }

    private fun showSmsBanner() {
        binding.cardService.visibility = View.GONE
        binding.bannerOrange.visibility = View.GONE
        binding.bannerRed.visibility = View.VISIBLE
        binding.btnSOS.isEnabled = false
        binding.btnSOS.alpha = 0.4f
        stopDetectionService()
    }

    // ── Service management ────────────────────────────────────

    private fun checkAndStartService() {
        if (!isServiceRunning()) {
            val serviceIntent = Intent(
                this,
                AccidentDetectionService::class.java
            ).apply {
                action = ServiceActions.ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            viewModel.updateServiceState(true)
        }
    }

    private fun stopDetectionService() {
        val serviceIntent = Intent(
            this,
            AccidentDetectionService::class.java
        ).apply {
            action = ServiceActions.ACTION_STOP_SERVICE
        }
        startService(serviceIntent)
        viewModel.updateServiceState(false)
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any {
                it.service.className ==
                        AccidentDetectionService::class.java.name
            }
    }

    // ── Receivers registration ────────────────────────────────

    private fun registerReceivers() {
        val serviceFilter = IntentFilter(
            ServiceActions.ACTION_SERVICE_STATE_CHANGED
        )
        val accidentFilter = IntentFilter(
            ServiceActions.ACTION_ACCIDENT_DETECTED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                serviceStateReceiver, serviceFilter,
                RECEIVER_NOT_EXPORTED
            )
            registerReceiver(
                accidentDetectedReceiver, accidentFilter,
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(serviceStateReceiver, serviceFilter)
            registerReceiver(accidentDetectedReceiver, accidentFilter)
        }
    }

    private fun unregisterReceivers() {
        try {
            unregisterReceiver(serviceStateReceiver)
            unregisterReceiver(accidentDetectedReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignoré
        }
    }

    // ── Observe ViewModel ─────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                user?.let {
                    binding.tvGreeting.text =
                        getString(R.string.home_greeting_default) +
                                ", ${it.fullName}"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isServiceRunning.collect { running ->
                updateServiceBanner(running)
            }
        }

        lifecycleScope.launch {
            viewModel.showSosDialog.collect { show ->
                if (show) showSosConfirmDialog()
            }
        }
    }

    // ── Click listeners ───────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnSOS.setOnClickListener {
            viewModel.onSosButtonPressed()
        }
        binding.navHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.navSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnActivateGps.setOnClickListener {
            openAppSettings()
        }
        binding.btnActivateSms.setOnClickListener {
            openAppSettings()
        }
    }

    // ── Ouvrir paramètres Android ─────────────────────────────

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    // ── Debug button ──────────────────────────────────────────

    private fun setupDebugButton() {
        binding.btnDebugTest.visibility = View.VISIBLE
        binding.btnDebugTest.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("DEBUG — Simuler accident")
                .setMessage(
                    "Simuler une détection d'accident ?"
                )
                .setPositiveButton("Simuler") { _, _ ->
                    navigateToCountdown()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    // ── Service banner ────────────────────────────────────────

    private fun updateServiceBanner(isRunning: Boolean) {
        val hasSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasGps = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Ne pas écraser les bannières permissions
        if (!hasSms || !hasGps) return

        if (isRunning) {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.green_surface)
            )
            binding.tvServiceStatus.setText(
                R.string.home_service_active
            )
            binding.tvServiceSub.setText(
                R.string.home_service_running
            )
            binding.viewPulse.setBackgroundResource(
                R.drawable.shape_circle_green
            )
        } else {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.red_surface)
            )
            binding.tvServiceStatus.setText(
                R.string.home_service_inactive
            )
            binding.tvServiceSub.setText(
                R.string.home_service_stopped
            )
            binding.viewPulse.setBackgroundResource(
                R.drawable.shape_circle_red
            )
        }
    }

    // ── SOS Dialog ────────────────────────────────────────────

    private fun showSosConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_sos_title)
            .setMessage(R.string.dialog_sos_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                viewModel.onSosConfirmed()
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                viewModel.onSosCancelled()
            }
            .setCancelable(false)
            .show()
    }

    // ── Navigation ────────────────────────────────────────────

    private fun navigateToCountdown() {
        val intent = Intent(
            this, CountdownActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
}
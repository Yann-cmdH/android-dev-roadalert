package com.roadalert.cameroun.ui.home

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
            if (intent.action == ServiceActions.ACTION_SERVICE_STATE_CHANGED) {
                val isRunning = intent.getBooleanExtra(
                    ServiceActions.EXTRA_SERVICE_RUNNING, false
                )
                viewModel.updateServiceState(isRunning)
            }
        }
    }

    private val accidentDetectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ServiceActions.ACTION_ACCIDENT_DETECTED) {
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
        checkAndStartService()
        viewModel.loadUser()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceivers()
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

    // ── Service management ────────────────────────────────────

    private fun checkAndStartService() {
        if (!isServiceRunning()) {
            val serviceIntent = Intent(
                this, AccidentDetectionService::class.java
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
    }

    // ── Debug button ──────────────────────────────────────────

    private fun setupDebugButton() {
        binding.btnDebugTest.visibility = View.VISIBLE
        binding.btnDebugTest.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("DEBUG — Simuler accident")
                .setMessage("Simuler une détection d'accident pour tester CountdownActivity ?")
                .setPositiveButton("Simuler") { _, _ ->
                    navigateToCountdown()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    // ── Service banner ────────────────────────────────────────

    private fun updateServiceBanner(isRunning: Boolean) {
        if (isRunning) {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.green_surface)
            )
            binding.tvServiceStatus.setText(R.string.home_service_active)
            binding.tvServiceSub.setText(R.string.home_service_running)
            binding.viewPulse.setBackgroundResource(
                R.drawable.shape_circle_green
            )
        } else {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.red_surface)
            )
            binding.tvServiceStatus.setText(R.string.home_service_inactive)
            binding.tvServiceSub.setText(R.string.home_service_stopped)
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
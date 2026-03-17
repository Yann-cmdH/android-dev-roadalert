package com.roadalert.cameroun.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.R
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.databinding.ActivityHomeBinding
import com.roadalert.cameroun.ui.history.HistoryActivity
import com.roadalert.cameroun.ui.settings.SettingsActivity
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            UserProfileRepository(
                AppDatabase.getInstance(this).userDAO(),
                AppDatabase.getInstance(this).emergencyContactDAO()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupClickListeners()
        viewModel.loadUser()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.user.collect { user ->
                user?.let {
                    binding.tvGreeting.text = getString(
                        R.string.home_greeting_default
                    ) + ", ${it.getDisplayName()}"
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

    private fun updateServiceBanner(isRunning: Boolean) {
        if (isRunning) {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.green_surface)
            )
            binding.tvServiceStatus.setText(R.string.home_service_active)
            binding.tvServiceSub.setText(R.string.home_service_running)
            binding.viewPulse.setBackgroundResource(R.drawable.shape_circle_green)
        } else {
            binding.cardService.setCardBackgroundColor(
                getColor(R.color.red_surface)
            )
            binding.tvServiceStatus.setText(R.string.home_service_inactive)
            binding.tvServiceSub.setText(R.string.home_service_stopped)
            binding.viewPulse.setBackgroundResource(R.drawable.shape_circle_red)
        }
    }

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
}
package com.roadalert.cameroun.ui.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.roadalert.cameroun.BuildConfig
import com.roadalert.cameroun.R
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.databinding.ActivitySettingsBinding
import com.roadalert.cameroun.ui.profile.ProfileSetupActivity
import com.roadalert.cameroun.ui.splash.SplashActivity
import com.roadalert.cameroun.util.AppSettings
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(applicationContext)
    }

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

    private var resetDialog: AlertDialog? = null
    private var easterEggCount: Int = 0

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ServiceActions.ACTION_SERVICE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(serviceStateReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignoré
        }
    }

    override fun onDestroy() {
        resetDialog?.dismiss()
        super.onDestroy()
    }

    // ── Observers ─────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateHeader(state)
                updateContacts(state.contacts)
                updateMedical(state)
                updateDetection(state)
                updateNotifications(state)
                handleResetDialog(state)
                handleSnackbar(state)
            }
        }

        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                when (event) {
                    is NavigationEvent.GoToSplash -> {
                        val intent = Intent(
                            this@SettingsActivity,
                            SplashActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                    is NavigationEvent.GoToProfileSetup -> {
                        val intent = Intent(
                            this@SettingsActivity,
                            ProfileSetupActivity::class.java
                        ).apply {
                            putExtra("CALLER", "SETTINGS")
                            putExtra("START_STEP", event.startStep)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    // ── Update functions ──────────────────────────────────────

    private fun updateHeader(state: SettingsUiState) {
        val user = state.user
        if (user == null) {
            binding.tvAvatar.text = "?"
            binding.tvSettingsName.text = getString(R.string.settings_header_no_profile)
            binding.tvSettingsPhone.text = "\u2014"
            binding.tvSettingsBlood.visibility = View.GONE
            binding.tvSettingsEdit.text = getString(R.string.settings_header_configure)
        } else {
            val name = user.fullName
            binding.tvAvatar.text = name.take(2).uppercase()
            binding.tvSettingsName.text = name
            binding.tvSettingsPhone.text = user.phoneNumber
            binding.tvSettingsEdit.text = getString(R.string.settings_header_modify)
            if (!user.bloodType.isNullOrBlank()) {
                binding.tvSettingsBlood.visibility = View.VISIBLE
                binding.tvSettingsBlood.text = user.bloodType
            } else {
                binding.tvSettingsBlood.visibility = View.GONE
            }
        }

        // Indicateur service
        when {
            user == null -> {
                binding.viewServiceDot.setBackgroundResource(R.drawable.shape_circle_red)
                binding.tvServiceStatus.text =
                    getString(R.string.settings_service_profile_required)
            }
            state.isServiceRunning -> {
                binding.viewServiceDot.setBackgroundResource(R.drawable.shape_circle_green)
                binding.tvServiceStatus.text =
                    getString(R.string.settings_service_active)
            }
            else -> {
                binding.viewServiceDot.setBackgroundResource(R.drawable.shape_circle_red)
                binding.tvServiceStatus.text =
                    getString(R.string.settings_service_inactive)
            }
        }
    }

    private fun updateContacts(contacts: List<EmergencyContact>) {
        binding.contactsContainer.removeAllViews()

        if (contacts.isEmpty()) {
            binding.bannerNoContacts.visibility = View.VISIBLE
            binding.tvNoContacts.visibility = View.VISIBLE
            binding.btnModifyContacts.text = getString(R.string.settings_contacts_add_first)
            return
        }

        binding.bannerNoContacts.visibility = View.GONE
        binding.tvNoContacts.visibility = View.GONE
        binding.btnModifyContacts.text = getString(R.string.settings_contacts_modify)

        val dp = resources.displayMetrics.density

        contacts.forEachIndexed { index, contact ->
            // Ligne contact
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = (56 * dp).toInt()
                setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Cercle numéroté
            val circleSize = (28 * dp).toInt()
            val circle = TextView(this).apply {
                text = "${index + 1}"
                textSize = 12f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                background = if (index == 0) {
                    androidx.core.content.ContextCompat.getDrawable(
                        this@SettingsActivity,
                        R.drawable.shape_contact_circle_red
                    )
                } else {
                    androidx.core.content.ContextCompat.getDrawable(
                        this@SettingsActivity,
                        R.drawable.shape_contact_circle_gray
                    )
                }
                layoutParams = LinearLayout.LayoutParams(circleSize, circleSize)
            }

            // Bloc texte
            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).also { it.marginStart = (10 * dp).toInt() }
            }

            val nameView = TextView(this).apply {
                text = contact.name
                textSize = 13f
                setTextColor(Color.parseColor("#2C3E50"))
                typeface = Typeface.DEFAULT_BOLD
            }

            val detailView = TextView(this).apply {
                text = "${contact.phoneNumber} · ${contact.relation}"
                textSize = 12f
                setTextColor(Color.parseColor("#95A5A6"))
            }

            infoLayout.addView(nameView)
            infoLayout.addView(detailView)
            row.addView(circle)
            row.addView(infoLayout)
            binding.contactsContainer.addView(row)

            // Divider entre contacts
            if (index < contacts.size - 1) {
                val divider = View(this).apply {
                    setBackgroundColor(Color.parseColor("#F0F0F0"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                }
                binding.contactsContainer.addView(divider)
            }
        }
    }

    private fun updateMedical(state: SettingsUiState) {
        val user = state.user
        val emptyText = getString(R.string.settings_medical_empty)

        // Groupe sanguin
        if (!user?.bloodType.isNullOrBlank()) {
            binding.tvMedicalBlood.text = user!!.bloodType
            binding.tvMedicalBlood.setTextColor(Color.parseColor("#C0392B"))
            binding.tvMedicalBlood.visibility = View.VISIBLE
        } else {
            binding.tvMedicalBlood.text = emptyText
            binding.tvMedicalBlood.setTextColor(Color.parseColor("#BDC3C7"))
        }

        // Allergies
        val allergies = user?.allergies
        if (!allergies.isNullOrBlank()) {
            binding.tvMedicalAllergies.text = allergies
            binding.tvMedicalAllergies.setTextColor(Color.parseColor("#2C3E50"))
            binding.tvMedicalAllergies.setTypeface(null, Typeface.NORMAL)
        } else {
            binding.tvMedicalAllergies.text = emptyText
            binding.tvMedicalAllergies.setTextColor(Color.parseColor("#BDC3C7"))
            binding.tvMedicalAllergies.setTypeface(null, Typeface.ITALIC)
        }

        // Conditions
        val conditions = user?.medicalConditions
        if (!conditions.isNullOrBlank()) {
            binding.tvMedicalConditions.text = conditions
            binding.tvMedicalConditions.setTextColor(Color.parseColor("#2C3E50"))
            binding.tvMedicalConditions.setTypeface(null, Typeface.NORMAL)
        } else {
            binding.tvMedicalConditions.text = emptyText
            binding.tvMedicalConditions.setTextColor(Color.parseColor("#BDC3C7"))
            binding.tvMedicalConditions.setTypeface(null, Typeface.ITALIC)
        }
    }

    private fun updateDetection(state: SettingsUiState) {
        fun updateToggle(btn: TextView, isActive: Boolean) {
            if (isActive) {
                btn.setBackgroundResource(R.drawable.shape_toggle_active)
                btn.setTextColor(Color.WHITE)
                btn.typeface = Typeface.DEFAULT_BOLD
            } else {
                btn.setBackgroundResource(R.drawable.shape_toggle_inactive)
                btn.setTextColor(Color.parseColor("#95A5A6"))
                btn.typeface = Typeface.DEFAULT
            }
        }

        updateToggle(binding.btnSensitivityPrudent, state.sensitivity == AppSettings.SENSITIVITY_LOW)
        updateToggle(binding.btnSensitivityStandard, state.sensitivity == AppSettings.SENSITIVITY_MEDIUM)
        updateToggle(binding.btnSensitivitySportif, state.sensitivity == AppSettings.SENSITIVITY_HIGH)

        binding.tvSensitivityNote.text = when (state.sensitivity) {
            AppSettings.SENSITIVITY_LOW ->
                getString(R.string.settings_sensitivity_note_prudent)
            AppSettings.SENSITIVITY_HIGH ->
                getString(R.string.settings_sensitivity_note_sportif)
            else ->
                getString(R.string.settings_sensitivity_note_standard)
        }

        updateToggle(binding.btnCountdown10, state.countdownSeconds == AppSettings.COUNTDOWN_10)
        updateToggle(binding.btnCountdown15, state.countdownSeconds == AppSettings.COUNTDOWN_15)
        updateToggle(binding.btnCountdown20, state.countdownSeconds == AppSettings.COUNTDOWN_20)

        binding.tvCountdownNote.text = when (state.countdownSeconds) {
            AppSettings.COUNTDOWN_10 ->
                getString(R.string.settings_countdown_note_10)
            AppSettings.COUNTDOWN_20 ->
                getString(R.string.settings_countdown_note_20)
            else ->
                getString(R.string.settings_countdown_note_15)
        }
    }

    private fun updateNotifications(state: SettingsUiState) {
        binding.switchSound.setOnCheckedChangeListener(null)
        binding.switchSound.isChecked = state.soundEnabled
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSoundEnabled(isChecked)
        }

        binding.switchVibration.setOnCheckedChangeListener(null)
        binding.switchVibration.isChecked = state.vibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setVibrationEnabled(isChecked)
        }
    }

    // ── Click listeners ───────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.tvSettingsEdit.setOnClickListener {
            viewModel.navigateToProfileSetup()
        }

        binding.btnBannerAdd.setOnClickListener {
            viewModel.navigateToProfileSetup(2)
        }

        binding.btnModifyContacts.setOnClickListener {
            viewModel.navigateToProfileSetup(2)
        }

        binding.btnSensitivityPrudent.setOnClickListener {
            viewModel.setSensitivity(AppSettings.SENSITIVITY_LOW)
        }
        binding.btnSensitivityStandard.setOnClickListener {
            viewModel.setSensitivity(AppSettings.SENSITIVITY_MEDIUM)
        }
        binding.btnSensitivitySportif.setOnClickListener {
            viewModel.setSensitivity(AppSettings.SENSITIVITY_HIGH)
        }

        binding.btnCountdown10.setOnClickListener {
            viewModel.setCountdown(AppSettings.COUNTDOWN_10)
        }
        binding.btnCountdown15.setOnClickListener {
            viewModel.setCountdown(AppSettings.COUNTDOWN_15)
        }
        binding.btnCountdown20.setOnClickListener {
            viewModel.setCountdown(AppSettings.COUNTDOWN_20)
        }

        binding.btnReset.setOnClickListener {
            viewModel.onResetRequested()
        }

        binding.tvVersion.setOnClickListener {
            easterEggCount++
            if (easterEggCount >= 7 && BuildConfig.DEBUG) {
                Toast.makeText(
                    this,
                    getString(R.string.settings_easter_egg),
                    Toast.LENGTH_SHORT
                ).show()
                easterEggCount = 0
            }
        }
    }

    // ── Reset dialog ──────────────────────────────────────────

    private fun handleResetDialog(state: SettingsUiState) {
        if (!state.showResetDialog) {
            resetDialog?.dismiss()
            resetDialog = null
            return
        }

        if (resetDialog == null) {
            resetDialog = AlertDialog.Builder(this)
                .setTitle(R.string.settings_reset_dialog_title)
                .setMessage(R.string.settings_reset_dialog_message)
                .setNegativeButton(R.string.settings_reset_dialog_cancel) { _, _ ->
                    viewModel.dismissResetDialog()
                }
                .setPositiveButton(R.string.settings_reset_dialog_confirm) { _, _ ->
                    viewModel.confirmReset()
                }
                .setCancelable(false)
                .create()
            resetDialog?.show()
        }

        val positiveBtn = resetDialog?.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveBtn?.isEnabled = state.resetButtonEnabled
        positiveBtn?.alpha = if (state.resetButtonEnabled) 1.0f else 0.35f
    }

    // ── Snackbar ──────────────────────────────────────────────

    private fun handleSnackbar(state: SettingsUiState) {
        val message = state.snackbarMessage ?: return
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.dismissSnackbar()
    }
}

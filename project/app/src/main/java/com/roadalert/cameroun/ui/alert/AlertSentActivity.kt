package com.roadalert.cameroun.ui.alert

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.roadalert.cameroun.R
import com.roadalert.cameroun.data.db.entity.SmsStatus
import com.roadalert.cameroun.databinding.ActivityAlertSentBinding
import com.roadalert.cameroun.ui.home.HomeActivity
import com.roadalert.cameroun.util.LocaleHelper
import com.roadalert.cameroun.util.ServiceActions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertSentActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityAlertSentBinding

    private val viewModel: AlertSentViewModel by viewModels {
        AlertSentViewModelFactory(
            context = applicationContext,
            eventId = intent.getStringExtra(
                ServiceActions.EXTRA_ACCIDENT_EVENT_ID
            ) ?: ""
        )
    }

    // ── Spinner animation ─────────────────────────────────
    private var spinnerAnimation: ObjectAnimator? = null

    // ── Bouton retour — désactivé uniquement État FAILED ──
    private val backCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // Intentionnellement vide
                // État FAILED — pas de retour possible
            }
        }

    // ── Lifecycle ─────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackNavigation()
        setupClickListeners()
        observeViewModel()
        startSpinnerAnimation()
    }

    override fun onDestroy() {
        stopSpinnerAnimation()
        super.onDestroy()
    }

    // ── Back navigation ───────────────────────────────────

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    // ── Spinner ───────────────────────────────────────────

    private fun startSpinnerAnimation() {
        spinnerAnimation = ObjectAnimator.ofFloat(
            binding.spinnerIcon,
            "rotation",
            0f,
            360f
        ).apply {
            duration = 1000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopSpinnerAnimation() {
        spinnerAnimation?.cancel()
        spinnerAnimation = null
    }

    // ── Observer ViewModel ────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.contacts.collect { contacts ->
                updateContactRows(contacts)
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AlertSentUiState.Sending ->
                        showSendingState()
                    is AlertSentUiState.Sent ->
                        showSentState(state.event)
                    is AlertSentUiState.Partial ->
                        showPartialState(state.event)
                    is AlertSentUiState.Failed ->
                        showFailedState(state.event)
                }
            }
        }
    }

    // ── Mise à jour des lignes contact ────────────────────

    private fun updateContactRows(
        contacts: List<AlertSentViewModel.ContactDisplayInfo>
    ) {
        val rows = listOf(
            Triple(
                binding.contactRow1,
                binding.tvContactName1,
                binding.tvContactStatus1
            ),
            Triple(
                binding.contactRow2,
                binding.tvContactName2,
                binding.tvContactStatus2
            ),
            Triple(
                binding.contactRow3,
                binding.tvContactName3,
                binding.tvContactStatus3
            )
        )

        val dots = listOf(
            binding.dot1,
            binding.dot2,
            binding.dot3
        )
        val dividers = listOf(
            binding.divider12,
            binding.divider23
        )

        contacts.forEachIndexed { index, contact ->
            if (index < rows.size) {
                val (row, nameView, statusView) = rows[index]
                row.visibility = View.VISIBLE
                nameView.text = contact.name
                statusView.text =
                    getSmsStatusLabel(contact.smsStatus)
                dots[index].setBackgroundResource(
                    getDotDrawable(contact.smsStatus)
                )
                if (index > 0 && index - 1 < dividers.size) {
                    dividers[index - 1].visibility = View.VISIBLE
                }
            }
        }
    }

    // ── État 1 — SENDING ──────────────────────────────────

    private fun showSendingState() {
        binding.rootLayout.setBackgroundResource(
            R.drawable.bg_alert_sending
        )
        binding.spinnerContainer.visibility = View.VISIBLE
        binding.iconContainer.visibility = View.GONE
        binding.tvAlertTitle.setText(
            R.string.alert_sending_title
        )
        binding.tvAlertDesc.setText(
            R.string.alert_sending_desc
        )
        binding.tvAlertDesc.visibility = View.VISIBLE
        binding.tvAlertTime.text = ""
        binding.gpsCard.visibility = View.GONE
        binding.emergencyButtons.visibility = View.GONE
        binding.tvNotice.setText(
            R.string.alert_notice_sending
        )
        binding.tvNotice.visibility = View.VISIBLE
        backCallback.isEnabled = false
    }

    // ── État 2 — SENT ─────────────────────────────────────

    private fun showSentState(
        event: com.roadalert.cameroun.data.db.entity.AccidentEvent
    ) {
        stopSpinnerAnimation()
        binding.rootLayout.setBackgroundResource(
            R.drawable.bg_alert_sent
        )
        binding.spinnerContainer.visibility = View.GONE
        binding.iconContainer.visibility = View.VISIBLE
        binding.tvStatusIcon.text = "✓"
        binding.tvStatusIcon.setBackgroundResource(
            R.drawable.bg_icon_sent
        )
        binding.tvAlertTitle.setText(R.string.alert_sent_title)
        binding.tvAlertTime.text = formatTimestamp(event.timestamp)
        binding.tvAlertDesc.visibility = View.GONE
        showGpsCard(event)
        binding.emergencyButtons.visibility = View.VISIBLE
        binding.btnReturnHome.setText(
            R.string.alert_btn_home_return
        )
        binding.tvNotice.visibility = View.GONE
        backCallback.isEnabled = false
    }

    // ── État 3 — PARTIAL ──────────────────────────────────

    private fun showPartialState(
        event: com.roadalert.cameroun.data.db.entity.AccidentEvent
    ) {
        stopSpinnerAnimation()
        binding.rootLayout.setBackgroundResource(
            R.drawable.bg_alert_partial
        )
        binding.spinnerContainer.visibility = View.GONE
        binding.iconContainer.visibility = View.VISIBLE
        binding.tvStatusIcon.text = "!"
        binding.tvStatusIcon.setBackgroundResource(
            R.drawable.bg_icon_partial
        )
        binding.tvAlertTitle.setText(
            R.string.alert_partial_title
        )
        binding.tvAlertTime.text = formatTimestamp(event.timestamp)
        binding.tvAlertDesc.setText(
            R.string.alert_partial_desc
        )
        binding.tvAlertDesc.visibility = View.VISIBLE
        showGpsCard(event)
        binding.emergencyButtons.visibility = View.VISIBLE
        binding.btnReturnHome.setText(
            R.string.alert_btn_home_return
        )
        binding.tvNotice.visibility = View.GONE
        backCallback.isEnabled = false
    }

    // ── État 4 — FAILED ───────────────────────────────────

    private fun showFailedState(
        event: com.roadalert.cameroun.data.db.entity.AccidentEvent
    ) {
        stopSpinnerAnimation()
        binding.rootLayout.setBackgroundResource(
            R.drawable.bg_alert_failed
        )
        binding.spinnerContainer.visibility = View.GONE
        binding.iconContainer.visibility = View.VISIBLE
        binding.tvStatusIcon.text = "✕"
        binding.tvStatusIcon.setBackgroundResource(
            R.drawable.bg_icon_failed
        )
        binding.tvAlertTitle.setText(
            R.string.alert_failed_title
        )
        binding.tvAlertTime.text = formatTimestamp(event.timestamp)
        binding.tvAlertDesc.setText(
            R.string.alert_failed_desc
        )
        binding.tvAlertDesc.visibility = View.VISIBLE
        showGpsCard(event)
        binding.emergencyButtons.visibility = View.VISIBLE
        binding.btnReturnHome.setText(
            R.string.alert_btn_home_failed
        )
        binding.tvNotice.setText(
            R.string.alert_notice_failed
        )
        binding.tvNotice.visibility = View.VISIBLE
        // État FAILED — pas d'auto-fermeture
        // Bouton retour système désactivé
        backCallback.isEnabled = true
    }

    // ── GPS Card ──────────────────────────────────────────

    private fun showGpsCard(
        event: com.roadalert.cameroun.data.db.entity.AccidentEvent
    ) {
        binding.gpsCard.visibility = View.VISIBLE

        if (event.latitude != null &&
            event.longitude != null) {
            binding.tvGpsLabel.setText(
                if (event.isPositionApproximate)
                    R.string.alert_gps_approx
                else
                    R.string.alert_gps_sent
            )
            binding.tvGpsCoords.text = String.format(
                Locale.US,
                "%.4f, %.4f",
                event.latitude,
                event.longitude
            )
            binding.tvGpsLink.text = String.format(
                Locale.US,
                "maps.google.com/?q=%.4f,%.4f",
                event.latitude,
                event.longitude
            )
        } else {
            binding.tvGpsLabel.setText(
                R.string.alert_gps_unavailable
            )
            binding.tvGpsCoords.text = ""
            binding.tvGpsLink.text = ""
        }
    }

    // ── Click listeners ───────────────────────────────────

    private fun setupClickListeners() {
        binding.btnSamu.setOnClickListener {
            dialNumber("15")
        }
        binding.btnPolice.setOnClickListener {
            dialNumber("117")
        }
        binding.btnReturnHome.setOnClickListener {
            navigateToHome()
        }
    }

    private fun dialNumber(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        startActivity(intent)
    }

    private fun navigateToHome() {
        val intent = Intent(
            this,
            HomeActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    // ── Helpers ───────────────────────────────────────────

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat(
            "HH:mm — dd/MM/yyyy",
            Locale.FRANCE
        )
        return sdf.format(Date(timestamp))
    }

    private fun getSmsStatusLabel(status: String): String {
        return when (status) {
            SmsStatus.SENT.name ->
                getString(R.string.alert_contact_sent)
            SmsStatus.FAILED.name ->
                getString(R.string.alert_contact_failed)
            else ->
                getString(R.string.alert_contact_pending)
        }
    }

    private fun getDotDrawable(status: String): Int {
        return when (status) {
            SmsStatus.SENT.name ->
                R.drawable.shape_circle_green
            SmsStatus.FAILED.name ->
                R.drawable.shape_circle_red
            else ->
                R.drawable.shape_circle_orange
        }
    }
}
package com.roadalert.cameroun.ui.countdown

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.roadalert.cameroun.databinding.ActivityCountdownBinding
import com.roadalert.cameroun.ui.home.HomeActivity
import com.roadalert.cameroun.util.Constants
import com.roadalert.cameroun.util.ServiceActions

class CountdownActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountdownBinding
    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackNavigation()
        setupVibrator()
        setupCancelButton()
        startCountdown()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Intentionnellement vide
                }
            }
        )
    }

    private fun setupVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(
                VIBRATOR_MANAGER_SERVICE
            ) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrateShort() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        50L, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50L)
            }
        }
    }

    private fun vibrateLong() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createOneShot(
                        300L, VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(300L)
            }
        }
    }

    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            handleCancellation()
        }
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(
            Constants.COUNTDOWN_DURATION, 1_000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000L) + 1
                updateUI(secondsLeft)
                vibrateShort()
            }

            override fun onFinish() {
                handleExpiration()
            }
        }.start()
    }

    private fun updateUI(secondsLeft: Long) {
        binding.tvCountdownNumber.text = secondsLeft.toString()
        val progress = secondsLeft.toFloat() /
                (Constants.COUNTDOWN_DURATION / 1000L).toFloat()
        binding.countdownRing.progress = progress
    }

    private fun handleCancellation() {
        countDownTimer?.cancel()
        sendBroadcast(Intent(ServiceActions.ACTION_CANCEL_COUNTDOWN))
        vibrateLong()
        showCancelledState()
        binding.root.postDelayed({ navigateToHome() }, 500L)
    }

    private fun handleExpiration() {
        showExpiredState()
        binding.root.postDelayed({ finish() }, 1_000L)
    }

    private fun showCancelledState() {
        binding.tvCountdownTitle.visibility = View.GONE
        binding.countdownContainer.visibility = View.GONE
        binding.tvSubtitle.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.tvCancelledTitle.visibility = View.VISIBLE
        binding.tvCancelledSub.visibility = View.VISIBLE
        binding.root.setBackgroundColor(0xFF1A4A1A.toInt())
    }

    private fun showExpiredState() {
        binding.tvCountdownTitle.visibility = View.GONE
        binding.countdownContainer.visibility = View.GONE
        binding.tvSubtitle.visibility = View.GONE
        binding.btnCancel.visibility = View.GONE
        binding.tvExpiredTitle.visibility = View.VISIBLE
        binding.tvExpiredSub.visibility = View.VISIBLE
        binding.root.setBackgroundColor(0xFF3D0000.toInt())
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
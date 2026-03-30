package com.roadalert.cameroun.ui.countdown

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
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
import com.roadalert.cameroun.util.AppSettings
import com.roadalert.cameroun.util.Constants
import com.roadalert.cameroun.util.LocaleHelper
import com.roadalert.cameroun.util.ServiceActions

class CountdownActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    private lateinit var binding: ActivityCountdownBinding
    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private var countdownDuration: Long = Constants.COUNTDOWN_DURATION
    private var mediaPlayer: MediaPlayer? = null
    private var soundEnabled: Boolean = true
    private var vibrationEnabled: Boolean = true
    private var currentRemainingMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownBinding.inflate(layoutInflater)
        setContentView(binding.root)

        countdownDuration = AppSettings(this).getCountdownDuration()
        soundEnabled = AppSettings(this).isSoundEnabled()
        vibrationEnabled = AppSettings(this).isVibrationEnabled()
        setupBackNavigation()
        setupVibrator()
        setupCancelButton()
        startCountdown()
        startAlarmSound()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        stopAlarmSound()
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
        if (!vibrationEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (currentRemainingMs > 5_000L) {
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            250L, VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    it.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 100, 100, 100, 100, 100),
                            -1
                        )
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(250L)
            }
        }
    }

    private fun vibrateLong() {
        if (!vibrationEnabled) return
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

    private fun startAlarmSound() {
        if (!soundEnabled) return
        try {
            val alarmUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@CountdownActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Son alarm indisponible — ignorer silencieusement
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(
            countdownDuration, 1_000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingMs = millisUntilFinished
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
                (countdownDuration / 1000L).toFloat()
        binding.countdownRing.progress = progress
    }

    private fun handleCancellation() {
        countDownTimer?.cancel()
        stopAlarmSound()
        sendBroadcast(Intent(ServiceActions.ACTION_CANCEL_COUNTDOWN).apply { setPackage(packageName) })
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
package com.roadalert.cameroun.ui.language

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.roadalert.cameroun.databinding.ActivityLanguageSelectionBinding
import com.roadalert.cameroun.ui.splash.SplashActivity
import com.roadalert.cameroun.util.LocaleHelper

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnglish.setOnClickListener { selectLanguage("en") }
        binding.btnFrancais.setOnClickListener { selectLanguage("fr") }
    }

    private fun selectLanguage(lang: String) {
        LocaleHelper.setLocale(this, lang)
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

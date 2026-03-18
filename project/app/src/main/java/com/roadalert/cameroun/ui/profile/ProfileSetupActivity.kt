package com.roadalert.cameroun.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.roadalert.cameroun.R
import com.roadalert.cameroun.databinding.ActivityProfileSetupBinding
import com.roadalert.cameroun.ui.home.HomeActivity
import kotlinx.coroutines.launch

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackNavigation()
        observeViewModel()
        setupTextWatchers()
        setupClickListeners()
        updateButtonState()
    }

    // ── Back navigation ───────────────────────────────────────

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.currentStep.value == 2) {
                        viewModel.goBackToStep1()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    // ── Observe ViewModel ─────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                when (step) {
                    1 -> showStep1()
                    2 -> showStep2()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.step1Data.collect { data ->
                data?.let {
                    if (viewModel.currentStep.value == 1) {
                        binding.etFullName.setText(it.fullName)
                        binding.etPhoneNumber.setText(it.phoneNumber)
                        binding.etBloodType.setText(it.bloodType)
                        binding.etAllergies.setText(it.allergies)
                        binding.etMedicalConditions.setText(it.medicalConditions)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.saveSuccess.collect { success ->
                if (success) {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.profile_saved),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    binding.root.postDelayed({
                        navigateToHome()
                    }, 1000)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.btnAction.isEnabled = !loading
                binding.btnAction.text = if (loading)
                    getString(R.string.profile_saving)
                else
                    getCurrentButtonText()
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Snackbar.make(
                        binding.root, it, Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ── Steps display ─────────────────────────────────────────

    private fun showStep1() {
        binding.step1Container.visibility = View.VISIBLE
        binding.step2Container.visibility = View.GONE
        binding.tvStepIndicator.text = getString(R.string.profile_step_1)
        binding.tvProgressText.text = getString(R.string.profile_step_1_label)
        binding.progressStep1.setBackgroundColor(getColor(R.color.red_primary))
        binding.progressStep2.setBackgroundColor(getColor(R.color.border))
        binding.btnAction.text = getString(R.string.profile_btn_next)
        clearStep1Errors()
        updateButtonState()
    }

    private fun showStep2() {
        binding.step1Container.visibility = View.GONE
        binding.step2Container.visibility = View.VISIBLE
        binding.tvStepIndicator.text = getString(R.string.profile_step_2)
        binding.tvProgressText.text = getString(R.string.profile_step_2_label)
        binding.progressStep1.setBackgroundColor(getColor(R.color.red_primary))
        binding.progressStep2.setBackgroundColor(getColor(R.color.red_primary))
        binding.btnAction.text = getString(R.string.profile_btn_save)
        clearStep2Errors()
        updateButtonState()
    }

    // ── Clear errors ──────────────────────────────────────────

    private fun clearStep1Errors() {
        binding.tvFullNameError.visibility = View.GONE
        binding.tvPhoneError.visibility = View.GONE
    }

    private fun clearStep2Errors() {
        binding.tvContact1NameError.visibility = View.GONE
        binding.tvContact1PhoneError.visibility = View.GONE
    }

    // ── Text watchers ─────────────────────────────────────────

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int, before: Int, count: Int
            ) {}
        }
        binding.etFullName.addTextChangedListener(watcher)
        binding.etPhoneNumber.addTextChangedListener(watcher)
        binding.etContact1Name.addTextChangedListener(watcher)
        binding.etContact1Phone.addTextChangedListener(watcher)
        binding.etContact2Name.addTextChangedListener(watcher)
        binding.etContact2Phone.addTextChangedListener(watcher)
    }

    // ── Click listeners ───────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener {
            when (viewModel.currentStep.value) {
                1 -> handleStep1Next()
                2 -> handleStep2Save()
            }
        }
    }

    // ── Step 1 handler ────────────────────────────────────────

    private fun handleStep1Next() {
        val name = binding.etFullName.text.toString()
        val phone = binding.etPhoneNumber.text.toString()
        val bloodType = binding.etBloodType.text.toString()

        val error = viewModel.validateStep1(name, phone, bloodType)

        if (error != null) {
            Snackbar.make(
                binding.root, error, Snackbar.LENGTH_LONG
            ).show()

            binding.tvFullNameError.visibility =
                if (name.isBlank() || !viewModel.isValidName(name))
                    View.VISIBLE else View.GONE
            binding.tvFullNameError.text =
                if (name.isBlank()) getString(R.string.profile_field_required)
                else getString(R.string.profile_name_invalid)

            binding.tvPhoneError.visibility =
                if (phone.isBlank() || !viewModel.isValidCameroonPhone(phone))
                    View.VISIBLE else View.GONE
            binding.tvPhoneError.text =
                if (phone.isBlank()) getString(R.string.profile_field_required)
                else getString(R.string.profile_phone_invalid)
            return
        }

        clearStep1Errors()
        viewModel.saveStep1Data(
            name = name,
            phone = phone,
            blood = bloodType,
            allergiesText = binding.etAllergies.text.toString(),
            conditions = binding.etMedicalConditions.text.toString()
        )
    }

    // ── Step 2 handler ────────────────────────────────────────

    private fun handleStep2Save() {
        val c1Name = binding.etContact1Name.text.toString()
        val c1Phone = binding.etContact1Phone.text.toString()
        val c2Name = binding.etContact2Name.text.toString()
        val c2Phone = binding.etContact2Phone.text.toString()

        val error = viewModel.validateStep2(
            userPhone = viewModel.getUserPhone(),
            c1Name = c1Name,
            c1Phone = c1Phone,
            c2Name = c2Name,
            c2Phone = c2Phone
        )

        if (error != null) {
            Snackbar.make(
                binding.root, error, Snackbar.LENGTH_LONG
            ).show()

            binding.tvContact1NameError.visibility =
                if (c1Name.isBlank() || !viewModel.isValidName(c1Name))
                    View.VISIBLE else View.GONE
            binding.tvContact1NameError.text =
                if (c1Name.isBlank()) getString(R.string.profile_field_required)
                else getString(R.string.profile_name_invalid)

            binding.tvContact1PhoneError.visibility =
                if (c1Phone.isBlank() || !viewModel.isValidCameroonPhone(c1Phone))
                    View.VISIBLE else View.GONE
            binding.tvContact1PhoneError.text =
                if (c1Phone.isBlank()) getString(R.string.profile_field_required)
                else getString(R.string.profile_phone_invalid)
            return
        }

        clearStep2Errors()
        viewModel.saveProfile(
            contact1Name = c1Name,
            contact1Phone = c1Phone,
            contact1Relation = binding.etContact1Relation.text.toString(),
            contact2Name = c2Name,
            contact2Phone = c2Phone,
            contact2Relation = binding.etContact2Relation.text.toString()
        )
    }

    // ── Button state ──────────────────────────────────────────

    private fun updateButtonState() {
        val isValid = when (viewModel.currentStep.value) {
            1 -> viewModel.isStep1ButtonEnabled(
                binding.etFullName.text.toString(),
                binding.etPhoneNumber.text.toString()
            )
            2 -> viewModel.isStep2ButtonEnabled(
                binding.etContact1Name.text.toString(),
                binding.etContact1Phone.text.toString()
            )
            else -> false
        }

        binding.btnAction.isEnabled = isValid
        binding.btnAction.backgroundTintList =
            if (isValid)
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.red_primary)
                )
            else
                android.content.res.ColorStateList.valueOf(
                    getColor(R.color.border)
                )

        binding.tvMissingFields.visibility =
            if (isValid) View.GONE else View.VISIBLE
    }

    private fun getCurrentButtonText(): String {
        return when (viewModel.currentStep.value) {
            1 -> getString(R.string.profile_btn_next)
            2 -> getString(R.string.profile_btn_save)
            else -> getString(R.string.profile_btn_next)
        }
    }

    // ── Navigation ────────────────────────────────────────────

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
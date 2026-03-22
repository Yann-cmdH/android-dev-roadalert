package com.roadalert.cameroun.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
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

    // ── Boutons groupe sanguin ────────────────────────────
    private val bloodTypeButtons by lazy {
        mapOf(
            "A+"  to binding.btnBloodAPos,
            "A-"  to binding.btnBloodANeg,
            "B+"  to binding.btnBloodBPos,
            "B-"  to binding.btnBloodBNeg,
            "O+"  to binding.btnBloodOPos,
            "O-"  to binding.btnBloodONeg,
            "AB+" to binding.btnBloodABPos,
            "AB-" to binding.btnBloodABNeg,
            "NSP" to binding.btnBloodNSP
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(
            layoutInflater
        )
        setContentView(binding.root)

        setupBackNavigation()
        setupBloodTypeButtons()
        setupContact3Button()
        setupTextWatchers()
        setupClickListeners()
        observeViewModel()
        updateButtonState()

        // ── Gestion ouverture depuis Settings ─────────────
        // Si START_STEP = 2 → aller directement aux contacts
        // Les données sont déjà chargées par init() du ViewModel
        val startStep = intent.getIntExtra("START_STEP", 1)
        if (startStep == 2) {
            viewModel.goToStep2Direct()
        }
    }

    // ── Back navigation ───────────────────────────────────

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

    // ── Groupe sanguin — grille boutons ───────────────────

    private fun setupBloodTypeButtons() {
        bloodTypeButtons.forEach { (type, button) ->
            button.setOnClickListener {
                if (viewModel.isBloodTypeSelected(type)) {
                    viewModel.selectBloodType(null)
                } else {
                    viewModel.selectBloodType(type)
                }
                updateBloodTypeUI()
            }
        }
    }

    private fun updateBloodTypeUI() {
        val selectedType = viewModel.selectedBloodType.value
        bloodTypeButtons.forEach { (type, button) ->
            if (type == selectedType) {
                button.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(R.color.red_primary)
                    )
                button.setTextColor(getColor(R.color.white))
            } else {
                button.backgroundTintList =
                    ColorStateList.valueOf(
                        getColor(R.color.border)
                    )
                button.setTextColor(
                    getColor(R.color.text_primary)
                )
            }
        }
    }

    // ── Contact 3 — bouton ajouter ────────────────────────

    private fun setupContact3Button() {
        binding.btnAddContact3.setOnClickListener {
            viewModel.showContact3()
            binding.contact3Container.visibility = View.VISIBLE
            binding.btnAddContact3.visibility = View.GONE
        }
    }

    // ── Observe ViewModel ─────────────────────────────────

    private fun observeViewModel() {

        // Observer étape courante
        lifecycleScope.launch {
            viewModel.currentStep.collect { step ->
                when (step) {
                    1 -> showStep1()
                    2 -> showStep2()
                }
            }
        }

        // Observer données étape 1 — pré-remplissage
        lifecycleScope.launch {
            viewModel.step1Data.collect { data ->
                data?.let {
                    if (viewModel.currentStep.value == 1) {
                        binding.etFullName.setText(it.fullName)
                        binding.etPhoneNumber.setText(
                            it.phoneNumber
                        )
                        binding.etAllergies.setText(it.allergies)
                        binding.etMedicalConditions.setText(
                            it.medicalConditions
                        )
                        viewModel.selectBloodType(it.bloodType)
                        updateBloodTypeUI()
                    }
                }
            }
        }

        // Observer contacts existants
        // Pré-remplit les contacts seulement si on vient
        // de Settings — évite d'écraser une saisie fraîche
        lifecycleScope.launch {
            viewModel.existingContacts.collect { contacts ->
                val caller = intent.getStringExtra("CALLER")
                if (caller == "SETTINGS" &&
                    contacts.isNotEmpty()) {

                    contacts.getOrNull(0)?.let { c ->
                        binding.etContact1Name.setText(c.name)
                        binding.etContact1Phone.setText(
                            c.phoneNumber
                        )
                        binding.etContact1Relation.setText(
                            c.relation
                        )
                    }

                    contacts.getOrNull(1)?.let { c ->
                        binding.etContact2Name.setText(c.name)
                        binding.etContact2Phone.setText(
                            c.phoneNumber
                        )
                        binding.etContact2Relation.setText(
                            c.relation
                        )
                    }

                    contacts.getOrNull(2)?.let { c ->
                        binding.contact3Container.visibility =
                            View.VISIBLE
                        binding.btnAddContact3.visibility =
                            View.GONE
                        binding.etContact3Name.setText(c.name)
                        binding.etContact3Phone.setText(
                            c.phoneNumber
                        )
                        binding.etContact3Relation.setText(
                            c.relation
                        )
                    }
                }
            }
        }

        // Observer succès sauvegarde
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

        // Observer chargement
        lifecycleScope.launch {
            viewModel.isLoading.collect { loading ->
                binding.btnAction.isEnabled = !loading
                binding.btnAction.text = if (loading)
                    getString(R.string.profile_saving)
                else
                    getCurrentButtonText()
            }
        }

        // Observer erreurs
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Snackbar.make(
                        binding.root,
                        it,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ── Steps display ─────────────────────────────────────

    private fun showStep1() {
        binding.step1Container.visibility = View.VISIBLE
        binding.step2Container.visibility = View.GONE
        binding.tvStepIndicator.text =
            getString(R.string.profile_step_1)
        binding.tvProgressText.text =
            getString(R.string.profile_step_1_label)
        binding.progressStep1.setBackgroundColor(
            getColor(R.color.red_primary)
        )
        binding.progressStep2.setBackgroundColor(
            getColor(R.color.border)
        )
        binding.btnAction.text =
            getString(R.string.profile_btn_next)
        clearStep1Errors()
        updateButtonState()
    }

    private fun showStep2() {
        binding.step1Container.visibility = View.GONE
        binding.step2Container.visibility = View.VISIBLE
        binding.tvStepIndicator.text =
            getString(R.string.profile_step_2)
        binding.tvProgressText.text =
            getString(R.string.profile_step_2_label)
        binding.progressStep1.setBackgroundColor(
            getColor(R.color.red_primary)
        )
        binding.progressStep2.setBackgroundColor(
            getColor(R.color.red_primary)
        )
        binding.btnAction.text =
            getString(R.string.profile_btn_save)
        clearStep2Errors()
        updateButtonState()
    }

    // ── Clear errors ──────────────────────────────────────

    private fun clearStep1Errors() {
        binding.tvFullNameError.visibility = View.GONE
        binding.tvPhoneError.visibility = View.GONE
    }

    private fun clearStep2Errors() {
        binding.tvContact1NameError.visibility = View.GONE
        binding.tvContact1PhoneError.visibility = View.GONE
    }

    // ── Text watchers ─────────────────────────────────────

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }
            override fun beforeTextChanged(
                s: CharSequence?, start: Int,
                count: Int, after: Int
            ) {}
            override fun onTextChanged(
                s: CharSequence?, start: Int,
                before: Int, count: Int
            ) {}
        }
        binding.etFullName.addTextChangedListener(watcher)
        binding.etPhoneNumber.addTextChangedListener(watcher)
        binding.etContact1Name.addTextChangedListener(watcher)
        binding.etContact1Phone.addTextChangedListener(watcher)
        binding.etContact2Name.addTextChangedListener(watcher)
        binding.etContact2Phone.addTextChangedListener(watcher)
        binding.etContact3Name.addTextChangedListener(watcher)
        binding.etContact3Phone.addTextChangedListener(watcher)
    }

    // ── Click listeners ───────────────────────────────────

    private fun setupClickListeners() {
        binding.btnAction.setOnClickListener {
            when (viewModel.currentStep.value) {
                1 -> handleStep1Next()
                2 -> handleStep2Save()
            }
        }
    }

    // ── Step 1 handler ────────────────────────────────────

    private fun handleStep1Next() {
        val name = binding.etFullName.text.toString()
        val phone = binding.etPhoneNumber.text.toString()

        val error = viewModel.validateStep1(name, phone)

        if (error != null) {
            Snackbar.make(
                binding.root, error, Snackbar.LENGTH_LONG
            ).show()

            binding.tvFullNameError.visibility =
                if (name.isBlank() ||
                    !viewModel.isValidName(name))
                    View.VISIBLE else View.GONE
            binding.tvFullNameError.text =
                if (name.isBlank())
                    getString(R.string.profile_field_required)
                else getString(R.string.profile_name_invalid)

            binding.tvPhoneError.visibility =
                if (phone.isBlank() ||
                    !viewModel.isValidCameroonPhone(phone))
                    View.VISIBLE else View.GONE
            binding.tvPhoneError.text =
                if (phone.isBlank())
                    getString(R.string.profile_field_required)
                else getString(R.string.profile_phone_invalid)

            return
        }

        clearStep1Errors()

        viewModel.saveStep1Data(
            name = name,
            phone = phone,
            allergiesText = binding.etAllergies
                .text.toString(),
            conditions = binding.etMedicalConditions
                .text.toString()
        )
    }

    // ── Step 2 handler ────────────────────────────────────

    private fun handleStep2Save() {
        val c1Name = binding.etContact1Name.text.toString()
        val c1Phone = binding.etContact1Phone.text.toString()
        val c2Name = binding.etContact2Name.text.toString()
        val c2Phone = binding.etContact2Phone.text.toString()
        val c3Name = binding.etContact3Name.text.toString()
        val c3Phone = binding.etContact3Phone.text.toString()

        val error = viewModel.validateStep2(
            userPhone = viewModel.getUserPhone(),
            c1Name = c1Name,
            c1Phone = c1Phone,
            c2Name = c2Name,
            c2Phone = c2Phone,
            c3Name = c3Name,
            c3Phone = c3Phone
        )

        if (error != null) {
            Snackbar.make(
                binding.root, error, Snackbar.LENGTH_LONG
            ).show()

            binding.tvContact1NameError.visibility =
                if (c1Name.isBlank() ||
                    !viewModel.isValidName(c1Name))
                    View.VISIBLE else View.GONE
            binding.tvContact1NameError.text =
                if (c1Name.isBlank())
                    getString(R.string.profile_field_required)
                else getString(R.string.profile_name_invalid)

            binding.tvContact1PhoneError.visibility =
                if (c1Phone.isBlank() ||
                    !viewModel.isValidCameroonPhone(c1Phone))
                    View.VISIBLE else View.GONE
            binding.tvContact1PhoneError.text =
                if (c1Phone.isBlank())
                    getString(R.string.profile_field_required)
                else getString(R.string.profile_phone_invalid)

            return
        }

        clearStep2Errors()

        viewModel.saveProfile(
            contact1Name = c1Name,
            contact1Phone = c1Phone,
            contact1Relation = binding.etContact1Relation
                .text.toString(),
            contact2Name = c2Name,
            contact2Phone = c2Phone,
            contact2Relation = binding.etContact2Relation
                .text.toString(),
            contact3Name = c3Name,
            contact3Phone = c3Phone,
            contact3Relation = binding.etContact3Relation
                .text.toString()
        )
    }

    // ── Button state ──────────────────────────────────────

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
                ColorStateList.valueOf(
                    getColor(R.color.red_primary)
                )
            else
                ColorStateList.valueOf(
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

    // ── Navigation ────────────────────────────────────────

    private fun navigateToHome() {
        val caller = intent.getStringExtra("CALLER")
        if (caller == "SETTINGS") {
            finish()
        } else {
            startActivity(
                Intent(this, HomeActivity::class.java)
            )
            finish()
        }
    }
}
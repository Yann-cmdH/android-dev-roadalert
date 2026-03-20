package com.roadalert.cameroun.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.data.db.entity.User
import com.roadalert.cameroun.data.repository.UserProfileRepository
import com.roadalert.cameroun.util.IdGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserProfileRepository
) : ViewModel() {

    // ── UI State ──────────────────────────────────────────────

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ── Step 1 data — kept in memory until atomic save ───────

    private var fullName: String = ""
    private var phoneNumber: String = ""
    private var bloodType: String? = null
    private var allergies: String = ""
    private var medicalConditions: String = ""

    // ── Step 1 pré-remplissage au retour ─────────────────────

    private val _step1Data = MutableStateFlow<Step1Data?>(null)
    val step1Data: StateFlow<Step1Data?> = _step1Data

    data class Step1Data(
        val fullName: String,
        val phoneNumber: String,
        val bloodType: String?,
        val allergies: String,
        val medicalConditions: String
    )

    // ── Groupe sanguin — valeurs fixes SAD DEC-01 ─────────────

    val bloodTypeOptions = listOf(
        "A+", "A-",
        "B+", "B-",
        "O+", "O-",
        "AB+", "AB-"
    )

    private val _selectedBloodType = MutableStateFlow<String?>(null)
    val selectedBloodType: StateFlow<String?> = _selectedBloodType

    fun selectBloodType(type: String?) {
        _selectedBloodType.value = type
        bloodType = type
    }

    fun isBloodTypeSelected(type: String): Boolean {
        return _selectedBloodType.value == type
    }

    // ── Contact 3 visible ─────────────────────────────────────

    private val _isContact3Visible = MutableStateFlow(false)
    val isContact3Visible: StateFlow<Boolean> = _isContact3Visible

    fun showContact3() {
        _isContact3Visible.value = true
    }

    // ── Phone utilisateur pour validation doublons ────────────

    fun getUserPhone(): String = phoneNumber

    // ── VALIDATION ────────────────────────────────────────────

    fun isValidCameroonPhone(phone: String): Boolean {
        return phone.trim().matches(Regex("^6[0-9]{8}$"))
    }

    fun isValidName(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.length >= 2 &&
                cleaned.all {
                    it.isLetter() ||
                            it.isWhitespace() ||
                            it == '-' ||
                            it == '\''
                }
    }

    fun isContactValid(name: String, phone: String): Boolean {
        val nameEmpty = name.isBlank()
        val phoneEmpty = phone.isBlank()
        if (nameEmpty && phoneEmpty) return true
        if (nameEmpty || phoneEmpty) return false
        return true
    }

    fun hasDuplicatePhones(
        userPhone: String,
        c1Phone: String,
        c2Phone: String,
        c3Phone: String
    ): Boolean {
        val phones = listOf(userPhone, c1Phone, c2Phone, c3Phone)
            .filter { it.isNotBlank() }
        return phones.size != phones.distinct().size
    }

    fun validateStep1(name: String, phone: String): String? {
        if (name.isBlank())
            return "Le nom complet est obligatoire"
        if (!isValidName(name))
            return "Le nom ne peut pas contenir de chiffres"
        if (phone.isBlank())
            return "Le numero de telephone est obligatoire"
        if (!isValidCameroonPhone(phone))
            return "Numero invalide — 9 chiffres commencant par 6"
        return null
    }

    fun validateStep2(
        userPhone: String,
        c1Name: String,
        c1Phone: String,
        c2Name: String,
        c2Phone: String,
        c3Name: String,
        c3Phone: String
    ): String? {
        if (c1Name.isBlank())
            return "Le nom du Contact 1 est obligatoire"
        if (!isValidName(c1Name))
            return "Nom Contact 1 invalide"
        if (c1Phone.isBlank())
            return "Le telephone du Contact 1 est obligatoire"
        if (!isValidCameroonPhone(c1Phone))
            return "Contact 1 — numero invalide (ex: 677654321)"
        if (c1Phone == userPhone)
            return "Le Contact 1 ne peut pas avoir votre propre numero"

        if (!isContactValid(c2Name, c2Phone))
            return "Contact 2 incomplet — remplissez nom et telephone ou laissez les deux vides"
        if (c2Phone.isNotBlank() && !isValidCameroonPhone(c2Phone))
            return "Contact 2 — numero invalide (ex: 677654321)"
        if (c2Phone.isNotBlank() && c2Phone == userPhone)
            return "Le Contact 2 ne peut pas avoir votre propre numero"

        if (!isContactValid(c3Name, c3Phone))
            return "Contact 3 incomplet — remplissez nom et telephone ou laissez les deux vides"
        if (c3Phone.isNotBlank() && !isValidCameroonPhone(c3Phone))
            return "Contact 3 — numero invalide (ex: 677654321)"
        if (c3Phone.isNotBlank() && c3Phone == userPhone)
            return "Le Contact 3 ne peut pas avoir votre propre numero"

        if (hasDuplicatePhones(userPhone, c1Phone, c2Phone, c3Phone))
            return "Deux contacts ne peuvent pas avoir le meme numero"

        return null
    }

    // ── Bouton actif ──────────────────────────────────────────

    fun isStep1ButtonEnabled(
        name: String,
        phone: String
    ): Boolean {
        return name.isNotBlank() && phone.isNotBlank()
    }

    fun isStep2ButtonEnabled(
        c1Name: String,
        c1Phone: String
    ): Boolean {
        return c1Name.isNotBlank() && c1Phone.isNotBlank()
    }

    // ── Step 1 actions ────────────────────────────────────────

    fun saveStep1Data(
        name: String,
        phone: String,
        allergiesText: String,
        conditions: String
    ) {
        fullName = name.trim()
        phoneNumber = phone.trim()
        allergies = allergiesText.trim()
        medicalConditions = conditions.trim()

        _step1Data.value = Step1Data(
            fullName = fullName,
            phoneNumber = phoneNumber,
            bloodType = bloodType,
            allergies = allergies,
            medicalConditions = medicalConditions
        )
        _currentStep.value = 2
    }

    fun goBackToStep1() {
        _currentStep.value = 1
    }

    // ── Sauvegarde atomique ───────────────────────────────────

    fun saveProfile(
        contact1Name: String,
        contact1Phone: String,
        contact1Relation: String,
        contact2Name: String,
        contact2Phone: String,
        contact2Relation: String,
        contact3Name: String,
        contact3Phone: String,
        contact3Relation: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val now = System.currentTimeMillis()

                // Vérifier si profil existe déjà
                // Si oui → conserver le même userId
                // pour éviter la duplication en Room
                val existingUser = repository.getUserSync()
                val userId = existingUser?.id
                    ?: IdGenerator.newId()

                val user = User(
                    id = userId,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    bloodType = bloodType,
                    allergies = allergies.ifBlank { null },
                    medicalConditions =
                    medicalConditions.ifBlank { null },
                    createdAt = existingUser?.createdAt ?: now,
                    updatedAt = now
                )

                val contacts = mutableListOf<EmergencyContact>()

                // Contact 1 — obligatoire
                if (contact1Name.isNotBlank() &&
                    contact1Phone.isNotBlank()) {
                    contacts.add(
                        EmergencyContact(
                            id = IdGenerator.newId(),
                            userId = userId,
                            name = contact1Name.trim(),
                            phoneNumber = contact1Phone.trim(),
                            relation = contact1Relation.trim(),
                            priority = 1,
                            isActive = true
                        )
                    )
                }

                // Contact 2 — optionnel
                if (contact2Name.isNotBlank() &&
                    contact2Phone.isNotBlank()) {
                    contacts.add(
                        EmergencyContact(
                            id = IdGenerator.newId(),
                            userId = userId,
                            name = contact2Name.trim(),
                            phoneNumber = contact2Phone.trim(),
                            relation = contact2Relation.trim(),
                            priority = 2,
                            isActive = true
                        )
                    )
                }

                // Contact 3 — optionnel
                if (contact3Name.isNotBlank() &&
                    contact3Phone.isNotBlank()) {
                    contacts.add(
                        EmergencyContact(
                            id = IdGenerator.newId(),
                            userId = userId,
                            name = contact3Name.trim(),
                            phoneNumber = contact3Phone.trim(),
                            relation = contact3Relation.trim(),
                            priority = 3,
                            isActive = true
                        )
                    )
                }

                repository.saveProfileWithContacts(user, contacts)
                _saveSuccess.value = true

            } catch (e: Exception) {
                _errorMessage.value =
                    "Erreur lors de la sauvegarde. Veuillez reessayer."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
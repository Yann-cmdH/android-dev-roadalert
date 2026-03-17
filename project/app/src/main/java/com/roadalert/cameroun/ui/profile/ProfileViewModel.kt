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
    private var bloodType: String = ""
    private var allergies: String = ""
    private var medicalConditions: String = ""

    // ── Expose phone for duplicate check in step 2 ───────────

    fun getUserPhone(): String = phoneNumber

    // ── Step 1 pre-fill on back navigation ───────────────────

    private val _step1Data = MutableStateFlow<Step1Data?>(null)
    val step1Data: StateFlow<Step1Data?> = _step1Data

    data class Step1Data(
        val fullName: String,
        val phoneNumber: String,
        val bloodType: String,
        val allergies: String,
        val medicalConditions: String
    )

    // ── VALIDATION ────────────────────────────────────────────

    // Numéro camerounais — 9 chiffres commençant par 6
    fun isValidCameroonPhone(phone: String): Boolean {
        val cleaned = phone.trim()
        return cleaned.matches(Regex("^6[0-9]{8}$"))
    }

    // Nom valide — min 2 chars, pas de chiffres
    fun isValidName(name: String): Boolean {
        val cleaned = name.trim()
        return cleaned.length >= 2 &&
                cleaned.all { it.isLetter() || it.isWhitespace() || it == '-' || it == '\'' }
    }

    // Groupe sanguin valide — max 5 chars
    fun isValidBloodType(blood: String): Boolean {
        if (blood.isBlank()) return true
        return blood.trim().length <= 5
    }

    // Contact partiel — nom sans téléphone ou téléphone sans nom
    fun isContactValid(name: String, phone: String): Boolean {
        val nameEmpty = name.isBlank()
        val phoneEmpty = phone.isBlank()
        if (nameEmpty && phoneEmpty) return true
        if (nameEmpty || phoneEmpty) return false
        return true
    }

    // Doublons numéros
    fun hasDuplicatePhones(
        userPhone: String,
        c1Phone: String,
        c2Phone: String
    ): Boolean {
        val phones = listOf(userPhone, c1Phone, c2Phone)
            .filter { it.isNotBlank() }
        return phones.size != phones.distinct().size
    }

    // Validation Step 1 complète
    fun validateStep1(
        name: String,
        phone: String,
        bloodType: String
    ): String? {
        if (name.isBlank())
            return "Le nom complet est obligatoire"
        if (!isValidName(name))
            return "Le nom ne peut pas contenir de chiffres ou caractères spéciaux"
        if (name.trim().length < 2)
            return "Le nom est trop court — minimum 2 caractères"
        if (phone.isBlank())
            return "Le numéro de téléphone est obligatoire"
        if (!isValidCameroonPhone(phone))
            return "Numéro invalide — 9 chiffres commençant par 6 (ex: 677654321)"
        if (!isValidBloodType(bloodType))
            return "Groupe sanguin invalide — maximum 5 caractères (ex: AB+)"
        return null
    }

    // Validation Step 2 complète
    fun validateStep2(
        userPhone: String,
        c1Name: String,
        c1Phone: String,
        c2Name: String,
        c2Phone: String
    ): String? {
        if (c1Name.isBlank())
            return "Le nom du Contact 1 est obligatoire"
        if (!isValidName(c1Name))
            return "Nom Contact 1 invalide — pas de chiffres ou caractères spéciaux"
        if (c1Phone.isBlank())
            return "Le téléphone du Contact 1 est obligatoire"
        if (!isValidCameroonPhone(c1Phone))
            return "Contact 1 — numéro invalide (ex: 677654321)"
        if (c1Phone == userPhone)
            return "Le Contact 1 ne peut pas avoir votre propre numéro"
        if (!isContactValid(c2Name, c2Phone))
            return "Contact 2 incomplet — remplissez nom et téléphone ou laissez les deux vides"
        if (c2Phone.isNotBlank() && !isValidCameroonPhone(c2Phone))
            return "Contact 2 — numéro invalide (ex: 677654321)"
        if (c2Phone.isNotBlank() && c2Phone == userPhone)
            return "Le Contact 2 ne peut pas avoir votre propre numéro"
        if (hasDuplicatePhones(userPhone, c1Phone, c2Phone))
            return "Les contacts ne peuvent pas avoir le même numéro de téléphone"
        return null
    }

    // Bouton actif step 1
    fun isStep1ButtonEnabled(name: String, phone: String): Boolean {
        return name.isNotBlank() && phone.isNotBlank()
    }

    // Bouton actif step 2
    fun isStep2ButtonEnabled(c1Name: String, c1Phone: String): Boolean {
        return c1Name.isNotBlank() && c1Phone.isNotBlank()
    }

    // ── Step 1 actions ────────────────────────────────────────

    fun saveStep1Data(
        name: String,
        phone: String,
        blood: String,
        allergiesText: String,
        conditions: String
    ) {
        fullName = name.trim()
        phoneNumber = phone.trim()
        bloodType = blood.trim()
        allergies = allergiesText.trim()
        medicalConditions = conditions.trim()

        // Garder pour pré-remplissage au retour
        _step1Data.value = Step1Data(
            fullName = fullName,
            phoneNumber = phoneNumber,
            bloodType = bloodType,
            allergies = allergies,
            medicalConditions = medicalConditions
        )
        _currentStep.value = 2
    }

    // ── Back to step 1 ────────────────────────────────────────

    fun goBackToStep1() {
        _currentStep.value = 1
    }

    // ── Save — atomic — sans runBlocking ─────────────────────

    fun saveProfile(
        contact1Name: String,
        contact1Phone: String,
        contact1Relation: String,
        contact2Name: String,
        contact2Phone: String,
        contact2Relation: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val userId = IdGenerator.newId()
                val now = System.currentTimeMillis()

                val user = User(
                    id = userId,
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    bloodType = bloodType.ifBlank { null },
                    allergies = allergies.ifBlank { null },
                    medicalConditions = medicalConditions.ifBlank { null },
                    createdAt = now,
                    updatedAt = now
                )

                val contacts = mutableListOf<EmergencyContact>()

                if (contact1Name.isNotBlank() && contact1Phone.isNotBlank()) {
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

                if (contact2Name.isNotBlank() && contact2Phone.isNotBlank()) {
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

                repository.saveProfileWithContacts(user, contacts)
                _saveSuccess.value = true

            } catch (e: Exception) {
                _errorMessage.value =
                    "Erreur lors de la sauvegarde. Veuillez réessayer."
            } finally {
                _isLoading.value = false
            }
        }
    }
}
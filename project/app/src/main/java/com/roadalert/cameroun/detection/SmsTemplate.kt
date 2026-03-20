package com.roadalert.cameroun.detection

import com.roadalert.cameroun.data.db.entity.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Résultat de capture GPS ───────────────────────────────
sealed class LocationResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val isApproximate: Boolean = false
    ) : LocationResult()
    object Unavailable : LocationResult()
}

// ── Résultat d'envoi SMS ──────────────────────────────────
sealed class SmsResult {
    object Sent : SmsResult()
    data class Failed(val reason: String) : SmsResult()
}

// ── Constructeur de SMS d'urgence ─────────────────────────
object SmsTemplate {

    private const val MAX_SMS_LENGTH = 160

    private val DATE_FORMAT = SimpleDateFormat(
        "HH:mm dd/MM/yyyy",
        Locale.FRANCE
    )

    fun build(
        user: User,
        locationResult: LocationResult,
        timestamp: Long
    ): String {

        val heure = DATE_FORMAT.format(Date(timestamp))

        val positionLine = when (locationResult) {
            is LocationResult.Success -> {
                val url = "maps.google.com/?q=" +
                        "${locationResult.latitude}," +
                        "${locationResult.longitude}"
                if (locationResult.isApproximate)
                    "Position approx : $url"
                else
                    "Position : $url"
            }
            LocationResult.Unavailable ->
                "Position : Indisponible"
        }

        // ── SMS complet avec toutes les données ───────────
        val smsFull = buildString {
            append("ALERTE ACCIDENT - RoadAlert\n")
            append("Victime : ${user.fullName}\n")
            append("Tel : ${user.phoneNumber}\n")
            append("Heure : $heure\n")
            append("$positionLine\n")
            if (!user.bloodType.isNullOrBlank()) {
                append("Groupe : ${user.bloodType}\n")
            }
            if (!user.allergies.isNullOrBlank()) {
                append("Allergies : ${user.allergies}\n")
            }
            if (!user.medicalConditions.isNullOrBlank()) {
                append("Conditions : ${user.medicalConditions}\n")
            }
            append("Contactez les secours")
        }

        if (smsFull.length <= MAX_SMS_LENGTH) return smsFull

        // ── Troncature 1 — Sans conditions médicales ──────
        val sansConditions = buildString {
            append("ALERTE ACCIDENT - RoadAlert\n")
            append("Victime : ${user.fullName}\n")
            append("Tel : ${user.phoneNumber}\n")
            append("Heure : $heure\n")
            append("$positionLine\n")
            if (!user.bloodType.isNullOrBlank()) {
                append("Groupe : ${user.bloodType}\n")
            }
            if (!user.allergies.isNullOrBlank()) {
                append("Allergies : ${user.allergies}\n")
            }
            append("Contactez les secours")
        }

        if (sansConditions.length <= MAX_SMS_LENGTH) return sansConditions

        // ── Troncature 2 — Sans allergies ─────────────────
        val sansAllergies = buildString {
            append("ALERTE ACCIDENT - RoadAlert\n")
            append("Victime : ${user.fullName}\n")
            append("Tel : ${user.phoneNumber}\n")
            append("Heure : $heure\n")
            append("$positionLine\n")
            if (!user.bloodType.isNullOrBlank()) {
                append("Groupe : ${user.bloodType}\n")
            }
            append("Contactez les secours")
        }

        if (sansAllergies.length <= MAX_SMS_LENGTH) return sansAllergies

        // ── Troncature 3 — Sans bloodType ─────────────────
        val sansBloodType = buildString {
            append("ALERTE ACCIDENT - RoadAlert\n")
            append("Victime : ${user.fullName}\n")
            append("Tel : ${user.phoneNumber}\n")
            append("Heure : $heure\n")
            append("$positionLine\n")
            append("Contactez les secours")
        }

        if (sansBloodType.length <= MAX_SMS_LENGTH) return sansBloodType

        // ── Troncature 4 — SMS minimal obligatoire ────────
        return buildString {
            append("ALERTE ACCIDENT - RoadAlert\n")
            append("Victime : ${user.fullName}\n")
            append("Tel : ${user.phoneNumber}\n")
            append("$positionLine\n")
            append("Contactez les secours")
        }
    }

    fun getLength(
        user: User,
        locationResult: LocationResult,
        timestamp: Long
    ): Int = build(user, locationResult, timestamp).length
}
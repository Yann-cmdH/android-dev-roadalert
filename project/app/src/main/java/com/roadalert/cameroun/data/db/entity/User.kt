package com.roadalert.cameroun.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val bloodType: String? = null,
    val allergies: String? = null,
    val medicalConditions: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isProfileComplete(): Boolean {
        return fullName.isNotBlank() && phoneNumber.isNotBlank()
    }

    fun getDisplayName(): String {
        return fullName.ifBlank { "Unknown" }
    }

    fun hasMedicalInfo(): Boolean {
        return !bloodType.isNullOrBlank() || !allergies.isNullOrBlank()
    }
}
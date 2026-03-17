package com.roadalert.cameroun.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_contact",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class EmergencyContact(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val relation: String,
    val priority: Int = 1,
    val isActive: Boolean = true
) {
    fun getFormattedPhone(): String {
        return phoneNumber.trim()
    }
}
package com.roadalert.cameroun.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emergency_contact",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"])
    ]
)
data class EmergencyContact(
    @PrimaryKey
    val id: String,

    // Clé étrangère vers User
    val userId: String,

    // Informations du contact
    val name: String,
    val phoneNumber: String,
    val relation: String,

    // Ordre d'alerte — 1 = principal
    val priority: Int,

    // Actif ou non
    val isActive: Boolean = true
)